package eu.netmobiel.profile.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.User_;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.profile.annotation.ProfileDatabase;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.model.Survey;
import eu.netmobiel.profile.model.SurveyInteraction;
import eu.netmobiel.profile.model.SurveyInteraction_;
import eu.netmobiel.profile.model.Survey_;


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
	 * Retrieves the survey interactions according the search criteria.
     * @param managedId the managed id of the user for whom to list the survey interactions. 
     * @param surveyId the provider ID of the survey interaction to lookup.
     * @param completedToo If true then return also interactions that have been completed.
     * @param cursor the max results and offset. 
	 * @return A pages result. Total count is determined only when maxResults is set to 0.
	 */
	public PagedResult<Long> listSurveyInteractions(String managedId, String surveyId, boolean completedToo, Cursor cursor) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<SurveyInteraction> root = cq.from(SurveyInteraction.class);
        List<Predicate> predicates = new ArrayList<>();
        Instant now = Instant.now();
        if (managedId != null) {
        	predicates.add(cb.equal(root.get(SurveyInteraction_.profile).get(User_.managedIdentity), managedId));
        }
        if (surveyId != null) {
        	predicates.add(cb.equal(root.get(SurveyInteraction_.survey).get(Survey_.surveyId), surveyId));
        }
        if (!completedToo) {
        	// If the submit time is set, the then there is (should be) a rewarding process active or starting soon.
        	predicates.add(cb.isNull(root.get(SurveyInteraction_.submitTime)));
        	// If the end time is set, then the end time should be somewhere after now 
        	predicates.add(cb.or(
        			cb.isNull(root.get(SurveyInteraction_.survey).get(Survey_.endTime)),
        			cb.greaterThan(root.get(SurveyInteraction_.survey).get(Survey_.endTime), now))
        	);
        }
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = null;
        if (cursor.isCountingQuery()) {
          cq.select(cb.count(root.get(SurveyInteraction_.id)));
          totalCount = em.createQuery(cq).getSingleResult();
        } else {
	        cq.select(root.get(SurveyInteraction_.id));
	        Expression<?> sortBy = root.get(SurveyInteraction_.id);
	        cq.orderBy(cb.desc(sortBy));
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(cursor.getOffset());
			tq.setMaxResults(cursor.getMaxResults());
			results = tq.getResultList();
        }
        return new PagedResult<>(results, cursor, totalCount);
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
