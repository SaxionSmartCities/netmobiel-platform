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

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.PagedResult;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Envelope;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.model.User;
import eu.netmobiel.communicator.repository.EnvelopeDao;

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
    public void publish(Message msg, List<User> recipients) throws CreateException, BadRequestException {
    	if (msg.getContext() == null) {
    		throw new BadRequestException("Constraint violation: 'context' must be set.");
    	}
    	if (msg.getSubject() == null) {
    		throw new BadRequestException("Constraint violation: 'subject' must be set.");
    	}
    	if (msg.getDeliveryMode() == null) {
    		throw new BadRequestException("Constraint violation: 'deliveryMode' must be set.");
    	}
    	if (recipients == null || recipients.isEmpty()) {
    		throw new BadRequestException("Constraint violation: 'recipients' must be set.");
    	}
    	// The sender is alwyas the calling user (for now)
    	msg.setCreationTime(Instant.now());
    	msg.setSender(userManager.registerCallingUser());
    	if (logger.isDebugEnabled()) {
            logger.debug(String.format("Send message from %s to %s: %s %s - %s", msg.getSender(), recipients, msg.getContext(), msg.getSubject(), msg.getBody()));
    	}
    	// Assure all recipients are present in the database
    	List<User> dbrecipients = recipients.stream().map(rcp -> userManager.register(rcp)).collect(Collectors.toList());
		if (msg.getDeliveryMode() == DeliveryMode.MESSAGE || msg.getDeliveryMode() == DeliveryMode.ALL) {
			List<Envelope> envelopes = dbrecipients.stream()
					.map(rpc -> new Envelope(msg, rpc))
					.collect(Collectors.toList());
			// Always add the sender as recipient too, but acknowledge the message for the sender immediately
			envelopes.add(new Envelope(msg, userManager.register(msg.getSender()), Instant.now()));
			envelopeDao.saveAll(envelopes);
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
    
	public PagedResult<Envelope> listEnvelopes(String recipient, String context, Instant since, Instant until, Integer maxResults, Integer offset) {
    	String effectiveRecipient = recipient != null ? recipient : sessionContext.getCallerPrincipal().getName();
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
    	PagedResult<Long> prs = envelopeDao.listEnvelopes(effectiveRecipient, context, since, until, 0, offset);
    	List<Envelope> results = null;
    	if (maxResults == null || maxResults > 0) {
    		// Get the actual data
    		PagedResult<Long> envIds = envelopeDao.listEnvelopes(effectiveRecipient, context, since, until, maxResults, offset);
    		results = envelopeDao.fetch(envIds.getData(), Envelope.LIST_MY_ENVELOPES_ENTITY_GRAPH);
    	} else {
    		results = Collections.emptyList();
    	}
    	return new PagedResult<Envelope>(results, maxResults, offset, prs.getTotalCount());
	}

    public PagedResult<Envelope> listConversations(String recipient, Integer maxResults, Integer offset) {
        if (maxResults == null) {
        	maxResults = MAX_RESULTS;
        }
        if (offset == null) {
        	offset = 0;
        }
    	String effectiveRecipient = recipient != null ? recipient : sessionContext.getCallerPrincipal().getName();
    	// Get the total count
    	PagedResult<Long> prs = envelopeDao.listConversations(effectiveRecipient, 0, offset);
    	List<Envelope> results = null;
    	if (maxResults == null || maxResults > 0) {
    		// Get the actual data
        	PagedResult<Long> envIds = envelopeDao.listConversations(effectiveRecipient, maxResults, offset);
        	results = envelopeDao.fetch(envIds.getData(), Envelope.LIST_MY_ENVELOPES_ENTITY_GRAPH);
    	} else {
    		results = Collections.emptyList();
    	}
    	return new PagedResult<Envelope>(results, maxResults, offset, prs.getTotalCount());
    }
    
    public void updateAcknowledgment(Long envelopeId, Instant ackTime) throws NotFoundException {
    	Envelope envdb = envelopeDao.find(envelopeId)
    			.orElseThrow(NotFoundException::new);
    	userManager.checkOwnership(envdb.getRecipient(), Envelope.class.getSimpleName());
    	envdb.setAckTime(ackTime);
    	envelopeDao.merge(envdb);
    }

}
