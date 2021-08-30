package eu.netmobiel.profile.repository;

import java.time.Duration;
import java.time.Instant;
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
import javax.persistence.criteria.Root;

import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.profile.annotation.ProfileDatabase;
import eu.netmobiel.profile.model.Survey;
import eu.netmobiel.profile.model.Survey_;


@ApplicationScoped
@Typed(SurveyDao.class)
public class SurveyDao extends AbstractDao<Survey, String> {

	@Inject @ProfileDatabase
    private EntityManager em;

    public SurveyDao() {
		super(String.class, Survey.class);
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
	public PagedResult<String> listSurveys(Cursor cursor) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        List<String> results = null;
        Long totalCount = null;
        if (cursor.isCountingQuery()) {
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<Survey> root = cq.from(Survey.class);
            cq.select(cb.count(root.get(Survey_.surveyId)));
            totalCount = em.createQuery(cq).getSingleResult();
        } else {
            CriteriaQuery<String> cq = cb.createQuery(String.class);
            Root<Survey> root = cq.from(Survey.class);
	        cq.select(root.get(Survey_.surveyId));
	        Expression<?> sortBy = root.get(Survey_.surveyId);
	        cq.orderBy(cb.asc(sortBy));
	        TypedQuery<String> tq = em.createQuery(cq);
			tq.setFirstResult(cursor.getOffset());
			tq.setMaxResults(cursor.getMaxResults());
			results = tq.getResultList();
        }
        return new PagedResult<>(results, cursor, totalCount);
	}

	public Optional<Survey> findSurveyToTake(Instant now, Instant triggerTime) {
		if (now == null || triggerTime ==  null) {
			throw new IllegalArgumentException("Supply 'now' and 'triggerTime' parameters");
		}
		if (now.isBefore(triggerTime)) {
			return Optional.empty();
		}
        int actualDelay = Math.toIntExact(Duration.between(triggerTime, now).getSeconds() / 3600); 
    	List<Survey> results = em.createQuery("from Survey where " 
				+"(startTime is null or startTime < :now) "
				+ "and (endTime is null or endTime > :now) " 
				+ "and (takeDelayHours is null or takeDelayHours <= :actualDelay) " 
				+ "and (takeIntervalHours is null or (takeIntervalHours + takeDelayHours) >= :actualDelay) " 
				+ "order by startTime asc, endTime asc",
				Survey.class)
			.setParameter("now", now)
			.setParameter("actualDelay", actualDelay)
			.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0)); 
	}
}
