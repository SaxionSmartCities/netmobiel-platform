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

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.planner.annotation.PlannerDatabase;
import eu.netmobiel.planner.model.Itinerary_;
import eu.netmobiel.planner.model.PlannerUser;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripState;
import eu.netmobiel.planner.model.Trip_;

@ApplicationScoped
@Typed(TripDao.class)
//@Logging
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

    public PagedResult<Long> findTrips(PlannerUser traveller, TripState state, Instant since, Instant until, 
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

    /**
     * Find all trips that are monitored right now, according to their monitor status.
     * @return A list of trips with the monitor flag set.
     */
    public List<Trip> findMonitoredTrips() {
    	List<Trip> trips = em.createQuery(
    			"from Trip t where monitored = true order by t.itinerary.departureTime asc", Trip.class)
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

    /**
     * Find the first ride without a departure or arrival postal code.
     * @return A ride without a departure or arrival postal code or an empty Optional.
     */
    public Optional<Trip> findFirstTripWithoutPostalCode() {
    	List<Trip> trips = em.createQuery(
    			"from Trip t where t.departurePostalCode is null or t.arrivalPostalCode is null order by t.id asc", Trip.class)
    			.setMaxResults(1)
    			.getResultList();
    	return trips.isEmpty() ? Optional.empty() : Optional.of(trips.get(0)); 
    }

    public int updateDeparturePostalCode(GeoLocation departureLocation, String postalCode) {
    	return em.createQuery(
    			"update Trip t set t.departurePostalCode = :postalCode where equals(:myPoint, t.from.point) = true")
    			.setParameter("myPoint", departureLocation.getPoint())
    			.setParameter("postalCode", postalCode)
   			.executeUpdate();
    }

    public int updateArrivalPostalCode(GeoLocation arrivalLocation, String postalCode) {
    	return em.createQuery(
    			"update Trip t set t.arrivalPostalCode = :postalCode where equals(:myPoint, t.to.point) = true")
    			.setParameter("myPoint", arrivalLocation.getPoint())
    			.setParameter("postalCode", postalCode)
   			.executeUpdate();
    }
    
    public PagedResult<Long> listTrips(Instant since, Instant until, Integer maxResults, Integer offset) {
    	String baseQuery =     			
    			"from Trip t where t.itinerary.departureTime >= :since and t.itinerary.departureTime < :until and " +
    			" t.state = :state";
    	TypedQuery<Long> tq = null;
    	if (maxResults == 0) {
    		// Only request the possible number of results
    		tq = em.createQuery("select count(t.id) " + baseQuery, Long.class);
    	} else {
    		// Get the data IDs
    		tq = em.createQuery("select t.id " + baseQuery + " order by t.itinerary.departureTime asc, t.id asc", Long.class);
    	}
    	tq.setParameter("since", since)
			.setParameter("until", until)
			.setParameter("state", TripState.COMPLETED);
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (maxResults == 0) {
            totalCount = tq.getSingleResult();
        } else {
			tq.setFirstResult(offset);
			tq.setMaxResults(maxResults);
			results = tq.getResultList();
        }
        return new PagedResult<Long>(results, maxResults, offset, totalCount);
    }

}