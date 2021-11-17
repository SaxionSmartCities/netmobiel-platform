package eu.netmobiel.communicator.service;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.commons.exception.DuplicateEntryException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.ExceptionUtil;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.communicator.filter.MessageFilter;
import eu.netmobiel.communicator.model.CommunicatorUser;
import eu.netmobiel.communicator.model.Conversation;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Envelope;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.model.UserRole;
import eu.netmobiel.communicator.repository.CommunicatorUserDao;
import eu.netmobiel.communicator.repository.ConversationDao;
import eu.netmobiel.communicator.repository.EnvelopeDao;
import eu.netmobiel.communicator.repository.MessageDao;
import eu.netmobiel.firebase.messaging.FirebaseMessagingClient;
import eu.netmobiel.messagebird.MessageBird;
import eu.netmobiel.profile.filter.DelegationFilter;
import eu.netmobiel.profile.model.Delegation;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.service.DelegationManager;
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
    private ConversationDao conversationDao;

    @Inject
    private ProfileManager profileManager;
    
    @Inject
    private DelegationManager delegationManager;

    @Inject
    private MessageBird	messageBirdClient;

//    @Inject
//    private NotifierService notifierService;

    @Inject
    private FirebaseMessagingClient firebaseMessagingClient;

	public void validateMessage(Message msg) throws CreateException, BadRequestException {
    	if (msg.getContext() == null) {
    		throw new BadRequestException("Constraint violation: sender/system 'context' must be set.");
    	}
    	if (msg.getDeliveryMode() == null) {
    		throw new BadRequestException("Constraint violation: 'deliveryMode' must be set.");
    	}
    	if (msg.getEnvelopes() == null || msg.getEnvelopes().isEmpty()) {
    		throw new BadRequestException("Constraint violation: 'envelopes' must be set and contain at least one recipient.");
    	}
		if (msg.getEnvelopes().stream()
			.filter(env -> env.getContext() == null && env.getContext().isBlank())
			.findAny().isPresent()) {
			throw new BadRequestException("Envelope context cannot be null or blank");
		}
		// There cannot yet be a sender in the envelopes
		if (msg.getEnvelopes().stream()
				.filter(env -> env.isSender())
				.findAny().isPresent()) {
				throw new BadRequestException("Do not add a sender envelope");
		}
    }

    /**
     * Sends a message and/or a notification to the recipients in the message envelopes.
     * The conversations of sender and recipients must already exist!
     * @param sender the sender of the message.
     * @param msg the message to send to the recipients in the envelopes
     * @return the message Id.
     * @throws BusinessException 
     */
    public Long publish(CommunicatorUser sender, Message msg) throws BusinessException {
		msg.setCreatedTime(Instant.now());
		validateMessage(msg);
		if (sender != null) {
			Conversation senderConv = lookupConversation(sender, msg.getContext());
			msg.addSender(senderConv, msg.getContext());
		}
		// Assure all recipient conversations are present in the database, replace transient instances of users with persistent instances.
		for (Envelope env : msg.getEnvelopes()) {
			Conversation rcpConv = lookupConversation(env.getRecipient(), env.getContext());
			env.setConversation(rcpConv);
		}
		msg.setId(null); 	// Assure it is a new message.
		// Save message and envelopes in database
		messageDao.save(msg);
		// Just to be sure before invoking the next (asynchronous) method.
//		messageDao.flush();
		// Send each user a notification, if required
		if (msg.getDeliveryMode() == DeliveryMode.NOTIFICATION || msg.getDeliveryMode() == DeliveryMode.ALL) {
			// The asynchronous notifier method does not save the push time for some reason.
			// For now: Make synchronous until it is a real problem.
//			notifierService.sendNotification(msg);
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
    	return msg.getId();
    }

    /**
     * Creates a conversation
     * @param c the conversation. Must already be present in the communicator database.
     * @return The ID of the conversation just created.
     * @throws BadRequestException In case of bad parameters.
     * @throws DuplicateEntryException When a conversation is found with a matching (context, owner)
     */
    public Long createConversation(Conversation c) throws BadRequestException, DuplicateEntryException {
    	if (c.getContexts().isEmpty()) {
    		throw new BadRequestException("Specify at least one context");
    	}
    	if (c.getTopic() == null || c.getTopic().isBlank()) {
    		throw new BadRequestException("Specify a non-blank topic");
    	}
    	Optional<Conversation> optConv = Optional.empty();
    	for (String context : c.getContexts()) {
    		optConv = conversationDao.findByContextAndOwner(context, c.getOwner());
    		if (!optConv.isPresent()) {
    			throw new DuplicateEntryException(
    					String.format("Conversation %d already associated with context %s", 
    							optConv.get().getId(), context));
    		}
		}
    	c.setCreatedTime(Instant.now());
    	c.setArchivedTime(null);
		return conversationDao.save(c).getId();
    }

    /**
     * Fetches a single conversation.  
     * @param id the id of the conversation
     * @return A conversation object (without messages).
     * @throws NotFoundException If there is no matching id found.
     */
    public Conversation getConversation(Long id) throws NotFoundException {
    	Conversation convdb = conversationDao.loadGraph(id, Conversation.FULL_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such conversation: " + id));
    	return convdb;
    }

    public void updateConversation(Conversation conversation) throws BusinessException {
    	Conversation convdb = conversationDao.loadGraph(conversation.getId(), Conversation.DEFAULT_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such conversation: " + conversation.getId()));
    	conversation.setOwner(convdb.getOwner());
    	conversationDao.merge(conversation);
    }
    
    /**
     * Lookup a conversation by owner and context. If absent create a new conversation.
     * @param owner the owner of the conversation to look for
     * @param ownerRole the role of the owner at the time of creation of the conversation. In general
     * 		this can be deducted from the context of the operation.
     * @param context the context to look for. Initial context for a new conversation. 
     * @param topic the subject the conversation is about. Might change during the lifetime of the conversation.
     * @param overwriteTopic if true then overwrite the subject. 
     * @return a conversation object.
     */
    public Conversation lookupOrCreateConversation(CommunicatorUser owner, UserRole ownerRole, String context, String topic, boolean overwriteTopic) {
		Optional<Conversation> optConv = conversationDao.findByContextAndOwner(context, owner);
		if (optConv.isPresent()) {
			// Is the equal check really needed?
			if (overwriteTopic && !Objects.equals(topic, optConv.get().getTopic())) {
				optConv.get().setTopic(topic);
			}
		} else {
			optConv = Optional.of(new Conversation(owner, ownerRole, context, topic));
			conversationDao.save(optConv.get());
		}
		return optConv.get();
    }

    public Conversation lookupOrCreateConversation(NetMobielUser owner, UserRole ownerRole, String context, String topic, boolean overwriteTopic) {
		CommunicatorUser user = userDao.findByManagedIdentity(owner.getManagedIdentity())
				.orElseGet(() -> userDao.save(new CommunicatorUser(owner)));
    	return lookupOrCreateConversation(user, ownerRole, context, topic, overwriteTopic);
    }

    public List<Conversation> lookupOrCreateConversations(List<? extends NetMobielUser> owner, UserRole ownerRole, String context, String topic, boolean overwriteTopic) {
    	return owner.stream()
    			.map(usr -> lookupOrCreateConversation(usr, ownerRole, context, topic, overwriteTopic))
    			.collect(Collectors.toList());
    }

    /**
     * Finds a conversation by user and context.
     * @param managedIdentity 
     * @param contexts
     * @return
     */
    public Optional<Conversation> findConversation(NetMobielUser owner, String context) {
		return findConversation(owner.getManagedIdentity(), context);
    }

    /**
     * Finds a conversation of a user by the managedIdentity and the context.
     * @param managedIdentity 
     * @param contexts
     * @return
     */
    public Optional<Conversation> findConversation(String managedIdentity, String context) {
		return conversationDao.findByContextAndOwner(context, managedIdentity);
    }

    /**
     * Finds the conversation by managedIdentity and context. 
     * @param nbUser NetMobiel user.
     * @param context The context to look for.
     * @return The conversation.
     * @throws NotFoundException If the conversation does not exist.
     */
    public Conversation lookupConversation(NetMobielUser nbUser, String context) throws NotFoundException {
		return conversationDao.findByContextAndOwner(context, nbUser.getManagedIdentity())
				.orElseThrow(() -> new NotFoundException(String.format("User %s has no conversation for %s", nbUser, context)));
    }

    /**
     * Extend the context list of an existing conversation.
     * @param conversation the (existing) conversation to update
     * @param context the context to add.
     * @throws NotFoundException In case the conversation does not exist.
     */
    public void addConversationContext(Conversation conversation, String context, String topic, boolean overwriteTopic) throws NotFoundException {
		Conversation cvdb = conversationDao.loadGraph(conversation.getId(), Conversation.FULL_ENTITY_GRAPH)
				.orElseThrow(() -> new NotFoundException("No such Conversation: " + conversation.getId()));
		cvdb.getContexts().add(context);
		if (overwriteTopic && topic != null) {
			cvdb.setTopic(topic);
		}
    }

    public void addConversationContext(Conversation conversation, String context) throws NotFoundException {
    	addConversationContext(conversation, context, null, false);
    }

    /**
     * Lists the messages matching the criteria.
	/**
	 * Lists the message ids considering the filter parameters.  
	 * @param filter The message filter to use. 
	 * @param cursor The cursor to use. 
     * @return A page of messages.
     * @throws BadRequestException Missing parameters.
     */
	public @NotNull PagedResult<Message> listMessages(MessageFilter filter, Cursor cursor) throws BadRequestException {
    	// As an optimisation we could first call the data. If less then maxResults are received, we can deduce the totalCount and thus omit
    	// the additional call to determine the totalCount.
    	// For now don't do conditional things. First always total count, then data if data is requested. 
    	// Get the total count
		filter.validate();
		cursor.validate(MAX_RESULTS, 0);
    	PagedResult<Long> prs = messageDao.listMessages(filter, Cursor.COUNTING_CURSOR);
    	List<Message> results = null;
    	if (!cursor.isCountingQuery()) {
    		// Get the actual data
    		PagedResult<Long> mids = messageDao.listMessages(filter, cursor);
    		results = messageDao.loadGraphs(mids.getData(), Message.MESSAGE_ENVELOPES_ENTITY_GRAPH, Message::getId);
    	} else {
    		results = Collections.emptyList();
    	}
    	return new PagedResult<>(results, cursor, prs.getTotalCount());
	}

	/**
	 * Lists the conversations of a user. Each conversation is a related list of messages, the relation is determined by the context attribute.
	 * The result contains the latest message for each conversation. Notification only messages are ignored.
	 * @param context the context to look for.
	 * @param owner the owner of the conversation
	 * @param actualOnly If true then list only the actual conversations, i.e. those that are not archived yet.   
	 * @param archoivedOnly If true then list only the archived conversations.   
     * @param maxResults for paging: maximum number of results per page.
     * @param offset for paging: zero-based offset in the result.  
     * @return A page of messages.
	 * @throws BadRequestException 
	 */
    public @NotNull PagedResult<Conversation> listConversations(String context, String owner, boolean actualOnly, boolean archivedOnly, Integer maxResults, Integer offset) throws BadRequestException {
        if (maxResults == null) {
        	maxResults = MAX_RESULTS;
        }
        if (offset == null) {
        	offset = 0;
        }
        if (owner == null) {
        	// The paging does not work with owner null, because of the inversion of message --> conversation
        	// To get it to work, something must be done to invert it. Not requried right now.
        	throw new BadRequestException("The owner of the conversation is a mandatory parameter");
        }
        if (actualOnly && archivedOnly) {
        	throw new BadRequestException("You cannot have actualOnly AND archiveOnly at the same time");
        }
    	// Get the total count
    	PagedResult<Long> prs = messageDao.listTopMessagesByConversations(context, owner, actualOnly, archivedOnly, 0, offset);
    	List<Message> messages = null;
    	if (maxResults > 0) {
    		// Get the actual data
        	PagedResult<Long> mids = messageDao.listTopMessagesByConversations(context, owner, actualOnly, archivedOnly, maxResults, offset);
        	messages = messageDao.loadGraphs(mids.getData(), Message.MESSAGE_ENVELOPES_ENTITY_GRAPH, Message::getId);
    	} else {
    		messages = Collections.emptyList();
    	}
    	// Detach all objects, we do not want to modify the database.
    	messageDao.clear();
    	List<Conversation> conversations = new ArrayList<>();
    	for (Message msg: messages) {
    		Conversation c = msg.getEnvelopes().stream()
    				.filter(env -> owner.equals(env.getConversation().getOwner().getManagedIdentity()))
    				.findFirst()
    				.orElseThrow(() -> new IllegalStateException("Expected an envelope for owner: " + owner))
    				.getConversation();
    		conversations.add(c);
    		c.setRecentMessage(msg);
    	}
    	return new PagedResult<>(conversations, maxResults, offset, prs.getTotalCount());
    }

    public void updateMessage(Long messageId, Message message) throws NotFoundException, BadRequestException {
//    	for (Envelope env : message.getEnvelopes()) {
//    		if (env.getId() == null) {
//    			envelopeDao.save(env);
//    		}
//		}
    	messageDao.merge(message);
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

    /**
     * Informs any delegator of the specified user with a message.
     * @param delegator The person who possibly has his travels managed by someone else.
     * @param message The message to deliver.
     * @param deliveryMode the delivery mode: message, notification or both.
     */
    @Asynchronous
    public void informDelegates(NetMobielUser delegator, String message, DeliveryMode deliveryMode) {
		try {
			DelegationFilter filter = new DelegationFilter();
			Cursor cursor = new Cursor(10, 0);
			filter.setDelegator(new Profile(delegator.getManagedIdentity()));
			PagedResult<Delegation> delegations = delegationManager.listDelegations(filter, cursor, Delegation.DELEGATE_PROFILE_ENTITY_GRAPH);
			if (delegations.getTotalCount() > cursor.getMaxResults()) {
				logger.warn("Too many delegates detected: # > " + cursor.getMaxResults());
			}
			if (delegations.getCount() > 0) {
				String topic = MessageFormat.format("Beheer van reizen van {0}", delegator.getName());
				Message msg = new Message();
				// The context of the 'sender': The profile of the delegator (the common denominator)
				msg.setContext(delegations.getData().get(0).getDelegatorRef());
				msg.setDeliveryMode(deliveryMode);
				msg.setBody(message);
				// Start or continue conversation for all recipients with delegation context
				// Delegation conversations are by definition targeting a passenger role.
				delegations.getData().forEach(delegation -> msg.addRecipient(
						lookupOrCreateConversation(delegation.getDelegate(), UserRole.DELEGATE, delegation.getUrn(), topic, false), delegation.getUrn()
				));
				publish(null, msg);
			}
		} catch (BusinessException ex) {
			logger.error("Cannot inform delegates: " + String.join("\n\t", ExceptionUtil.unwindException(ex)));
		}

    }
}
