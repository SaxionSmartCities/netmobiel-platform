package eu.netmobiel.overseer.processor;

import java.text.MessageFormat;
import java.time.Instant;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.RunAs;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.service.PublisherService;
import eu.netmobiel.profile.event.DelegationActivationConfirmedEvent;
import eu.netmobiel.profile.event.DelegationActivationRequestedEvent;
import eu.netmobiel.profile.event.DelegationTransferCompletedEvent;
import eu.netmobiel.profile.event.DelegationTransferRequestedEvent;
import eu.netmobiel.profile.event.DelegatorAccountCreatedEvent;
import eu.netmobiel.profile.event.DelegatorAccountPreparedEvent;
import eu.netmobiel.profile.model.Delegation;

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


    @Resource
    private SessionContext context;

    @Inject
    private Logger logger;
    
    @PostConstruct
    public void initialize() {
    }

    public void onDelegatorAccountPrepared(@Observes(during = TransactionPhase.IN_PROGRESS) DelegatorAccountPreparedEvent event) throws BusinessException {
	    // The delegator needs to have a number that can receive an SMS. We require a mobile number.
    	if (! publisherService.isValidForMobileMessaging(event.getDelegator())) {
	    	throw new BadRequestException(String.format("The delegator requires a valid mobile phone number. Not valid: '%s'", event.getDelegator().getPhoneNumber()));
	    }
    }

    public void onDelegatorAccountCreated(@Observes(during = TransactionPhase.IN_PROGRESS) DelegatorAccountCreatedEvent event) throws BusinessException {
		String text = String.format("U hebt een account bij NetMobiel: %s. Uw registratie is uitgevoerd door %s.", 
				event.getDelegator().getNameAndEmail(), 
				event.getInitiator().getNameEmailPhone());
		publisherService.sendTextMessage(text, event.getDelegator());
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
		String text = String.format(
				"%s biedt aan om uw reizen met NetMobiel voor u te beheren. "
						+ "Uw instemming geeft u door deze persoon desgevraagd de volgende code te geven: %s" 
						+ "De code is maximaal 24 uur geldig.", 
						delegation.getDelegate().getName(), delegation.getActivationCode());
		String smsId = publisherService.sendTextMessage(text, delegation.getDelegator());
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
		String delegatorText = String.format("%s beheert vanaf nu uw reizen met NetMobiel.", delegation.getDelegate().getName());
 		String smsId = publisherService.sendTextMessage(delegatorText, delegation.getDelegator());
		logger.info(String.format("Activation of delegation %s confirmed in SMS %s", delegation.getId(), smsId));

		// Inform delegate through push message 
 		Message msg = new Message();
		msg.setContext(delegation.getUrn());
		msg.setSubject("Nieuwe Netmobiel cliënt");
		msg.setBody(
				MessageFormat.format("U beheert vanaf nu de reizen met NetMobiel van {0}.", delegation.getDelegator().getName())
				);
		msg.setDeliveryMode(DeliveryMode.NOTIFICATION);
		msg.addRecipient(delegation.getDelegate());
		publisherService.publish(null, msg);
     }

    
    /**
     * Handles the delegation transfer requested event.
     * @param event the transfer event
     * @throws BusinessException 
     */
    public void onDelegationTransferRequested(@Observes(during = TransactionPhase.IN_PROGRESS) DelegationTransferRequestedEvent event) {
     	Delegation fromDelegation = event.getFrom();
		// Inform prospected delegate through push message 
 		Message msg = new Message();
		msg.setContext(fromDelegation.getUrn());
		msg.setSubject("Aanvraag overdracht Netmobiel cliënt");
		msg.setBody(
				MessageFormat.format("{0} vraagt u om het beheer van de reizen in Netmobiel namens {1} over te nemen. " 
						+ "Vraag {2} om de via SMS ontvangen activeringscode en vul deze in op het overdrachtsformulier in de app." 
						+ "U dient de activatie binnen 24 uur uit te voeren.", 
						fromDelegation.getDelegate().getName(), 
						fromDelegation.getDelegator().getName(),
						fromDelegation.getDelegator().getGivenName())
				);
		msg.setDeliveryMode(DeliveryMode.NOTIFICATION);
		msg.addRecipient(fromDelegation.getDelegate());
		publisherService.publish(null, msg);
    }

    /**
     * Handles the delegation transfer competed event.
     * @param event the transfer event
     * @throws BusinessException 
     */
    public void onDelegationTransferCompleted(@Observes(during = TransactionPhase.IN_PROGRESS) DelegationTransferCompletedEvent event) {
     	Delegation fromDelegation = event.getFrom();
     	Delegation toDelegation = event.getTo();
     	if (!event.isImmediate()) {
     	}
		// Inform previous delegate through push message 
 		Message msg = new Message();
		msg.setContext(fromDelegation.getUrn());
		msg.setSubject("Netmobiel cliënt is overgedragen");
		msg.setBody(
				MessageFormat.format("Uw beheer van de reizen in Netmobiel namens {0} is overgedragen aan {1}.", 
						fromDelegation.getDelegator().getName(), toDelegation.getDelegate().getName())
				);
		msg.setDeliveryMode(DeliveryMode.NOTIFICATION);
		msg.addRecipient(fromDelegation.getDelegate());
		publisherService.publish(null, msg);
    }
}
