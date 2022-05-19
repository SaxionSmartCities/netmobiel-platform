package eu.netmobiel.overseer.processor;

import java.text.MessageFormat;
import java.time.Instant;

import javax.annotation.Resource;
import javax.annotation.security.RunAs;
import javax.ejb.Asynchronous;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.annotation.Created;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.ExceptionUtil;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.communicator.event.ChatMessageEvent;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Envelope;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.model.UserRole;
import eu.netmobiel.communicator.service.PublisherService;
import eu.netmobiel.profile.event.DelegationActivationConfirmedEvent;
import eu.netmobiel.profile.event.DelegationActivationRequestedEvent;
import eu.netmobiel.profile.event.DelegationTransferCompletedEvent;
import eu.netmobiel.profile.event.DelegationTransferRequestedEvent;
import eu.netmobiel.profile.event.DelegatorAccountCreatedEvent;
import eu.netmobiel.profile.event.DelegatorAccountPreparedEvent;
import eu.netmobiel.profile.filter.DelegationFilter;
import eu.netmobiel.profile.model.Delegation;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.service.DelegationManager;

/**
 * Stateless bean for the management of the high-level delegation process, in particular the communication with end users.
 * 
 * @author Jaap Reitsma
 *
 */
		
@Stateless
@Logging
@RunAs("system") 
public class DelegationProcessor {
	
    @Inject
    private PublisherService publisherService;

    @Inject
    private DelegationManager delegationManager;

    @Resource
    private SessionContext context;

    @Inject
    private Logger logger;
    
    @Inject
    private TextHelper textHelper;


    public void onDelegatorAccountPrepared(@Observes(during = TransactionPhase.IN_PROGRESS) DelegatorAccountPreparedEvent event) throws BusinessException {
	    // The delegator needs to have a number that can receive an SMS. We require a mobile number.
    	if (! publisherService.isValidForMobileMessaging(event.getDelegator())) {
	    	throw new BadRequestException(String.format("The delegator requires a valid mobile phone number. Not valid: '%s'", event.getDelegator().getPhoneNumber()));
	    }
    }

    public void onDelegatorAccountCreated(@Observes(during = TransactionPhase.IN_PROGRESS) DelegatorAccountCreatedEvent event) throws BusinessException {
		String text = textHelper.createDelegatorAccountCreatedText(event.getDelegator(), event.getInitiator());
		publisherService.sendTextMessage(event.getDelegator(), text);
    }

   /**
    * Observes the request of an activation. Must be IN_PROGRESS because the delegation object is altered.
    * @param event the activation request event
    * @throws BusinessException
    */
    public void onDelegationActivationRequested(@Observes(during = TransactionPhase.IN_PROGRESS) DelegationActivationRequestedEvent event) throws BusinessException {
    	// The activation code is already set/renewed
    	// Inform delegator
    	Delegation delegation = event.getDelegation();
		String text = textHelper.createDelegationActivationText(delegation);
		String smsId = publisherService.sendTextMessage(delegation.getDelegator(), text);
		// Update the information in the delegation object (!) 
		delegation.setActivationCodeSentTime(Instant.now());
		delegation.setSmsId(smsId);
    }
    
