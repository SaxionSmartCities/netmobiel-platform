package eu.netmobiel.profile.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.event.RewardEvent;
import eu.netmobiel.commons.event.RewardRollbackEvent;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.UpdateException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.EventFireWrapper;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.model.Survey;
import eu.netmobiel.profile.model.SurveyInteraction;
import eu.netmobiel.profile.repository.ProfileDao;
import eu.netmobiel.profile.repository.SurveyDao;
import eu.netmobiel.profile.repository.SurveyInteractionDao;

/**
 * Bean class for the Survey service.  
 */
@Stateless
@Logging
public class SurveyManager {
	public static final Integer MAX_RESULTS = 10; 

	@SuppressWarnings("unused")
	@Inject
    private Logger logger;

    @Inject
    private ProfileDao profileDao;
    
    @Inject
    private SurveyDao surveyDao;
    
    @Inject
    private SurveyInteractionDao surveyInteractionDao;

    @Inject
    private Event<RewardEvent> rewardEvent;

    @Inject
    private Event<RewardRollbackEvent> rewardRollbackEvent;

    
    /**
     * List the survey interaction according the criteria. 
     * @param managedId the managed id of the user for whom to list the survey interactions. 
     * @param surveyId the provider ID of the survey interaction to lookup.
     * @param completedToo If true then return also interactions that have been completed or have expired.
     * @param incentiveCode If set then use an incentive code to lookup an survey.
     * @param cursor the max results and offset. 
     * @return A paged list of survey interactions.
     * @throws BadRequestException 
     */
	public PagedResult<SurveyInteraction> listSurveyInteractions(String managedId, String surveyId, boolean completedToo, String incentiveCode, Cursor cursor) 
			throws NotFoundException, BadRequestException {
    	cursor.validate(MAX_RESULTS, 0);
    	PagedResult<Long> prs = surveyInteractionDao.listSurveyInteractions(managedId, surveyId, completedToo, incentiveCode, Cursor.COUNTING_CURSOR);
    	List<SurveyInteraction> results = null;
    	if (prs.getTotalCount() > 0 && !cursor.isCountingQuery()) {
    		// Get the actual data
    		PagedResult<Long> pids = surveyInteractionDao.listSurveyInteractions(managedId, surveyId, completedToo, incentiveCode, cursor);
    		results = surveyInteractionDao.loadGraphs(pids.getData(), SurveyInteraction.SURVEY_PROFILE_ENTITY_GRAPH, SurveyInteraction::getId);
    	}
    	return new PagedResult<>(results, cursor, prs.getTotalCount());
	}

	/**
     * Invites a user (a profile) to take part in a survey (the one being active, at most one). The invitation does not yet contain the url, use
     * the method getSurveyInteraction to retrieve the survey url.
     * @param managedId the profile asking the question.
     * @return An Optional with a survey interaction record. The survey itself is attached as well.
     * @throws NotFoundException when the profile does not exist.
     */
	public Optional<SurveyInteraction> inviteToSurvey(String managedId) throws NotFoundException {
    	Profile profile = profileDao.getReferenceByManagedIdentity(managedId)
    			.orElseThrow(() -> new NotFoundException("No such profile: " + managedId));
		Optional<Survey> survey = surveyDao.findSurveyToTake(Instant.now(), profile.getCreationTime());
		Optional<SurveyInteraction> si = Optional.empty();
		if (survey.isPresent()) {
			// Check whether the survey interaction has already been created
			si = surveyInteractionDao.findInteraction(survey.get(), profile);
			if (si.isEmpty()) {
				SurveyInteraction sia = new SurveyInteraction(survey.get(), profile, profile.getCreationTime());
				surveyInteractionDao.save(sia);
				// A fresh interaction
				si = Optional.of(sia);
			}
			if (si.get().getSubmitTime() != null) {
				// Already finished
				si = Optional.empty();
			} else {
				si.get().incrementInvitationCount();
			}
		}
		// Because the survey was also fetched, the survey attached to the interaction is available to the REST layer
		return si;
	}

    /**
     * Returns the survey interaction with the specified id. 
     * @param id the od of the survey interaction.
     * @return the survey interaction object.
     * @throws NotFoundException when the object does not exist.
     */
	public SurveyInteraction getSurveyInteraction(Long id) throws NotFoundException {
		SurveyInteraction si = surveyInteractionDao.loadGraph(id, SurveyInteraction.SURVEY_PROFILE_ENTITY_GRAPH)
				.orElseThrow(() -> new NotFoundException("No such surveyInteraction: " + id));
		si.setSurveyUrl(String.format("https://saxion.eu.qualtrics.com/jfe/form/%s?NetmobielID=%s", 
				si.getSurvey().getSurveyId(), si.getProfile().getManagedIdentity()));
		return si;
	}

