package eu.netmobiel.communicator.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.ejb.Schedule;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import eu.netmobiel.commons.annotation.Created;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.commons.exception.DuplicateEntryException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.NetMobielMessage;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.util.ExceptionUtil;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.communicator.event.ChatMessageEvent;
import eu.netmobiel.communicator.event.RequestConversationEvent;
import eu.netmobiel.communicator.filter.ConversationFilter;
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
import eu.netmobiel.communicator.repository.EnvelopeDao.UnreadMessagesCount;
import eu.netmobiel.firebase.messaging.FirebaseMessagingClient;
import eu.netmobiel.messagebird.MessageBird;

/**
 * Bean class for Publisher enterprise bean. 
 */
@Stateless
@Logging
public class PublisherService {
	public static final int NOTIFICATION_TTL = 60 * 60 * 1000; // [ms] Expiration time of a notification  
	public static final Integer MAX_RESULTS = 10;
	/**
	 * Archive conversations if most recent message is older than 30 days from now. 
	 */
	public static final int ARCHIVE_DAYS = 30;
    @Inject
    private Logger logger;

    @Resource
	private SessionContext sessionContext;

    @Inject
    private EnvelopeDao envelopeDao;
    
    @Inject
    private MessageDao messageDao;

    @Inject
    private CommunicatorUserDao userDao;

    @Inject
    private ConversationDao conversationDao;

    @Inject
    private MessageBird	messageBirdClient;

    @Inject @Created
    private Event<ChatMessageEvent> chatMessageCreatedEvent;

    @Inject @Created
    private Event<RequestConversationEvent> requestConversationEvent;
//    @Inject
//    private NotifierService notifierService;

    @Inject
    private FirebaseMessagingClient firebaseMessagingClient;

	private static void validateChatMessage(Message msg) throws CreateException, BadRequestException {
		// There must be a real sender
		long nrSenders = msg.getEnvelopes().stream()
				.filter(env -> env.isSender())
				.count();
		if (nrSenders != 1) {
			throw new BadRequestException("A chat message must have exact one sender");
		}
    }
 	