    /**
     * Observes the confirmation of an activation. 
     * @param event the activation confirm event
     * @throws BusinessException
     */
     public void onDelegationActivationConfirmed(@Observes(during = TransactionPhase.IN_PROGRESS) DelegationActivationConfirmedEvent event) throws BusinessException {
     	Delegation delegation = event.getDelegation();
     	// Inform delegator trough SMS
		String delegatorText = textHelper.createDelegationConfirmedToDelegatorText(delegation);
 		String smsId = publisherService.sendTextMessage(delegation.getDelegator(), delegatorText);
		logger.info(String.format("Activation of delegation %s confirmed in SMS %s", delegation.getId(), smsId));

		// Inform delegate
		// Delegate's conversation context is the delegation
		// Message context is the delegation, so is the recipient's context
    	Message msg = Message.create()
    			.withBody(textHelper.createDelegationConfirmedToDelegateText(delegation))
    			.withContext(delegation.getUrn())
    			.addEnvelope()
	    			.withRecipient(delegation.getDelegate())
	    			.withConversationContext(delegation.getUrn())
	    			.withUserRole(UserRole.DELEGATE)
	    			.withTopic(textHelper.createDelegationTopic(delegation))
	    			.buildConversation()
    			.buildMessage();
		publisherService.publish(msg);
     }

    
    /**
     * Handles the delegation transfer requested event.
     * @param event the transfer event
     * @throws BusinessException 
     */
    public void onDelegationTransferRequested(@Observes(during = TransactionPhase.IN_PROGRESS) DelegationTransferRequestedEvent event) throws BusinessException {
     	Delegation fromDelegation = event.getFrom();
     	Delegation toDelegation = event.getTo();
		// Inform prospected delegate through push message
		// Message context is the prospected delegation, so is the recipient's context
    	Message msg = Message.create()
    			.withBody(textHelper.createTransferDelegationToText(fromDelegation))
    			.withContext(toDelegation.getUrn())
    			.addEnvelope()
	    			.withRecipient(toDelegation.getDelegate())
	    			.withConversationContext(toDelegation.getUrn())
	    			.withUserRole(UserRole.DELEGATE)
	    			.withTopic(textHelper.createDelegationTopic(toDelegation))
	    			.buildConversation()
    			.buildMessage();
		publisherService.publish(msg);
    }

    /**
     * Handles the delegation transfer competed event.
     * @param event the transfer event
     * @throws BusinessException 
     */
    public void onDelegationTransferCompleted(@Observes(during = TransactionPhase.IN_PROGRESS) DelegationTransferCompletedEvent event) throws BusinessException {
     	Delegation fromDelegation = event.getFrom();
     	Delegation toDelegation = event.getTo();
     	if (!event.isImmediate()) {
     	}
		// Inform previous delegate 
		// Message context is the fromDelegation, so is the recipient's context
    	Message msg = Message.create()
    			.withBody(textHelper.createTransferDelegationCompletedText(fromDelegation, toDelegation))
    			.withContext(fromDelegation.getUrn())
    			.addEnvelope()
	    			.withRecipient(fromDelegation.getDelegate())
	    			.withConversationContext(fromDelegation.getUrn())
	    			.withUserRole(UserRole.DELEGATE)
	    			.withTopic(textHelper.createDelegationTopic(toDelegation))
	    			.buildConversation()
    			.buildMessage();
		publisherService.publish(msg);
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
				Message.MessageBuilder mb = Message.create()
						.withBody(message)
						.withContext(delegations.getData().get(0).getDelegatorRef())
						.withDeliveryMode(deliveryMode);
				for (Delegation delegation : delegations.getData()) {
					mb.addEnvelope(delegation.getUrn())
						.withRecipient(delegation.getDelegate())
		    			.withConversationContext(delegation.getUrn())
		    			.withUserRole(UserRole.DELEGATE)
		    			.withTopic(topic)
						.buildConversation();
				}
				Message msg = mb.buildMessage();
				publisherService.publish(msg);
			}
		} catch (BusinessException ex) {
			logger.error("Cannot inform delegates: " + String.join("\n\t", ExceptionUtil.unwindException(ex)));
		}
    }

    /**
     * Inform any delegate about the reception of a chat message by the delegator.
     * @param event the transfer event
     * @throws BusinessException 
     */
    public void onChatMessageCreated(@Observes(during = TransactionPhase.IN_PROGRESS) @Created ChatMessageEvent event) throws BusinessException {
    	for (Envelope env : event.getChatMessage().getEnvelopes()) {
    		if (env.isSender()) {
    			// Delegate would be the sender probably.
    			continue;
    		}
        	context.getBusinessObject(DelegationProcessor.class)
        	.informDelegates(env.getRecipient(), "Direct bericht van " + event.getChatMessage().getSender().getName(), DeliveryMode.ALL);
		} 
	}
    
}
