package eu.netmobiel.overseer.processor;

import java.time.Instant;
import java.util.List;
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
import eu.netmobiel.commons.event.RewardEvaluationEvent;
import eu.netmobiel.commons.event.RewardEvent;
import eu.netmobiel.commons.event.RewardRollbackEvent;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.model.UserRole;
import eu.netmobiel.communicator.service.PublisherService;
import eu.netmobiel.rideshare.service.RideManager;

/**
 * Stateless bean for managing incentives and rewards.
 *  
 * @author Jaap Reitsma
 *
 */
		
@Stateless
public class RewardProcessor {
	// These numbers must be matched with the messages in the repeated-ride incentive.
	public static final int REPEATED_RIDE_WINDOW_DAYS = 30; 
	public static final int REPEATED_RIDE_MINIMUM_RIDES = 4;
	
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
    
	@Inject
	private RideManager rideManager;
	
    @Resource
	private SessionContext sessionContext;

    /**
     * Tool to check whether a transaction is active.
     * The status starts at 0, see javax.transaction.Status.
     */
//	@Resource
//	private TransactionSynchronizationRegistry transactionSynchronisationRegistry;
//	private static final String [] TRANSACTION_STATUS = {
//		"STATUS_ACTIVE",
//		"STATUS_MARKED_ROLLBACK",
//		"STATUS_PREPARED",
//		"STATUS_COMMITTED",
//		"STATUS_ROLLEDBACK",
//		"STATUS_UNKNOWN",
//		"STATUS_NO_TRANSACTION",
//		"STATUS_PREPARING",
//		"STATUS_COMMITTING",
//		"STATUS_ROLLING_BACK",
//	};
//
//	private void showTransactionStatus(String contextName) {
//		if (logger.isDebugEnabled()) {
//			logger.debug(String.format("%s: Key %s Transaction status is %d %s",
//					contextName,
//					transactionSynchronisationRegistry.getTransactionKey(),
//					transactionSynchronisationRegistry.getTransactionStatus(), 
//					TRANSACTION_STATUS[transactionSynchronisationRegistry.getTransactionStatus()]));
//		}
//	}

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

    private void handOutReward(Incentive incentive, NetMobielUser recipient, String fact, Integer yield) throws BusinessException {
		Instant now = Instant.now();
		if (!incentive.isActive(now)) {
			logger.warn(String.format("No reward, incentive %s is not active", incentive.getCode()));
			return;
		}
		if (!incentive.isRedemption() || ledgerService.canRedeemSome(recipient)) {
			// The reward is absolute or there is some premium left to redeem
			// Check whether the reward was already handed out. Theoretically, multiple incentives might exists for 
			// the same fact.
			Optional<Reward> optReward = rewardService.lookupRewardByFact(incentive, recipient, fact);
			Reward reward = null;
			if (optReward.isEmpty()) {
				// Create reward
				reward = rewardService.createReward(incentive, recipient, fact, yield);
				sendPersonalMessage(reward);
			} else {
				// Reward already exists
				reward = optReward.get();
				if (reward.getCancelTime() != null) {
					// Ok, restore the original reward
					reward = rewardService.restoreReward(reward, yield);
					sendPersonalMessage(reward);
				}
			}
		}
    }
    
	private void revokeReward(Incentive incentive, NetMobielUser recipient, String fact, boolean paymentOnly) throws NotFoundException {
		Instant now = Instant.now();
		if (!incentive.isActive(now)) {
			logger.warn(String.format("No reward, incentive %s is not active", incentive.getCode()));
			return;
		}
		Optional<Reward> optReward = rewardService.lookupRewardByFact(incentive, recipient, fact);
		if (optReward.isPresent()) {
			// Always a soft remove
			rewardService.removeReward(optReward.get().getId(), false, paymentOnly);
		}
	}