	private static void validateMessage(Message msg) throws CreateException, BadRequestException {
    	if (msg.getContext() == null) {
    		throw new BadRequestException("Constraint violation: sender/system 'context' must be set.");
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
     * The conversations of sender and recipients must already exist!
     * @param msg the message to send to the recipients in the envelopes
     * @return the message Id.
     * @throws BusinessException 
     */
    public Long chat(Message msg) throws BusinessException {
    	validateChatMessage(msg);
		Long msgId = publish(msg);
		chatMessageCreatedEvent.fire(new ChatMessageEvent(msg));
		return msgId;
    }

    /**
     * Notifies the recipient in the specified envelope with the given message.
     * @param msg the message to send as push notification
     * @param env the recipient's envelope
     */
    private void notifyRecipient(NetMobielMessage msg, Envelope env) {
		try {
			CommunicatorUser user = env.getRecipient();
			if ((user.getFcmToken() == null || user.getFcmToken().isBlank()) && logger.isDebugEnabled()) {
				logger.debug(String.format("Cannot send push notification to %s (%s): No FCM token set", 
						user.getManagedIdentity(), user.getName()));
			} else if (FirebaseMessagingClient.isFcmTokenProbablyStale(user.getFcmTokenTimestamp())) {
				logger.warn(String.format("Cannot send push notification to %s: FCM token (%s) is probably stale", user.getName(), user.getFcmTokenTimestamp()));
				user.setFcmToken(null);
			} else {
				try {
					firebaseMessagingClient.send(user.getFcmToken(), msg);
					env.setPushTime(Instant.now());
				} catch (NotFoundException | BadRequestException ex) {
					logger.error(String.format("Cannot send push notification to %s: FCM token (%s) is stale or invalid.", user.getManagedIdentity(), user.getFcmTokenTimestamp()));
					user.setFcmToken(null);
				}
			}
		} catch (Exception ex) {
			logger.error(String.format("Cannot send push notification to %s: %s", 
					env.getRecipient().getManagedIdentity(), String.join("\n\t", ExceptionUtil.unwindException(ex))));
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
    public Long publish(Message msg) throws BusinessException {
		validateMessage(msg);
		if (msg.getCreatedTime() == null) {
			msg.setCreatedTime(Instant.now());
		}
		// Assure all recipient conversations are present in the database, replace transient instances of users with persistent instances.
		for (Envelope env : msg.getEnvelopes()) {
			Conversation conv_in = env.getConversation();
			Conversation conv_db = lookupOrCreateConversation(conv_in.getOwner(), 
					conv_in.getOwnerRole(),	conv_in.getInitialContext(), conv_in.getTopic());
			// Add additional contexts
			conv_db.getContexts().addAll(conv_in.getContexts());
			// The context of the envelope is added to the conversation. The message context is the shared context
			conv_db.getContexts().add(msg.getContext());
			conv_db.getContexts().add(env.getContext());
			// New message, so don't archive anymore (if it was archived)
			conv_db.setArchivedTime(null);
			env.setConversation(conv_db);
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
				// Skip the sender envelope, if any.
				if (env.isSender()) {
					continue;
				}
				notifyRecipient(msg, env);
			}
		}
    	return msg.getId();
    }

    /**
     * Creates a conversation. This method is used by the front-end to start a conversation. Especially needed when 
     * starting a chat with someone (and no system message was received yet). 
     * @param c the conversation. 
     * @return The ID of the conversation just created.
     * @throws BadRequestException In case of bad parameters.
     * @throws DuplicateEntryException When a conversation is found with a matching (context, owner)
     * @throws NotFoundException 
     */
    public Long createConversation(Conversation c) throws BadRequestException, DuplicateEntryException, NotFoundException, CreateException {
    	if (c.getInitialContext() == null) {
    		throw new BadRequestException("Specify the initial context");
    	}
    	if (c.getOwner() == null) {
    		throw new BadRequestException("Specify an owner");
    	}
    	if (c.getOwner().getId() == null) {
    		CommunicatorUser usr = userDao.findByManagedIdentity(c.getOwner().getManagedIdentity())
    				.orElseThrow(() -> new NotFoundException("No such user: " + c.getOwner().getManagedIdentity()));
    		c.setOwner(usr);
    	}
   		Optional<Conversation> optConv = conversationDao.findByContextAndOwner(c.getInitialContext(), c.getOwner());
		if (optConv.isPresent()) {
			throw new DuplicateEntryException(
					String.format("Conversation %s already associated with context %s", 
							optConv.get().getUrn(), c.getInitialContext()));
		}
    	Optional<Conversation> cdbopt = createConversation(c.getOwner(), c.getOwnerRole(), c.getInitialContext(), c.getTopic());
    	if (cdbopt.isEmpty()) {
    		throw new CreateException("Error creating conversation");
    	}
    	Conversation cdb = cdbopt.get();
    	cdb.getContexts().addAll(c.getContexts());
		return cdb.getId();
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
    	conversation.setCreatedTime(convdb.getCreatedTime());
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
     * @throws NotFoundException 
     * @throws BusinessException 
     */
    private Conversation lookupOrCreateConversation(CommunicatorUser owner, UserRole ownerRole, String context, String topic) throws BusinessException {
    	CommunicatorUser ownerdb = owner;
    	if (owner.getId() == null) {
    		ownerdb = userDao.findByManagedIdentity(owner.getManagedIdentity())
    				.orElseThrow(() -> new NotFoundException("No such user: " + owner)); 
    	}
		Optional<Conversation> optConv = conversationDao.findByContextAndOwner(context, ownerdb);
		if (optConv.isPresent()) {
			// Is this optimization needed?
			if (topic != null && !Objects.equals(topic, optConv.get().getTopic())) {
				optConv.get().setTopic(topic);
			}
		} else {
			optConv = sessionContext.getBusinessObject(PublisherService.class)
					.createConversation(ownerdb, ownerRole, context, topic);
			if (optConv.isEmpty()) {
				optConv = conversationDao.findByContextAndOwner(context, ownerdb);
				if (optConv.isEmpty()) {
					throw new CreateException(String.format("Unable to create conversation for user %s and context %s", 
							ownerdb.getUrn(), context));
				}
			}
		}
		return optConv.get();
    }

    /**
     * Creates a conversation. If topic or userRole are missing the system is asked to create a conversation.
     * @param ownerdb the owner of the conversation to look for. Must already exist.
     * @param ownerRole the role of the owner at the time of creation of the conversation. In general
     * 		this can be deducted from the context of the operation.
     * @param context the context to look for. Initial context for a new conversation. 
     * @param topic the subject the conversation is about. Might change during the lifetime of the conversation.
     * @param overwriteTopic if true then overwrite the subject. 
     * @return a conversation object.
     * @throws NotFoundException 
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Optional<Conversation> createConversation(CommunicatorUser ownerdb, UserRole ownerRole, String context, String topic) throws NotFoundException {
		Optional<Conversation> optConv = Optional.empty();
		if (topic == null || ownerRole == null) {
			// Find more details about the conversation, by delegating to the application
			requestConversationEvent.fire(new RequestConversationEvent(context, ownerdb));
			// Now try to lookup if the event was successfully processed 
			optConv = conversationDao.findByContextAndOwner(context, ownerdb);
			if (optConv.isEmpty()) {
				throw new NotFoundException(String.format("User %s has no conversation established for %s", ownerdb, context));
			}
		} else {
			try {
				// Create a new conversation
				Conversation c = new Conversation(ownerdb, ownerRole, context, topic);
				optConv = Optional.of(conversationDao.save(c));
				// Check whether we don't violation the unique constraint
				conversationDao.flush();
			} catch (PersistenceException ex) {
				// System will log message too if enabled in wildfly console
				logger.warn(String.join("\n\t", ExceptionUtil.unwindException("Error creating conversation", ex)));
				optConv = Optional.empty();
			}
		}
		return optConv;
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
    public @NotNull PagedResult<Conversation> listConversations(ConversationFilter filter, Cursor cursor) throws BadRequestException {
    	filter.validate();
    	cursor.validate(MAX_RESULTS, 0);
    	// Get the total count
    	PagedResult<Long> prs = conversationDao.listConversations(filter, Cursor.COUNTING_CURSOR);
    	List<Conversation> conversations = null;
    	if (!cursor.isCountingQuery()) {
    		// Get the actual data
        	PagedResult<Long> cids = conversationDao.listConversations(filter, cursor);
        	conversations = conversationDao.loadGraphs(cids.getData(), Conversation.FULL_ENTITY_GRAPH, Conversation::getId);
    	}
    	return new PagedResult<>(conversations, cursor, prs.getTotalCount());
    }

	/**
	 * Lists the conversations of a user for the inbox display. Each conversation refers to the most recent message.
	 * Notification only messages are ignored.
	 * @param owner the owner of the conversation
	 * @param actualOnly If true then list only the actual conversations, i.e. those that are not archived yet.   
	 * @param archivedOnly If true then list only the archived conversations.
	 * @param sortDirection Dor4ection of the sort (on recent message creation)   
     * @param maxResults for paging: maximum number of results per page.
     * @param offset for paging: zero-based offset in the result.  
     * @return A page of messages.
	 * @throws BadRequestException 
	 */
    public @NotNull PagedResult<Conversation> listConversationsForInbox(CommunicatorUser owner, boolean actualOnly, 
    		boolean archivedOnly, SortDirection sortDir, Integer maxResults, Integer offset) throws BadRequestException {
        if (maxResults == null) {
        	maxResults = MAX_RESULTS;
        }
        if (offset == null) {
        	offset = 0;
        }
        if (owner == null) {
        	// The paging does not work with owner null, because of the inversion of message --> conversation
        	// To get it to work, something must be done to invert it. Not required right now.
        	throw new BadRequestException("The owner of the conversation is a mandatory parameter");
        }
        if (actualOnly && archivedOnly) {
        	throw new BadRequestException("You cannot have actualOnly AND archiveOnly at the same time");
        }
    	// Get the total count
    	PagedResult<Long> prs = messageDao.listTopMessagesByConversations(null, owner, 
    			actualOnly, archivedOnly, sortDir, 0, offset);
    	List<Message> messages = null;
    	if (maxResults > 0) {
    		// Get the actual data
        	PagedResult<Long> mids = messageDao.listTopMessagesByConversations(null, owner, 
        			actualOnly, archivedOnly, sortDir, maxResults, offset);
        	messages = messageDao.loadGraphs(mids.getData(), Message.MESSAGE_ENVELOPES_ENTITY_GRAPH, Message::getId);
    	} else {
    		messages = Collections.emptyList();
    	}
    	// Detach all objects, we do not want to modify the database.
    	messageDao.clear();
    	List<Conversation> conversations = new ArrayList<>();
    	for (Message msg: messages) {
    		Conversation c = msg.getEnvelopes().stream()
    				.filter(env -> owner.equals(env.getConversation().getOwner()))
    				.findFirst()
    				.orElseThrow(() -> new IllegalStateException("Expected an envelope for owner: " + owner))
    				.getConversation();
    		conversations.add(c);
    		c.setRecentMessage(msg);
    	}
    	Map<Long, Conversation> cMap = conversations.stream()
    			.collect(Collectors.toMap(Conversation::getId, Function.identity()));
    	List<UnreadMessagesCount> umsgCount = envelopeDao.countUnreadMessages(cMap.keySet());
    	umsgCount.forEach(uc -> cMap.get(uc.conversationId).setUnreadCount(uc.unreadCount.intValue()));
    	return new PagedResult<>(conversations, maxResults, offset, prs.getTotalCount());
    }

    public Message getMessage(Long messageId) throws NotFoundException {
    	return messageDao.loadGraph(messageId, Message.MESSAGE_ENVELOPES_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such message: " + messageId));
    }
    
    public void updateMessage(Long messageId, Message message) throws NotFoundException, BadRequestException {
//    	for (Envelope env : message.getEnvelopes()) {
//    		if (env.getId() == null) {
//    			envelopeDao.save(env);
//    		}
//		}
    	messageDao.merge(message);
    }

    private CommunicatorUser lookupUser(NetMobielUser recipient) throws NotFoundException {
    	return userDao.findByManagedIdentity(recipient.getManagedIdentity())
    			.orElseThrow(() -> new NotFoundException("No such communicator user: " + recipient.getManagedIdentity()));
    }
    
    public boolean isValidForMobileMessaging(NetMobielUser user) throws NotFoundException {
    	CommunicatorUser recipient = lookupUser(user);
    	boolean isValid = false;
	    // The delegator needs to have a number that can receive an SMS. We require a mobile number.
	    if (! StringUtils.isAllBlank(recipient.getPhoneNumber())) {
		    if (messageBirdClient.isMobileNumber(recipient.getPhoneNumber(), recipient.getCountryCode())) {
		    	isValid = true;
		    }
	    }
	    return isValid;
    }

    public String sendTextMessage(NetMobielUser user, String text) throws BadRequestException, NotFoundException {
    	CommunicatorUser recipient = lookupUser(user);
		String rcpPhoneNr = messageBirdClient.formatPhoneNumberTechnical(recipient.getPhoneNumber(), recipient.getCountryCode());
		return messageBirdClient.sendSMS(null, text, new String[] { rcpPhoneNr });
	}
	
	public String sendVoiceMessage(NetMobielUser user, String text) throws BadRequestException, NotFoundException {
    	CommunicatorUser recipient = lookupUser(user);
		String rcpPhoneNr = messageBirdClient.formatPhoneNumberTechnical(recipient.getPhoneNumber(), recipient.getCountryCode());
		return messageBirdClient.sendVoiceMessage(null, text, new String[] { rcpPhoneNr }, recipient.getLanguageCode());
	}

    public Object getMessageBirdMessage(String messageId) throws NotFoundException {
		return messageBirdClient.getMessage(messageId);
    }

    /**
     * Verify the actual conversations. 
     */
	@Schedule(info = "Archive conversations", hour = "5", minute = "0", second = "0", persistent = false /* non-critical job */)
	public void archiveConversations() {
		// Get the list of actual inbox conversations
		// Could also be done with a bulk update query, but I can't think about the exact JPL query needed.
		Instant history = Instant.now().minusSeconds(3600L * 24 * ARCHIVE_DAYS);
		try {
			int count = conversationDao.archiveConversations(history);
			logger.info(String.format("Archived conversations before %s: %d", history, count));
		} catch (Exception e) {
			logger.error("Error archiving the conversations", e);
		}
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
	    	Envelope envdb = envelopeDao.findByMessageAndRecipient(messageId, recipient);
	    	envdb.setAckTime(ackTime);
	    	envelopeDao.merge(envdb);
    	} catch (NoResultException ex) {
    		throw new NotFoundException (String.format("No such recipient %s for message %d", recipient, messageId));	
    	}
    }

    /**
     * Acknowledge the whole conversation, i.e., mark all envelopes owned by the conversation as acknowledges.
     * @param conv the conversation.
     */
	public void acknowledgeConversation(Conversation conv) {
		envelopeDao.acknowledge(conv, Instant.now());
	}
}
