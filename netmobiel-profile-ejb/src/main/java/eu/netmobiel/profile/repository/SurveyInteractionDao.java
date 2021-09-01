package eu.netmobiel.profile.repository;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.profile.annotation.ProfileDatabase;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.model.Survey;
import eu.netmobiel.profile.model.SurveyInteraction;


@ApplicationScoped
@Typed(SurveyInteractionDao.class)
public class SurveyInteractionDao extends AbstractDao<SurveyInteraction, Long> {

	@Inject @ProfileDatabase
    private EntityManager em;

    public SurveyInteractionDao() {
		super(SurveyInteraction.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

	/**
	 * Finds optionally an interaction for a given survey and profile.
	 * @param survey the survey to look for.
	 * @param profile the profile to look for.
	 * @return An optional with the record found, if any. If none is found, then the user has never seen the invitation. 
	 */
	public Optional<SurveyInteraction> findInteraction(Survey survey, Profile profile) {
        if (survey == null || profile == null) {
        	throw new IllegalArgumentException("Survey and profile are mandatory parameters");
        }
    	List<SurveyInteraction> results = em.createQuery("from SurveyInteraction where " 
				+ "id.survey = :survey and id.profile = :profile", SurveyInteraction.class)
			.setParameter("survey", survey)
			.setParameter("profile", profile)
			.getResultList();
    	return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0)); 
	}
}
