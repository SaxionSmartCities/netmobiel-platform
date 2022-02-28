package eu.netmobiel.overseer.processor;

import java.util.Optional;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.banker.model.Incentive;
import eu.netmobiel.banker.model.Reward;
import eu.netmobiel.banker.service.RewardService;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.communicator.model.Conversation;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.model.UserRole;
import eu.netmobiel.communicator.service.PublisherService;
import eu.netmobiel.profile.event.SurveyCompletedEvent;
import eu.netmobiel.profile.event.SurveyRemovalEvent;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.model.Survey;
import eu.netmobiel.profile.model.SurveyInteraction;

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
	private RewardService rewardService;
	
    @Inject
    private PublisherService publisherService;

    @Inject
    private TextHelper textHelper;
    
    /**
     * Processes a SurveyCompletedEvent: Create a reward (if not issued yet) and attempt to start a ledger transaction.
     * There is no fallback in this method. There is general catch-all 
     * @param surveyCompletedEvent the event.
     */
	@Asynchronous
	public void onSurveyCompleted(@Observes(during = TransactionPhase.AFTER_SUCCESS) SurveyCompletedEvent surveyCompletedEvent) {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Survey completed: %s", surveyCompletedEvent.toString()));
		}
		try {
			// Check whether the reward was already handed out. Theoretically, multiple incentives might exists for 
			// the same fact, so lookup the incentive (the incentive this method is about) first.
			SurveyInteraction si = surveyCompletedEvent.getSurveyInteraction();
			Survey survey = si.getSurvey();
			Profile owner = si.getProfile();  
			Optional<Incentive> optIncentive = rewardService.lookupIncentive(survey.getIncentiveCode());
			if (optIncentive.isEmpty()) {
				logger.warn(String.format("Incentive %s not found for survey %s", survey.getIncentiveCode(), survey.getSurveyId()));
			} else {
				Optional<Reward> optReward;
					optReward = rewardService.lookupRewardByFact(optIncentive.get(), owner, si.getUrn());
				if (optReward.isPresent()) {
					logger.info("Reward on survey completion already given: " + optReward.get().getUrn());
				} else {
					// Create reward
					String rewardContext = surveyCompletedEvent.getSurveyInteraction().getUrn();
					Reward rwd = rewardService.createReward(optIncentive.get(), owner, rewardContext);
					// Assure the conversation exists
					Conversation personalConv = publisherService.lookupOrCreateConversation(rwd.getRecipient(), 
							UserRole.GENERIC, owner.getUrn(), textHelper.createPersonalGenericTopic(), false);
					publisherService.addConversationContext(personalConv, rwd.getUrn());
					String messageText = textHelper.createPremiumRewardText(rwd);
			    	Message msg = new Message();
					msg.setContext(rwd.getUrn());
					msg.setDeliveryMode(DeliveryMode.ALL);
					msg.addRecipient(personalConv, rwd.getUrn());
					msg.setBody(messageText);
					publisherService.publish(msg);
				}
			}
		} catch (BusinessException e) {
			logger.error("Error in onSurveyCompleted: " + e);
		}
    }

	public void onSurveyRemoval(@Observes(during = TransactionPhase.IN_PROGRESS) SurveyRemovalEvent surveyRemovalEvent) throws NotFoundException {
		SurveyInteraction si = surveyRemovalEvent.getSurveyInteraction(); 
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Survey removal: %s", surveyRemovalEvent.toString()));
		}
		Survey s = si.getSurvey();
		Optional<Incentive> optIncentive = rewardService.lookupIncentive(s.getIncentiveCode());
		if (optIncentive.isEmpty()) {
			logger.warn(String.format("Incentive %s not found for survey %s", s.getIncentiveCode(), s.getSurveyId()));
		} else {
			Optional<Reward> optReward = rewardService.lookupRewardByFact(optIncentive.get(), si.getProfile(), si.getUrn());
			if (optReward.isPresent()) {
				rewardService.withdrawReward(optReward.get(), surveyRemovalEvent.isPaymentOnly());
			} else  {
				logger.info(String.format("No reward found concerning incentive %s (for survey %s) for user %s: ", 
						s.getIncentiveCode(), s.getSurveyId(), si.getProfile().getManagedIdentity()));
			}
		}
		
	}
}
