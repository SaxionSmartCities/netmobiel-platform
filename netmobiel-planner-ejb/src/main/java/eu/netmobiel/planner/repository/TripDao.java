package eu.netmobiel.planner.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
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
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;

import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.planner.annotation.PlannerDatabase;
import eu.netmobiel.planner.filter.TripFilter;
import eu.netmobiel.planner.model.Itinerary_;
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

    public PagedResult<Long> findTrips(TripFilter filter, Cursor cursor) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Trip> trips = cq.from(Trip.class);
        List<Predicate> predicates = new ArrayList<>();
        if (filter.getTraveller() != null) {
            predicates.add(cb.equal(trips.get(Trip_.traveller), filter.getTraveller()));
        }
        if (filter.getTripState() != null) {
            predicates.add(cb.equal(trips.get(Trip_.state), filter.getTripState()));
        }
        if (filter.getSince() != null) {
	        predicates.add(cb.greaterThanOrEqualTo(trips.get(Trip_.itinerary).get(Itinerary_.arrivalTime), filter.getSince()));
        }        
        if (filter.getUntil() != null) {
	        predicates.add(cb.lessThan(trips.get(Trip_.itinerary).get(Itinerary_.departureTime), filter.getUntil()));
        }        
        if (!Boolean.TRUE.equals(filter.getDeletedToo())) {
            Predicate predNotDeleted = cb.or(cb.isNull(trips.get(Trip_.deleted)), cb.isFalse(trips.get(Trip_.deleted)));
	        predicates.add(predNotDeleted);
        }
        if (Boolean.TRUE.equals(filter.getSkipCancelled())) {
	        predicates.add(cb.notEqual(trips.get(Trip_.state), TripState.CANCELLED));
        }
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (cursor.isCountingQuery()) {
            cq.select(cb.count(trips.get(Trip_.id)));
            totalCount = em.createQuery(cq).getSingleResult();
        } else {
            cq.select(trips.get(Trip_.id));
            Expression<?> orderExpr = trips.get(Trip_.itinerary).get(Itinerary_.departureTime); 
           	cq.orderBy(filter.getSortDir() == SortDirection.DESC ? cb.desc(orderExpr) : cb.asc(orderExpr));
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(cursor.getOffset());
			tq.setMaxResults(cursor.getMaxResults());
			results = tq.getResultList();
        }
        return new PagedResult<>(results, cursor, totalCount);
    }

	public List<Long> findTripsToMonitor(Instant departureBefore) {
		List<Long> trips = em
				.createQuery("select t.id from Trip t "
						+ "where t.state in :stateSet and t.itinerary.departureTime < :departureTime and "
						+ " (t.deleted is null or t.deleted = false)"
						+ "order by t.itinerary.departureTime asc", Long.class)
				.setParameter("departureTime", departureBefore)
				.setParameter("stateSet", EnumSet.of(TripState.SCHEDULED,
						TripState.DEPARTING, TripState.IN_TRANSIT, TripState.ARRIVING, TripState.VALIDATING))
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
        return new PagedResult<>(results, maxResults, offset, totalCount);
    }

}