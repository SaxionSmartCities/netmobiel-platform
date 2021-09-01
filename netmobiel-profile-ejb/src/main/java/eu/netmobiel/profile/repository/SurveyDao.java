package eu.netmobiel.profile.repository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.slf4j.Logger;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.profile.annotation.ProfileDatabase;
import eu.netmobiel.profile.model.Survey;


@ApplicationScoped
@Typed(SurveyDao.class)
public class SurveyDao extends AbstractDao<Survey, String> {

	@Inject @ProfileDatabase
    private EntityManager em;

	@Inject
    private Logger logger;

	public SurveyDao() {
		super(String.class, Survey.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
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
				+ "order by startTime asc, endTime asc, id asc",
				Survey.class)
			.setParameter("now", now)
			.setParameter("actualDelay", actualDelay)
			.getResultList();
    	if (results.size() > 1) {
    		logger.warn("No support for multiple active surveys!");
    	}
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0)); 
	}

	public Optional<Survey> findSurveyByProviderReference(String reference) {
		if (reference == null) {
			throw new IllegalArgumentException("provider survey identifier");
		}
    	List<Survey> results = em.createQuery("from Survey where surveyId = :reference", Survey.class)
			.setParameter("reference", reference)
			.getResultList();
    	if (results.size() > 1) {
    		throw new IllegalStateException("Duplicate provider survey identifier: " + reference);
    	}
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0)); 
	}
}