	/**
	 * Marks the survey interaction of the specified profile and survey for a redirection.
	 * @param managedId the calling profile.
	 * @param providerSurveyId the survey as known at the provider.
	 * @throws NotFoundException if profile or survey cannot be found.
	 * @throws UpdateException if the interaction was already completed or when there is no preceding invitation.
	 * @throws BadRequestException when the user has not been invited yet (can this ever happen?).
	 */
	public void markSurveyRedirect(Long surveyInteractionId) throws NotFoundException, UpdateException, BadRequestException {
		SurveyInteraction si = getSurveyInteraction(surveyInteractionId);
		if (si.getSubmitTime() != null) {
			throw new UpdateException(String.format("Survey %s has already been submitted by %s", 
					si.getSurvey().getSurveyId(), si.getProfile().getManagedIdentity()));
		}
		if (si.getInvitationTime() == null) {
			throw new BadRequestException(String.format("User %s has never been invited for Survey %s", 
					si.getProfile().getManagedIdentity(), si.getSurvey().getSurveyId()));
		}
		if (si.isExpired()) {
			throw new UpdateException(String.format("Survey %s has expired for user %s", 
					si.getSurvey().getSurveyId(), si.getProfile().getManagedIdentity()));
		}
		if (si.getRedirectTime() == null) {
			si.setRedirectTime(Instant.now());
		}
		si.incrementRedirectCount();
	}

	/**
	 * Marks the survey interaction of the specified profile and survey as completed.
	 * @param managedId the calling profile.
	 * @param providerSurveyId the survey as known at the provider.
	 * @throws NotFoundException if profile or survey cannot be found.
	 * @throws UpdateException if the interaction was already completed or when there is no preceding invitation.
	 * @throws BadRequestException if called without prior invitation.
	 */
	public void markSurveySubmitted(Long surveyInteractionId) throws NotFoundException, UpdateException, BadRequestException {
		SurveyInteraction si = getSurveyInteraction(surveyInteractionId);
		if (si.getSubmitTime() != null) {
			throw new UpdateException(String.format("Survey %s has already been submitted by %s", 
					si.getSurvey().getSurveyId(), si.getProfile().getManagedIdentity()));
		}
		if (si.getInvitationTime() == null) {
			throw new BadRequestException(String.format("User %s has never been invited for Survey %s", 
					si.getProfile().getManagedIdentity(), si.getSurvey().getSurveyId()));
		}
		if (si.getRedirectTime() == null) {
			throw new BadRequestException(String.format("User %s has never been redirected to Survey %s", 
					si.getProfile().getManagedIdentity(), si.getSurvey().getSurveyId()));
		}
		if (si.isExpired()) {
			throw new UpdateException(String.format("Survey %s has expired for user %s", 
					si.getSurvey().getSurveyId(), si.getProfile().getManagedIdentity()));
		}
		si.setSubmitTime(Instant.now());
		// Mark the completion of the survey to postprocessing services (should use on-success option)
		rewardEvent.fire(createRewardEvent(si));
	}

	/**
	 * Reverts a survey interaction for testing purposes. The security is already checked at this stage. 
	 * @param surveyProviderId the survey interaction id.
	 * @param hard If true then remove the object from the database. Default false.
	 */
	public void revertSurveyInteraction(Long surveyInteractionId, boolean hard) throws BusinessException {
		SurveyInteraction si = getSurveyInteraction(surveyInteractionId);
		if (si.getSubmitTime() != null) {
			// Revert reward (and payment). This is a synchronous event.
			EventFireWrapper.fire(rewardRollbackEvent, createRewardRollbackEvent(si));
			si.setSubmitTime(null);
		}
		if (hard) {
			surveyInteractionDao.remove(si);
		}
	}
	
	private static RewardEvent createRewardEvent(SurveyInteraction si) {
		return new RewardEvent(si.getSurvey().getIncentiveCode(), si.getProfile(), si.getUrn()); 
	}
	
	private static RewardRollbackEvent createRewardRollbackEvent(SurveyInteraction si) {
		return new RewardRollbackEvent(si.getSurvey().getIncentiveCode(), si.getProfile(), si.getUrn()); 
	}
}
