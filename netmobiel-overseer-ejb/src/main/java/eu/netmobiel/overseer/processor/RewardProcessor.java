package eu.netmobiel.overseer.processor;

import java.util.Optional;

import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.banker.model.Incentive;
import eu.netmobiel.banker.model.Reward;
import eu.netmobiel.banker.service.LedgerService;
import eu.netmobiel.banker.service.RewardService;
import eu.netmobiel.commons.event.RewardEvent;
import eu.netmobiel.commons.event.RewardRollbackEvent;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.model.UserRole;
import eu.netmobiel.communicator.service.PublisherService;

/**
 * Stateless bean for managing incentives and rewards.
 *  
 * @author Jaap Reitsma
 *
 */
		
@Stateless
public class RewardProcessor {
	@Inject
    private Logger logger;
    
	@Inject
	private LedgerService ledgerService;
	
	@Inject
	private RewardService rewardService;
	
    @Inject
    private PublisherService publisherService;

    @Inject
    private TextHelper textHelper;
    
    @Resource
	private SessionContext sessionContext;

    /**
     * Sends a message concerning the reward to the personal conversation. That conversation is attached to the managed identity of 
     * the user and acts as container for all personal (non-trip/ride) messages. 
     * @param reward
     * @throws BusinessException
     */
    private void sendPersonalMessage(Reward reward) throws BusinessException {
		// Assure the conversation exists
    	// The message context is the reward
    	// The conversation context is the keycloak URN (for personal messages)
    	NetMobielUser owner = reward.getRecipient();
    	Message msg = Message.create()
    			.withBody(textHelper.createRewardText(reward))
    			.withContext(reward.getUrn())
    			.addEnvelope()
	    			.withRecipient(owner)
	    			.withConversationContext(owner.getKeyCloakUrn())
	    			.withUserRole(UserRole.GENERIC)
	    			.withTopic(textHelper.createPersonalGenericTopic())
	    			.buildConversation()
    			.buildMessage();
		publisherService.publish(msg);
    }

    public void handleOnReward(RewardEvent rewardEvent) throws BusinessException {
		final var code = rewardEvent.getIncentiveCode();
		// Check whether the reward was already handed out. Theoretically, multiple incentives might exists for 
		// the same fact, so lookup the incentive (the incentive this method is about) first.
		Optional<Incentive> optIncentive = rewardService.lookupIncentive(code);
		if (optIncentive.isEmpty()) {
			logger.warn(String.format("No such incentive: %s", code));
		} else {
			final var incentive = optIncentive.get();
			final var recipient = rewardEvent.getRecipient();
			if (incentive.getDisableTime() != null) {
				logger.warn(String.format("No reward, incentive %s is disabled since %s", code, incentive.getDisableTime().toString()));
			} else if (!incentive.isRedemption() || ledgerService.canRedeemSome(recipient)) {
				// The reward is absolute or there is some premium left to redeem
				final var fact = rewardEvent.getFactContext(); 
				Optional<Reward> optReward = rewardService.lookupRewardByFact(incentive, recipient, fact);
				Reward reward = null;
				if (optReward.isEmpty()) {
					// Create reward
					reward = rewardService.createReward(optIncentive.get(), recipient, fact, rewardEvent.getYield());
					sendPersonalMessage(reward);
				} else {
					// Reward already exists
					reward = optReward.get();
					if (reward.getCancelTime() != null) {
						// Ok, restore the original reward
						reward = rewardService.restoreReward(reward, rewardEvent.getYield());
						sendPersonalMessage(reward);
					}
				}
			}
		}
    }
    /**
     * Processes a RewardEvent: Create a reward (if not issued yet) and attempt to start a ledger transaction.
     * This operation is asynchronous as there is no use to let the caller wait for the result.
     * @param rewardEvent the event to process.
     */
	@Asynchronous
	public void onReward(@Observes(during = TransactionPhase.AFTER_SUCCESS) RewardEvent rewardEvent) {
		if (logger.isDebugEnabled()) {
			logger.debug(rewardEvent.toString());
		}
		try {
			// Force start of transaction
			sessionContext.getBusinessObject(RewardProcessor.class).handleOnReward(rewardEvent);
		} catch (BusinessException e) {
			logger.error("Error in onReward: " + e);
		}
    }

	/**
	 * Performs a rollback of an earlier reward. This is an in-progress operation to assure consistency. 
	 * A reward can be rolled back even if disabled, for now.
	 * @param rewardRollbackEvent the reward to rollback
	 * @throws NotFoundException
	 */
	public void onRewardRollback(@Observes(during = TransactionPhase.IN_PROGRESS) RewardRollbackEvent rewardRollbackEvent) throws NotFoundException {
		if (logger.isDebugEnabled()) {
			logger.debug(rewardRollbackEvent.toString());
		}
		final var code = rewardRollbackEvent.getIncentiveCode();
		Optional<Incentive> optIncentive = rewardService.lookupIncentive(code);
		if (optIncentive.isEmpty()) {
			logger.warn("No such incentive: " + code);
		} else {
			final var incentive = optIncentive.get();
			final var recipient = rewardRollbackEvent.getRecipient(); 
			final var fact = rewardRollbackEvent.getFactContext(); 
			Optional<Reward> optReward = rewardService.lookupRewardByFact(incentive, recipient, fact);
			if (optReward.isPresent()) {
				// Always a soft remove
				rewardService.removeReward(optReward.get().getId(), false, rewardRollbackEvent.isPaymentOnly());
			}
		}
		
	}
}
