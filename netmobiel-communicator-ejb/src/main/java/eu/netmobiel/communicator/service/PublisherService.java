package eu.netmobiel.communicator.service;

import java.time.Instant;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Queue;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Envelope;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.repository.EnvelopeDao;

/**
 * Bean class for Publisher enterprise bean. 
 */
@Stateless
public class PublisherService {

    @Resource
    private SessionContext sc;
    @Resource(lookup = "java:module/jms/netmobielMessageProcessor")
    private Queue queue;
    @Inject
    private JMSContext context;

    @Inject
    private Logger logger;

    private EnvelopeDao envelopeDao;
    
    public PublisherService() {
    }

    /**
     * Creates producer and message. Sends messages after setting their NewsType
     * property and using the property value as the message text. Messages are
     * received by MessageBean, a message-driven bean that uses a message
     * selector to retrieve messages whose NewsType property has certain values.
     */
    public void publish(Message msg, String recipients) throws CreateException {
        MapMessage message;
        try {
        	if (logger.isDebugEnabled()) {
                logger.debug(String.format("Send message from %s to %s: %s %s - %s", msg.getSender(), recipients, msg.getContext(), msg.getSubject(), msg.getBody()));
        	}
            message = context.createMapMessage();
            message.setString("text", msg.getBody());
            message.setString("context", msg.getContext());
            message.setString("subject", msg.getSubject());
            message.setString("deliveryMode", msg.getDeliveryMode().name());
            // The sender is the caller for now, unless the sender is set.
            
            message.setString("sender", msg.getSender() != null ? msg.getSender() : sc.getCallerPrincipal().getName());
            message.setString("recipients", recipients);
            context.createProducer().send(queue, message);
        } catch (JMSException ex) {
            logger.error("Error sending message", ex);
            sc.setRollbackOnly();
            throw new CreateException("Failed to publish message", ex);
        }
    }
    
	public List<Envelope> listEnvelopes(String recipient, String context, Instant since, Instant until, Integer maxResults, Integer offset) {
    	String effectiveRecipient = recipient != null ? recipient : sc.getCallerPrincipal().getName();
    	List<Long> ids = envelopeDao.listEnvelopes(effectiveRecipient, context, since, until, maxResults, offset);
    	return envelopeDao.fetch(ids, null);
	}

    public List<Envelope> listConversation(String recipient, Integer maxResults, Integer offset) {
    	String effectiveRecipient = recipient != null ? recipient : sc.getCallerPrincipal().getName();
    	List<Long> ids = envelopeDao.listConverations(effectiveRecipient, maxResults, offset);
    	return envelopeDao.fetch(ids, null);
    }
}
