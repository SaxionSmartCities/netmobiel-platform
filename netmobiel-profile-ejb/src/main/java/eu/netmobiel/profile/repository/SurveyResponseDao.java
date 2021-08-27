package eu.netmobiel.profile.repository;

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
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.profile.annotation.ProfileDatabase;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.model.Survey;
import eu.netmobiel.profile.model.SurveyResponse;
import eu.netmobiel.profile.model.SurveyResponse_;


@ApplicationScoped
@Typed(SurveyResponseDao.class)
public class SurveyResponseDao extends AbstractDao<SurveyResponse, Long> {

	@Inject @ProfileDatabase
    private EntityManager em;

    public SurveyResponseDao() {
		super(SurveyResponse.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

	/**
	 * Retrieves the Places according the search criteria.
	 * @param filter the filter criteria. 
	 * @param cursor The cursor to use.
	 * @return A pages result. Total count is determined only when maxResults is set to 0.
	 */
	public PagedResult<Long> listResponses(Survey survey, Profile profile, Cursor cursor) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<SurveyResponse> root = cq.from(SurveyResponse.class);
        Long totalCount = null;
        List<Long> results = null;
        List<Predicate> predicates = new ArrayList<>();
        if (survey != null) {
	        predicates.add(cb.equal(root.get(SurveyResponse_.survey), survey));
        }        
        if (profile != null) {
	        predicates.add(cb.equal(root.get(SurveyResponse_.profile), profile));
        }        
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        if (cursor.isCountingQuery()) {
          cq.select(cb.count(root.get(SurveyResponse_.id)));
          totalCount = em.createQuery(cq).getSingleResult();
        } else {
	        cq.select(root.get(SurveyResponse_.id));
	        Expression<?> sortBy = root.get(SurveyResponse_.id);
	        cq.orderBy(cb.asc(sortBy));
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(cursor.getOffset());
			tq.setMaxResults(cursor.getMaxResults());
			results = tq.getResultList();
        }
        return new PagedResult<>(results, cursor, totalCount);
	}

	/**
	 * Finds optionally a response for a given survey and profile.
	 * @param survey the survey to look for.
	 * @param profile the profile to look for.
	 * @return An optional with the record found, if any.
	 */
	public Optional<SurveyResponse> findResponse(Survey survey, Profile profile) {
        if (survey == null || profile == null) {
        	throw new IllegalArgumentException("Survey and profile are mandatory parameters");
        }
    	List<SurveyResponse> results = em.createQuery("from SurveyResponse where " 
				+ "survey = :survey and profile = :profile", SurveyResponse.class)
			.setParameter("survey", survey)
			.setParameter("profile", profile)
			.getResultList();
    	return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0)); 
	}
}
