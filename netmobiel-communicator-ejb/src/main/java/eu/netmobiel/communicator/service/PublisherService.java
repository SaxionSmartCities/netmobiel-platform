package eu.netmobiel.communicator.service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.ExceptionUtil;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.communicator.model.CommunicatorUser;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Envelope;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.repository.CommunicatorUserDao;
import eu.netmobiel.communicator.repository.EnvelopeDao;
import eu.netmobiel.communicator.repository.MessageDao;
import eu.netmobiel.firebase.messaging.FirebaseMessagingClient;
import eu.netmobiel.messagebird.MessageBird;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.service.ProfileManager;

/**
 * Bean class for Publisher enterprise bean. 
 */
@Stateless
@Logging
public class PublisherService {
	public static final int NOTIFICATION_TTL = 60 * 60 * 1000; // [ms] Expiration time of a notification  
	public static final Integer MAX_RESULTS = 10; 

    @Inject
    private Logger logger;

    @Inject
    private EnvelopeDao envelopeDao;
    
    @Inject
    private MessageDao messageDao;

    @Inject
    private CommunicatorUserDao userDao;

    @Inject
    private ProfileManager profileManager;
    
    @Inject
    private FirebaseMessagingClient firebaseMessagingClient;
    @Inject
    private MessageBird	messageBirdClient;

    public static final CommunicatorUser SYSTEM_USER = new CommunicatorUser("SYSTEM", "Netmobiel", "", null);

    public PublisherService() {
    }

    public CommunicatorUser findSystemUser() {
    	return userDao.findByManagedIdentity(SYSTEM_USER.getManagedIdentity())
    			.orElseGet(() -> userDao.save(SYSTEM_USER));
    }

    public void validateMessage(CommunicatorUser sender, Message msg) throws CreateException, BadRequestException {
    	if (msg.getContext() == null) {
    		throw new BadRequestException("Constraint violation: 'context' must be set.");
    	}
    	if (msg.getSubject() == null) {
    		throw new BadRequestException("Constraint violation: 'subject' must be set.");
    	}
    	if (msg.getDeliveryMode() == null) {
    		throw new BadRequestException("Constraint violation: 'deliveryMode' must be set.");
    	}
    	if (msg.getEnvelopes() == null || msg.getEnvelopes().isEmpty()) {
    		throw new BadRequestException("Constraint violation: 'envelopes' must be set and contain at least one recipient.");
    	}
    }

    /**
     * Sends a message and/or a notification to the recipients in the message envelopes.
     * This is an asynchronous call. Because of the asynchronous nature, this call cannot throw exceptions. 
     * @param sender the sender of the message 
     * @param msg the message to send to the recipients in the envelopes 
     */
    @Asynchronous
    public void publish(CommunicatorUser sender, Message msg) {
    	try {
			validateMessage(sender, msg);
			// The sender is always the calling user (for now)
			msg.setCreationTime(Instant.now());
			msg.setSender(sender != null ? sender : findSystemUser());
			if (logger.isDebugEnabled()) {
			    logger.debug(String.format("Send message from %s to %s: %s %s - %s", msg.getSender(), 
			    		msg.getEnvelopes().stream().map(env -> env.getRecipient().getManagedIdentity()).collect(Collectors.joining(", ")), 
			    		msg.getContext(), msg.getSubject(), msg.getBody()));
			}
			// Assure all recipients are present in the database, replace transient instances of users with persistent instances.
			for (Envelope env : msg.getEnvelopes()) {
				// Connect the child to the master for JPA
				env.setMessage(msg);
				CommunicatorUser rcp = userDao.findByManagedIdentity(env.getRecipient().getManagedIdentity())
						.orElseGet(() -> userDao.save(env.getRecipient()));
				env.setRecipient(rcp);
			}
			msg.setId(null); 	// Assure it is a new message.
			messageDao.save(msg);
			// Send each user a notification, if required
			if (msg.getDeliveryMode() == DeliveryMode.NOTIFICATION || msg.getDeliveryMode() == DeliveryMode.ALL) {
				for (Envelope env : msg.getEnvelopes()) {
					try {
						Profile profile = profileManager.getFlatProfileByManagedIdentity(env.getRecipient().getManagedIdentity());
						if (profile.getFcmToken() == null || profile.getFcmToken().isBlank()) {
							logger.error(String.format("Cannot send push notification to %s (%s): No FCM token set", 
									profile.getManagedIdentity(), profile.getName()));  
						} else {
							firebaseMessagingClient.send(profile.getFcmToken(), msg);
							env.setPushTime(Instant.now());
						}
					} catch (Exception ex) {
						logger.error(String.format("Cannot send push notification to %s: %s", 
								env.getRecipient().getManagedIdentity(), String.join("\n\t", ExceptionUtil.unwindException(ex))));
					}
				}
			}
		} catch (CreateException | BadRequestException ex) {
			logger.error(String.format("Cannot publish, validation error: %s %s - %s", 
					sender, msg, String.join("\n\t", ExceptionUtil.unwindException(ex))));
		}
    }

