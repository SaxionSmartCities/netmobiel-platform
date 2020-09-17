package eu.netmobiel.planner.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;

import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.planner.annotation.PlannerDatabase;
import eu.netmobiel.planner.model.Itinerary_;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripState;
import eu.netmobiel.planner.model.Trip_;
import eu.netmobiel.planner.model.User;

@ApplicationScoped
@Typed(TripDao.class)
public class TripDao extends AbstractDao<Trip, Long> {
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

    public PagedResult<Long> findTrips(User traveller, TripState state, Instant since, Instant until, 
    		Boolean deletedToo, SortDirection sortDirection, Integer maxResults, Integer offset) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Trip> trips = cq.from(Trip.class);
        List<Predicate> predicates = new ArrayList<>();
        if (traveller != null) {
            predicates.add(cb.equal(trips.get(Trip_.traveller), traveller));
        }
        if (state != null) {
            predicates.add(cb.equal(trips.get(Trip_.state), state));
        }
        if (since != null) {
	        predicates.add(cb.greaterThanOrEqualTo(trips.get(Trip_.itinerary).get(Itinerary_.departureTime), since));
        }        
        if (until != null) {
	        predicates.add(cb.lessThan(trips.get(Trip_.itinerary).get(Itinerary_.departureTime), until));
        }        
        if (deletedToo == null || !deletedToo.booleanValue()) {
            Predicate predNotDeleted = cb.or(cb.isNull(trips.get(Trip_.deleted)), cb.isFalse(trips.get(Trip_.deleted)));
	        predicates.add(predNotDeleted);
        }
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (maxResults == 0) {
            cq.select(cb.count(trips.get(Trip_.id)));
            totalCount = em.createQuery(cq).getSingleResult();
        } else {
            cq.select(trips.get(Trip_.id));
            if (sortDirection == SortDirection.DESC) {
            	cq.orderBy(cb.desc(trips.get(Trip_.itinerary).get(Itinerary_.departureTime)));
            } else {
            	cq.orderBy(cb.asc(trips.get(Trip_.itinerary).get(Itinerary_.departureTime)));
            }
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(offset);
			tq.setMaxResults(maxResults);
			results = tq.getResultList();
        }
        return new PagedResult<Long>(results, maxResults, offset, totalCount);
    }

    public List<Trip> findMonitorableTrips(Instant departureBefore) {
    	List<Trip> trips = em.createQuery(
    			"from Trip t " + 
    			"where state = :state and monitored = false and t.itinerary.departureTime < :departureTime " +
    			"order by t.itinerary.departureTime asc", Trip.class)
    			.setParameter("state", TripState.SCHEDULED)
    			.setParameter("departureTime", departureBefore)
    			.getResultList();
    	return trips; 
    }

    public Optional<Long> findTripIdByItineraryId(Long itineraryId) {
    	Long tripId = null;
    	try {
			tripId = em.createQuery(
					"select t.id from Trip t where t.itinerary.id = :id", Long.class)
					.setParameter("id", itineraryId)
					.getSingleResult();
		} catch (NoResultException e) {
			// Not found
		}
    	return Optional.ofNullable(tripId); 
    }
    public Optional<Long> findTripIdByLegId(Long legId) {
    	Long tripId = null;
    	try {
    		tripId = em.createQuery(
					"select t.id from Trip t where (select leg from Leg leg where leg.id = :id) member of t.itinerary.legs", Long.class)
					.setParameter("id", legId)
					.getSingleResult();
		} catch (NoResultException e) {
			// Not found
		}
    	return Optional.ofNullable(tripId); 
    }
}