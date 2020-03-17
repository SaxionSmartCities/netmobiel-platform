package eu.netmobiel.communicator.service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.NoResultException;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Envelope;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.model.User;
import eu.netmobiel.communicator.repository.EnvelopeDao;
import eu.netmobiel.communicator.repository.MessageDao;

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
//    @Resource(lookup = "java:module/jms/netmobielNotificationTopic")
//    private Topic notificationTopic;
//    @Inject
//    private JMSContext context;

    @Inject
    private Logger logger;

    @EJB(name = "java:app/netmobiel-communicator-ejb/UserManager")
    private UserManager userManager;

    @Inject
    private EnvelopeDao envelopeDao;
    
    @Inject
    private MessageDao messageDao;
    
    public PublisherService() {
    }

    /**
     * Creates producer and message. Sends messages after setting their NewsType
     * property and using the property value as the message text. Messages are
     * received by MessageBean, a message-driven bean that uses a message
     * selector to retrieve messages whose NewsType property has certain values.
     * @param msg the message to send
     * @param recipients the addressees of the message
     */
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
    	// Assure all recipients are present in the database
    	List<User> recipients = msg.getEnvelopes().stream().map(env -> env.getRecipient()).collect(Collectors.toList());
    	List<User> dbrecipients = recipients.stream().map(rcp -> userManager.register(rcp)).collect(Collectors.toList());
		if (msg.getDeliveryMode() == DeliveryMode.MESSAGE || msg.getDeliveryMode() == DeliveryMode.ALL) {
			List<Envelope> envelopes = dbrecipients.stream()
					.map(rpc -> new Envelope(msg, rpc))
					.collect(Collectors.toList());
			msg.setEnvelopes(envelopes);
			messageDao.save(msg);
		}
		if (msg.getDeliveryMode() == DeliveryMode.NOTIFICATION || msg.getDeliveryMode() == DeliveryMode.ALL) {
//			sendNotification(msg, recipients);
		}
    }

//    protected void sendNotification(Message msg, String recipients) {
//        try {
//	        MapMessage message = context.createMapMessage();
//	        message.setString("text", msg.getBody());
//	        message.setString("context", msg.getContext());
//	        message.setString("subject", msg.getSubject());
//	        message.setString("deliveryMode", msg.getDeliveryMode().name());
//	        // The sender is the caller for now, unless the sender is set.
//	        message.setString("sender", msg.getSender() != null ? msg.getSender() : sessionContext.getCallerPrincipal().getName());
//	        message.setString("recipients", recipients);
//	        context.createProducer()
//	        	.setDeliveryMode(javax.jms.DeliveryMode.NON_PERSISTENT)	// Notifications are not critical
//	        	.setTimeToLive(NOTIFICATION_TTL)
//	        	.send(notificationTopic, message);
//        } catch (JMSException ex) {
//            logger.error("Error sending notification", ex);
//        }
//	    	
//    }
    
	public PagedResult<Message> listMessages(String participant, String context, Instant since, Instant until, Integer maxResults, Integer offset) {
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
    	PagedResult<Long> prs = messageDao.listMessages(effectiveParticipant, context, since, until, 0, offset);
    	List<Message> results = null;
    	if (maxResults == null || maxResults > 0) {
    		// Get the actual data
    		PagedResult<Long> mids = messageDao.listMessages(effectiveParticipant, context, since, until, maxResults, offset);
    		results = messageDao.fetch(mids.getData(), Message.LIST_MY_MESSAGES_ENTITY_GRAPH);
    	} else {
    		results = Collections.emptyList();
    	}
    	return new PagedResult<Message>(results, maxResults, offset, prs.getTotalCount());
	}

    public PagedResult<Message> listConversations(String participant, Integer maxResults, Integer offset) {
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
    
    public void updateAcknowledgment(Long messaged, Instant ackTime) throws NotFoundException {
    	String caller = sessionContext.getCallerPrincipal().getName();
    	try {
	    	Envelope envdb = envelopeDao.findByMessageAndRecipient(messaged, caller);
	    	userManager.checkOwnership(envdb.getRecipient(), Envelope.class.getSimpleName());
	    	envdb.setAckTime(ackTime);
	    	envelopeDao.merge(envdb);
    	} catch (NoResultException ex) {
    		throw new NotFoundException (String.format("No such recipient %s for message %d", caller, messaged));	
    	}
    }

}