    /**
     * Lists the messages matching the criteria.
     * @param participant The managed identity of the sender or recipient of the message. 
     * @param context the context of the message (a urn pointing to the database object triggering the message). Default is any context.
     * @param since only show messages sent after a specified time. Default is no time limit set.
     * @param until only show message sent before specified time. Default is no time limit set.
     * @param mode only show messages with specific (effective) delivery mode. Omitting the modes or specifying DeliveryMode.ALL 
     * 				has the same effect: No filter. 
     * @param maxResults for paging: maximum number of results per page. 
     * @param offset for paging: zero-based offset in the result.  
     * @return A page of messages.
     * @throws BadRequestException Missing parameters.
     */
	public @NotNull PagedResult<Message> listMessages(String participant, String context, Instant since, Instant until, 
			DeliveryMode mode, Integer maxResults, Integer offset) throws BadRequestException {
    	// As an optimisation we could first call the data. If less then maxResults are received, we can deduce the totalCount and thus omit
    	// the additional call to determine the totalCount.
    	// For now don't do conditional things. First always total count, then data if data is requested. 
    	// Get the total count
        if (maxResults == null) {
        	maxResults = MAX_RESULTS;
        }
        if (offset == null) {
        	offset = 0;
        }
    	PagedResult<Long> prs = messageDao.listMessages(participant, context, since, until, mode, 0, offset);
    	List<Message> results = null;
    	if (maxResults == null || maxResults > 0) {
    		// Get the actual data
    		PagedResult<Long> mids = messageDao.listMessages(participant, context, since, until, mode, maxResults, offset);
    		results = messageDao.loadGraphs(mids.getData(), Message.LIST_MY_MESSAGES_ENTITY_GRAPH, Message::getId);
    	} else {
    		results = Collections.emptyList();
    	}
    	return new PagedResult<Message>(results, maxResults, offset, prs.getTotalCount());
	}

	/**
	 * Lists the conversations of a user. Each conversation is a related list of messages, the relation is determined by the context attribute.
	 * The result contains the latest message for each conversation. Notification only messages are ignored.
	 * @param participant the sender or recipient
     * @param maxResults for paging: maximum number of results per page. 
     * @param offset for paging: zero-based offset in the result.  
     * @return A page of messages.
	 */
    public @NotNull PagedResult<Message> listConversations(String participant, Integer maxResults, Integer offset) {
        if (maxResults == null) {
        	maxResults = MAX_RESULTS;
        }
        if (offset == null) {
        	offset = 0;
        }
    	// Get the total count
    	PagedResult<Long> prs = messageDao.listConversations(participant, 0, offset);
    	List<Message> results = null;
    	if (maxResults == null || maxResults > 0) {
    		// Get the actual data
        	PagedResult<Long> mids = messageDao.listConversations(participant, maxResults, offset);
        	results = messageDao.loadGraphs(mids.getData(), Message.LIST_MY_MESSAGES_ENTITY_GRAPH, Message::getId);
    	} else {
    		results = Collections.emptyList();
    	}
    	return new PagedResult<Message>(results, maxResults, offset, prs.getTotalCount());
    }

    /**
     * Updates the acknowledgement time of a message, meaning that the user has read the message.
     * The front-end should call this method explicitly when the user has read the message. 'Unreading' is also 
     * possible by setting the acknowledgment time to <code>null</code>.
     * @param recipient The recipient of the message.
     * @param messageId the message ID. The calling user must be the owner of the message. 
     * @param ackTime the timestamp, if <code>null</code> then the timestamp is removed.
     * @throws NotFoundException if the envelope does not exist for the combination of recipient and message.
     * @throws BadRequestException If the recipient is null.
     */
    public void updateAcknowledgment(CommunicatorUser recipient, Long messageId, Instant ackTime) throws NotFoundException, BadRequestException {
    	try {
	    	if (recipient == null) {
	    		throw new BadRequestException("recipient is a mandatory parameter");
	    	}
	    	Envelope envdb = envelopeDao.findByMessageAndRecipient(messageId, recipient.getManagedIdentity());
	    	envdb.setAckTime(ackTime);
	    	envelopeDao.merge(envdb);
    	} catch (NoResultException ex) {
    		throw new NotFoundException (String.format("No such recipient %s for message %d", recipient, messageId));	
    	}
    }

    public boolean isValidForMobileMessaging(Profile profile) {
    	boolean isValid = false;
	    // The delegator needs to have a number that can receive an SMS. We require a mobile number.
	    if (! StringUtils.isAllBlank(profile.getPhoneNumber())) {
		    if (messageBirdClient.isMobileNumber(profile.getPhoneNumber(), profile.getDefaultCountry())) {
		    	isValid = true;
		    }
	    }
	    return isValid;
    }

    public String sendTextMessage(String text, Profile recipient) throws BadRequestException {
		String rcpPhoneNr = messageBirdClient.formatPhoneNumberTechnical(recipient.getPhoneNumber(), recipient.getDefaultCountry());
		return messageBirdClient.sendSMS(null, text, new String[] { rcpPhoneNr });
	}
	
    public String sendTextMessage(String managedIdentity, String text) throws NotFoundException, BadRequestException {
		Profile profile = profileManager.getFlatProfileByManagedIdentity(managedIdentity);
		return sendTextMessage(text, profile);
    }

	public String sendVoiceMessage(String text, Profile recipient) throws BadRequestException {
		String rcpPhoneNr = messageBirdClient.formatPhoneNumberTechnical(recipient.getPhoneNumber(), recipient.getDefaultCountry());
		return messageBirdClient.sendVoiceMessage(null, text, new String[] { rcpPhoneNr }, "nl-nl");
	}

	public String sendVoiceMessage(String managedIdentity, String text) throws NotFoundException, BadRequestException {
		Profile profile = profileManager.getFlatProfileByManagedIdentity(managedIdentity);
		return sendVoiceMessage(text, profile);
    }
    
    public Object getMessageBirdMessage(String messageId) throws NotFoundException {
		return messageBirdClient.getMessage(messageId);
    }

}
