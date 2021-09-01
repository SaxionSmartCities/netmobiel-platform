package eu.netmobiel.profile.service;

import java.time.Instant;
import java.util.Optional;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.UpdateException;
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

	@Resource
    private SessionContext sessionContext;

    @SuppressWarnings("unused")
	@Inject
    private Logger logger;

    @Inject
    private ProfileDao profileDao;
    
    @Inject
    private SurveyDao surveyDao;
    
    @Inject
    private SurveyInteractionDao surveyInteractionDao;

    /**
     * Invites a user (a profile) to take part in a survey (the one being active, at most one) 
     * @param managedId the profile asking the question.
     * @return An Optional with a survey interaction record. The survey itself is attached as well.
     * @throws NotFoundException when the profile does not exist.
     */
	public Optional<SurveyInteraction> inviteToSurvey(String managedId) throws NotFoundException {
    	Profile profile = profileDao.getReferenceByManagedIdentity(managedId)
    			.orElseThrow(() -> new NotFoundException("No such profile: " + managedId));
		Optional<Survey> survey = surveyDao.findSurveyToTake(Instant.now(), profile.getCreationTime());
		Optional<SurveyInteraction> si = null;
		if (! survey.isPresent()) {
			// No active survey
			si = Optional.empty();
		} else {
			// Check whether the survey interaction has already been created
			si = surveyInteractionDao.findInteraction(survey.get(), profile);
			if (! si.isPresent()) {
				SurveyInteraction sia = new SurveyInteraction(survey.get(), profile);
				surveyInteractionDao.save(sia);
				// A fresh interaction
				si = Optional.of(sia);
			}
			if (si.get().getSubmitTime() != null) {
				// Already finished
				si = Optional.empty();
			} else {
				si.get().incrementInvitationCount();
				survey.get().setProviderUrl(String.format("https://saxion.eu.qualtrics.com/jfe/form/%s?NetmobielID=%s", 
						survey.get().getSurveyId(), managedId));
			}
		}
		// Because the survey was also fetched, the survey attached to the interaction is available to the REST layer
		return si;
	}

	/**
	 * Marks the survey interaction of the specified profile and survey for a redirection.
	 * @param managedId the calling profile.
	 * @param surveyId the survey.
	 * @throws NotFoundException if profile or survey cannot be found.
	 * @throws UpdateException if the interaction was already completed or when there is no preceding invitation.
	 * @throws BadRequestException when the user has not been invited yet (can this ever happen?).
	 */
	public void markSurveyRedirect(Long surveyInteractionId) throws NotFoundException, UpdateException, BadRequestException {
		Optional<SurveyInteraction> si = surveyInteractionDao.find(surveyInteractionId);
		if (! si.isPresent()) {
			throw new NotFoundException("No such survey interaction: " + surveyInteractionId);
		}
		if (si.get().getSubmitTime() != null) {
			throw new UpdateException(String.format("Survey has already been submitted on %s", si.get().getSubmitTime()));
		}
		if (si.get().getRedirectTime() == null) {
			si.get().setRedirectTime(Instant.now());
		}
		si.get().incrementRedirectCount();
	}

	/**
	 * Marks the survey interaction of the specified profile and survey as completed.
	 * @param managedId the calling profile.
	 * @param providerSurveyId the survey as known at the provider.
	 * @throws NotFoundException if profile or survey cannot be found.
	 * @throws UpdateException if the interaction was already completed or when there is no preceding invitation.
	 * @throws BadRequestException if called without prior invitation.
	 */
	public void onSurveySubmitted(String managedId, String providerSurveyId) throws NotFoundException, UpdateException, BadRequestException {
    	Profile profile = profileDao.getReferenceByManagedIdentity(managedId)
    			.orElseThrow(() -> new NotFoundException("No such profile: " + managedId));
		Survey survey = surveyDao.findSurveyByProviderReference(providerSurveyId)
				.orElseThrow(() -> new NotFoundException("No such survey: " + providerSurveyId));
			// Check whether the survey has already been taken
		Optional<SurveyInteraction> si = surveyInteractionDao.findInteraction(survey, profile);
		if (! si.isPresent()) {
			throw new BadRequestException(String.format("Cannot set submit time of %s, first invite user %s", providerSurveyId, managedId));
		}
		if (si.get().getSubmitTime() != null) {
			throw new UpdateException(String.format("Survey %s has already been submitted by %s", providerSurveyId, managedId));
		}
		si.get().setSubmitTime(Instant.now());
	}
}
