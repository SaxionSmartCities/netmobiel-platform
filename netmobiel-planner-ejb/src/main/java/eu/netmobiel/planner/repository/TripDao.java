package eu.netmobiel.planner.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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

import com.vividsolutions.jts.geom.Polygon;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.GeoLocation_;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.commons.util.EllipseHelper;
import eu.netmobiel.planner.annotation.PlannerDatabase;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripState;
import eu.netmobiel.planner.model.Trip_;
import eu.netmobiel.planner.model.User;
import eu.netmobiel.planner.repository.helper.WithinPredicate;

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

    public PagedResult<Long> findByTraveller(User traveller, TripState state, Instant since, Instant until, 
    		Boolean deletedToo, SortDirection sortDirection, Integer maxResults, Integer offset) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Trip> trips = cq.from(Trip.class);
        List<Predicate> predicates = new ArrayList<>();
        Predicate predTraveller = cb.equal(trips.get(Trip_.traveller), traveller);
        predicates.add(predTraveller);
        if (state != null) {
            predicates.add(cb.equal(trips.get(Trip_.state), state));
        }
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
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (maxResults == 0) {
            cq.select(cb.count(trips.get(Trip_.id)));
            totalCount = em.createQuery(cq).getSingleResult();
        } else {
            cq.select(trips.get(Trip_.id));
            if (sortDirection == SortDirection.DESC) {
            	cq.orderBy(cb.desc(trips.get(Trip_.departureTime)));
            } else {
            	cq.orderBy(cb.asc(trips.get(Trip_.departureTime)));
            }
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(offset);
			tq.setMaxResults(maxResults);
			results = tq.getResultList();
        }
        return new PagedResult<Long>(results, maxResults, offset, totalCount);
    }


    /**
     * Lists a page of trips in planning state (of anyone) that have a departure or arrival location within a circle with radius 
     * <code>arrdepRadius</code> meter around the <code>location</code> and where both departure and arrival location are within
     * a circle with radius <code>travelRadius</code> meter. Consider only trips with a departure time beyond now.
     * For a shout-out we have two option: Drive to the nearby departure, then to the drop-off, then back home. The other way around is
     * also feasible. This why the small circle must included either departure or arrival location!
     * @param location the reference location of the driver asking for the trips.
     * @param startTime the time from where to start the search. 
     * @param depArrRadius the small circle containing at least departure or arrival location of the traveller.
     * @param travelRadius the larger circle containing both departure and arrival location of the traveller.
     * @param maxResults For paging: maximum results.
     * @param offset For paging: the offset in the results to return.
     * @return A list of trips matching the parameters.
     */
    public PagedResult<Long> findShoutOutTrips(GeoLocation location, Instant startTime, Integer depArrRadius, Integer travelRadius, Integer maxResults, Integer offset) {
    	Polygon deparrCircle = EllipseHelper.calculateCircle(location.getPoint(), depArrRadius);
    	Polygon travelCircle = EllipseHelper.calculateCircle(location.getPoint(), travelRadius);
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Trip> trips = cq.from(Trip.class);
        List<Predicate> predicates = new ArrayList<>();
        // Only trips in planning state
        predicates.add(cb.equal(trips.get(Trip_.state), TripState.PLANNING));
        // Only consider trips that depart after startTime
        predicates.add(cb.greaterThanOrEqualTo(trips.get(Trip_.departureTime), startTime));
        // Skip deleted trips
        predicates.add(cb.or(cb.isNull(trips.get(Trip_.deleted)), cb.isFalse(trips.get(Trip_.deleted))));
        // Either departure or arrival location must be within the small circle
        predicates.add(cb.or(
        		new WithinPredicate(cb, trips.get(Trip_.from).get(GeoLocation_.point), deparrCircle),
        		new WithinPredicate(cb, trips.get(Trip_.to).get(GeoLocation_.point), deparrCircle)
        ));
        // Both departure and arrival location must be within the large circle.
        predicates.add(cb.and(
        		new WithinPredicate(cb, trips.get(Trip_.from).get(GeoLocation_.point), travelCircle),
        		new WithinPredicate(cb, trips.get(Trip_.to).get(GeoLocation_.point), travelCircle)
        ));
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (maxResults == 0) {
            cq.select(cb.count(trips.get(Trip_.id)));
            totalCount = em.createQuery(cq).getSingleResult();
        } else {
            cq.select(trips.get(Trip_.id));
            // Order by increasing departure time
	        cq.orderBy(cb.asc(trips.get(Trip_.departureTime)));
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(offset);
			tq.setMaxResults(maxResults);
			results = tq.getResultList();
        }
        return new PagedResult<Long>(results, maxResults, offset, totalCount);
    }

    @Override
	public List<Trip> fetch(List<Long> ids, String graphName) {
		// Create an identity map using the generic fetch. Rows are returned, but not necessarily in the same order
		Map<Long, Trip> resultMap = super.fetch(ids, graphName).stream().collect(Collectors.toMap(Trip::getId, Function.identity()));
		// Now return the rows in the same order as the ids.
		return ids.stream().map(id -> resultMap.get(id)).collect(Collectors.toList());
	}

//  Map<Long, T> resultMap = tq.getResultList().stream().collect(Collectors.toMap(Trip::getId, ));

}