	private void evaluateRepeatedRideIncentive(Incentive incentive, NetMobielUser recipient, String fact) throws BusinessException {
		// Determine the period to check. Take the same period as the incentive.
		// This is the period that a decision can be taken on the reward. 
		// Outside this period nothing will happen with this specific reward.
		Instant startTime = incentive.getStartTime();
		if (startTime == null) {
			startTime = Instant.parse("2022-04-01 00:00:00");
		}
		Instant endTime = incentive.getEndTime();
		if (endTime == null) {
			endTime = Instant.now().plusSeconds(60 * 60 * 24 * 7 * RideManager.HORIZON_WEEKS);  
		}
		boolean issueReward = rideManager.matchesRepeatedRideCondition(recipient, 
					startTime, endTime, REPEATED_RIDE_WINDOW_DAYS, REPEATED_RIDE_MINIMUM_RIDES);
		if (issueReward) {
			handOutReward(incentive, recipient, fact, null);
		} else {
			// Check if reward has been handed out, if so then revoke.
			revokeReward(incentive, recipient, fact, false);
		}
	}
	
    /**
     * Processes a RewardEvent: Create a reward (if not issued yet) and attempt to start a ledger transaction.
     * This operation is asynchronous as there is no use to let the caller wait for the result.
     * @param event the event to process.
     */
	@Asynchronous
	public void onReward(@Observes(during = TransactionPhase.AFTER_SUCCESS) RewardEvent event) {
//		showTransactionStatus("onReward");
		if (logger.isDebugEnabled()) {
			logger.debug(event.toString());
		}
		try {
			final var code = event.getIncentiveCode();
			Optional<Incentive> optIncentive = rewardService.lookupIncentive(code);
			if (optIncentive.isEmpty()) {
				logger.warn("No such incentive: " + code);
			} else {
				handOutReward(optIncentive.get(), event.getRecipient(), event.getFactContext(), event.getYield());
			}
		} catch (Exception e) {
			logger.error("Error in onReward: " + e);
		}
    }

	/**
	 * In case a method sends multiple rewards, then handle them in sequence, to prevent troubles with conversation creation. 
	 * @param events
	 */
	@Asynchronous
	public void onRewards(@Observes(during = TransactionPhase.AFTER_SUCCESS) List<RewardEvent> events) {
		for (RewardEvent event : events) {
			onReward(event);
		}
	}

	/**
	 * Performs a rollback of an earlier reward. This is an in-progress operation to assure consistency. 
	 * A reward can be rolled back even if disabled, for now.
	 * @param event the reward to rollback
	 * @throws NotFoundException
	 */
	public void onRewardRollback(@Observes(during = TransactionPhase.IN_PROGRESS) RewardRollbackEvent event) throws NotFoundException {
//		showTransactionStatus("onRewardRollback");
		if (logger.isDebugEnabled()) {
			logger.debug(event.toString());
		}
		final var code = event.getIncentiveCode();
		Optional<Incentive> optIncentive = rewardService.lookupIncentive(code);
		if (optIncentive.isEmpty()) {
			logger.warn("No such incentive: " + code);
		} else {
			revokeReward(optIncentive.get(), event.getRecipient(), 
					event.getFactContext(), event.isPaymentOnly());
		}
	}

	/**
     * Processes a RewardEvaluationEvent: This is a trigger to evaluate whether a reward is to handed out, or perhaps revoked.
     * This operation is asynchronous as there is no use to let the caller wait for the result.
     * @param event the event to process.
     */
	@Asynchronous
	public void onRewardEvaluation(@Observes(during = TransactionPhase.AFTER_SUCCESS) RewardEvaluationEvent event) {
//		showTransactionStatus("onRewardEvaluation");
		if (logger.isDebugEnabled()) {
			logger.debug(event.toString());
		}
		try {
			final Optional<Incentive> optIncentive = rewardService.lookupIncentive(event.getIncentiveCode());
			if (optIncentive.isEmpty()) {
				logger.error("No such incentive: " + event.getIncentiveCode());
				return;
			}
			final Incentive inc = optIncentive.get();
			Instant now = Instant.now();
			if (!inc.isActive(now)) {
				if (logger.isDebugEnabled()) {
					logger.error("Incentive is not active: " + inc.getCode());
				}
				return;
			}
			if (inc.getCode().equals(RideManager.INCENTIVE_CODE_REPEATED_RIDE)) {
				evaluateRepeatedRideIncentive(inc, event.getRecipient(), event.getFactContext());
			} else {
				logger.error("Don't know how to evaluate this incentive: " + event.getIncentiveCode());
			}
		} catch (Exception e) {
			logger.error("Error in onRewardEvaluation: " + e);
		}
    }

}
