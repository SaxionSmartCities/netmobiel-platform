package eu.netmobiel.planner.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.planner.annotation.PlannerDatabase;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.Trip_;
import eu.netmobiel.planner.model.User;

@ApplicationScoped
@Typed(TripDao.class)
public class TripDao extends AbstractDao<Trip, Long> {
	public static final Integer MAX_RESULTS = 10; 
    @SuppressWarnings("unused")
	@Inject
    private Logger logger;
    
    @Inject @PlannerDatabase
    private EntityManager em;

    public TripDao() {
		super(Trip.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

    public List<Long> findByTraveller(User traveller, Instant since, Instant until, Boolean deletedToo, Integer maxResults, Integer offset) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Trip> trips = cq.from(Trip.class);
        cq.select(trips.get(Trip_.id));
        List<Predicate> predicates = new ArrayList<>();
        Predicate predTraveller = cb.equal(trips.get(Trip_.traveller), traveller);
        predicates.add(predTraveller);
        if (since != null) {
	        Predicate predSince = cb.greaterThanOrEqualTo(trips.get(Trip_.departureTime), since);
	        predicates.add(predSince);
        }        
        if (until != null) {
	        Predicate predUntil = cb.lessThan(trips.get(Trip_.departureTime), until);
	        predicates.add(predUntil);
        }        
        if (deletedToo == null || !deletedToo.booleanValue()) {
            Predicate predNotDeleted = cb.or(cb.isNull(trips.get(Trip_.deleted)), cb.isFalse(trips.get(Trip_.deleted)));
	        predicates.add(predNotDeleted);
        }
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        cq.orderBy(cb.asc(trips.get(Trip_.departureTime)));
        TypedQuery<Long> tq = em.createQuery(cq);
		tq.setFirstResult(offset == null ? 0 : offset);
		tq.setMaxResults(maxResults == null ? MAX_RESULTS : maxResults);
        return tq.getResultList();
    }

	@Override
	public List<Trip> fetch(List<Long> ids, String graphName) {
		// Create an identity map.
		Map<Long, Trip> resultMap = super.fetch(ids, graphName).stream().collect(Collectors.toMap(Trip::getId, Function.identity()));
		// Now return the rows in the same order as the ids.
		return ids.stream().map(id -> resultMap.get(id)).collect(Collectors.toList());
	}

//  Map<Long, T> resultMap = tq.getResultList().stream().collect(Collectors.toMap(Trip::getId, ));

}