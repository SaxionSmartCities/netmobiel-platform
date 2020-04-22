package eu.netmobiel.communicator.service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.ExceptionUtil;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Envelope;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.repository.EnvelopeDao;
import eu.netmobiel.communicator.repository.MessageDao;
import eu.netmobiel.firebase.messaging.FirebaseMessagingClient;
import eu.netmobiel.profile.client.ProfileClient;

/**
 * Bean class for Publisher enterprise bean. 
 */
@Stateless
@Logging
public class PublisherService {
	public static final int NOTIFICATION_TTL = 60 * 60 * 1000; // [ms] Expiration time of a notification  
	public static final Integer MAX_RESULTS = 10; 

	@Resource
    private SessionContext sessionContext;

    @Inject
    private Logger logger;

    @EJB(name = "java:app/netmobiel-communicator-ejb/UserManager")
    private UserManager userManager;

    @Inject
    private EnvelopeDao envelopeDao;
    
    @Inject
    private MessageDao messageDao;

    @Inject
    private ProfileClient profileClient;
    
    @Inject
    private FirebaseMessagingClient firebaseMessagingClient;
    
    public PublisherService() {
    }

    /**
     * Sends a message and/or a notification to the recipients in the message envelopes.
     * This is an asynchronous call.  
     * @param msg the message to send to the recipients in the envelopes 
     */
    @Asynchronous
    public void publish(Message msg) throws CreateException, BadRequestException {
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
    	// The sender is always the calling user (for now)
    	msg.setCreationTime(Instant.now());
    	msg.setSender(userManager.registerCallingUser());
    	if (logger.isDebugEnabled()) {
            logger.debug(String.format("Send message from %s to %s: %s %s - %s", msg.getSender(), 
            		msg.getEnvelopes().stream().map(env -> env.getRecipient().getManagedIdentity()).collect(Collectors.joining(", ")), 
            		msg.getContext(), msg.getSubject(), msg.getBody()));
    	}
    	// Assure all recipients are present in the database, replace transient instances off users with persistent instances.
		for (Envelope env : msg.getEnvelopes()) {
			env.setId(null);
			env.setAckTime(null);
			env.setPushTime(null);
			env.setMessage(msg);
			env.setRecipient(userManager.register(env.getRecipient()));
		}
		msg.setId(null); 	// Assure it is a new message.
		messageDao.save(msg);
		// Send each user a notification, if required
		if (msg.getDeliveryMode() == DeliveryMode.NOTIFICATION || msg.getDeliveryMode() == DeliveryMode.ALL) {
			for (Envelope env : msg.getEnvelopes()) {
				try {
					String fcmToken = profileClient.getFirebaseToken(env.getRecipient().getManagedIdentity());
					firebaseMessagingClient.send(fcmToken, msg);
					env.setPushTime(Instant.now());
				} catch (Exception ex) {
					logger.error(String.format("Cannot send push notification to %s: %s", 
							env.getRecipient().getManagedIdentity(), String.join(" - ", ExceptionUtil.unwindException(ex))));
				}
			}
		}
    }

    /**
     * Lists the messages matching the criteria.
     * @param participant The sender or recipient of the message. Default is the calling user.
     * @param context the context of the message (a urn pointing to the database object triggering the message). Default is any context.
     * @param since only show messages sent after a specified time. Default is no time limit set.
     * @param until only show message sent before specified time. Default is no time limit set.
     * @param mode only show messages with specific (effective) delivery mode. Omitting the modes or specifying DeliveryMode.ALL 
     * 				has the same effect: No filter. 
     * @param maxResults for paging: maximum number of results per page. 
     * @param offset for paging: zero-based offset in the result.  
     * @return A page of messages.
     */
	public @NotNull PagedResult<Message> listMessages(String participant, String context, Instant since, Instant until, DeliveryMode mode, Integer maxResults, Integer offset) {
    	// As an optimisation we could first call the data. If less then maxResults are received, we can deduce the totalCount and thus omit
    	// the additional call to determine the totalCount.
    	// For now don't do conditional things. First always total count, then data if data is requested. 
    	// Get the total count
    	String effectiveParticipant = participant != null ? participant : sessionContext.getCallerPrincipal().getName();
        if (maxResults == null) {
        	maxResults = MAX_RESULTS;
        }
        if (offset == null) {
        	offset = 0;
        }
    	PagedResult<Long> prs = messageDao.listMessages(effectiveParticipant, context, since, until, mode, 0, offset);
    	List<Message> results = null;
    	if (maxResults == null || maxResults > 0) {
    		// Get the actual data
    		PagedResult<Long> mids = messageDao.listMessages(effectiveParticipant, context, since, until, mode, maxResults, offset);
    		results = messageDao.fetch(mids.getData(), Message.LIST_MY_MESSAGES_ENTITY_GRAPH);
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
    	String effectiveParticipant = participant != null ? participant : sessionContext.getCallerPrincipal().getName();
        if (maxResults == null) {
        	maxResults = MAX_RESULTS;
        }
        if (offset == null) {
        	offset = 0;
        }
    	// Get the total count
    	PagedResult<Long> prs = messageDao.listConversations(effectiveParticipant, 0, offset);
    	List<Message> results = null;
    	if (maxResults == null || maxResults > 0) {
    		// Get the actual data
        	PagedResult<Long> mids = messageDao.listConversations(effectiveParticipant, maxResults, offset);
        	results = messageDao.fetch(mids.getData(), Message.LIST_MY_MESSAGES_ENTITY_GRAPH);
    	} else {
    		results = Collections.emptyList();
    	}
    	return new PagedResult<Message>(results, maxResults, offset, prs.getTotalCount());
    }

    /**
     * Updates the acknowledgement time of a message, meaning that the user has read the mesaage.
     * The front-end should call this method explicitly when the user has read the message. 'Unreading' is also 
     * possible by setting the acknowledgment time to <code>null</code>.
     * @param messageId the message ID. The calling user must be the owner of the message. 
     * @param ackTime the timestamp, if <code>null</code> then the timestamp is removed.
     * @throws NotFoundException if the message does not exist.
     */
    public void updateAcknowledgment(Long messageId, Instant ackTime) throws NotFoundException {
    	String caller = sessionContext.getCallerPrincipal().getName();
    	try {
	    	Envelope envdb = envelopeDao.findByMessageAndRecipient(messageId, caller);
	    	userManager.checkOwnership(envdb.getRecipient(), Envelope.class.getSimpleName());
	    	envdb.setAckTime(ackTime);
	    	envelopeDao.merge(envdb);
    	} catch (NoResultException ex) {
    		throw new NotFoundException (String.format("No such recipient %s for message %d", caller, messageId));	
    	}
    }

}
