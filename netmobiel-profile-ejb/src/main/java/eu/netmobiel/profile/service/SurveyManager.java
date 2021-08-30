package eu.netmobiel.profile.service;

import java.time.Instant;
import java.util.Optional;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.UpdateException;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.model.Survey;
import eu.netmobiel.profile.model.SurveyResponse;
import eu.netmobiel.profile.repository.ProfileDao;
import eu.netmobiel.profile.repository.SurveyDao;
import eu.netmobiel.profile.repository.SurveyResponseDao;

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
    private SurveyResponseDao surveyResponseDao;

    /**
     * Checks whether there is a profile related survey relevant for the calling user. 
     * @param managedId
     * @return
     * @throws NotFoundException
     */
	public Optional<Survey> lookupSurvey(String managedId) throws NotFoundException {
    	Profile profile = profileDao.getReferenceByManagedIdentity(managedId)
    			.orElseThrow(() -> new NotFoundException("No such profile: " + managedId));
		Optional<Survey> survey = surveyDao.findSurveyToTake(Instant.now(), profile.getCreationTime());
		if (survey.isPresent()) {
			// Check whether the survey has already been taken
			Optional<SurveyResponse> sr = surveyResponseDao.findResponse(survey.get(), profile);
			if (sr.isPresent() && sr.get().getSubmitTime() != null) {
				// Already finished
				survey = Optional.empty();
			}
		}
		return survey;
	}
	
	public void markSurveyStart(String managedId, String surveyId) throws NotFoundException, UpdateException {
    	Profile profile = profileDao.getReferenceByManagedIdentity(managedId)
    			.orElseThrow(() -> new NotFoundException("No such profile: " + managedId));
		Survey survey = surveyDao.find(surveyId)
				.orElseThrow(() -> new NotFoundException("No such survey: " + surveyId));
			// Check whether the survey has already been taken
		Optional<SurveyResponse> sr = surveyResponseDao.findResponse(survey, profile);
		SurveyResponse srsp = null;
		if (sr.isPresent()) {
			if (sr.get().getSubmitTime() != null) {
				throw new UpdateException("Survey has already been taken");
			}
			srsp = sr.get();
		} else {
			srsp = new SurveyResponse();
			srsp.setProfile(profile);
			srsp.setSurvey(survey);
			surveyResponseDao.save(srsp);
		}
		srsp.setRequestTime(Instant.now());
	}

	public void markSurveyTaken(String managedId, String surveyId) throws NotFoundException, UpdateException {
    	Profile profile = profileDao.getReferenceByManagedIdentity(managedId)
    			.orElseThrow(() -> new NotFoundException("No such profile: " + managedId));
		Survey survey = surveyDao.find(surveyId)
				.orElseThrow(() -> new NotFoundException("No such survey: " + surveyId));
			// Check whether the survey has already been taken
		Optional<SurveyResponse> sr = surveyResponseDao.findResponse(survey, profile);
		SurveyResponse srsp = null;
		if (sr.isPresent()) {
			if (sr.get().getSubmitTime() != null) {
				throw new UpdateException("Survey has already been taken");
			}
			srsp = sr.get();
		} else {
			// Hmmm, should already have a record 
			srsp = new SurveyResponse();
			srsp.setProfile(profile);
			srsp.setSurvey(survey);
			surveyResponseDao.save(srsp);
		}
		srsp.setSubmitTime(Instant.now());
	}
}
