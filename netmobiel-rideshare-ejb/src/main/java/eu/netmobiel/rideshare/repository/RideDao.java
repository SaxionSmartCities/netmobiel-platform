package eu.netmobiel.rideshare.repository;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
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

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.commons.util.EllipseHelper;
import eu.netmobiel.rideshare.annotation.RideshareDatabase;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideTemplate;
import eu.netmobiel.rideshare.model.RideTemplate_;
import eu.netmobiel.rideshare.model.Ride_;
import eu.netmobiel.rideshare.model.User;

@ApplicationScoped
@Typed(RideDao.class)
public class RideDao extends AbstractDao<Ride, Long> {
	public static final Integer DEFAULT_PAGE_SIZE = 10; 

	@Inject
    private Logger logger;
    
    @Inject @RideshareDatabase
    private EntityManager em;

    public RideDao() {
		super(Ride.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

    public PagedResult<Long> findByDriver(User driver, Instant since, Instant until, Boolean deletedToo, Integer maxResults, Integer offset) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Ride> rides = cq.from(Ride.class);
        List<Predicate> predicates = new ArrayList<>();
        Predicate predDriver = cb.equal(rides.get(Ride_.rideTemplate).get(RideTemplate_.driver), driver);
        predicates.add(predDriver);
        if (since != null) {
	        Predicate predSince = cb.greaterThanOrEqualTo(rides.get(Ride_.departureTime), since);
	        predicates.add(predSince);
        }        
        if (until != null) {
	        Predicate predUntil = cb.lessThanOrEqualTo(rides.get(Ride_.departureTime), until);
	        predicates.add(predUntil);
        }        
        if (deletedToo == null || !deletedToo) {
            Predicate predNotDeleted = cb.or(cb.isNull(rides.get(Ride_.deleted)), cb.isFalse(rides.get(Ride_.deleted)));
	        predicates.add(predNotDeleted);
        }
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (maxResults == 0) {
            cq.select(cb.count(rides.get(Ride_.id)));
            totalCount = em.createQuery(cq).getSingleResult();
        } else {
            cq.select(rides.get(Ride_.id));
            cq.orderBy(cb.asc(rides.get(Ride_.departureTime)));
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(offset);
			tq.setMaxResults(maxResults);
			results = tq.getResultList();
        }
        return new PagedResult<Long>(results, maxResults, offset, totalCount);
    }


    /**
     * Searches for matching rides. The following rules apply:<br/>
     * 1. Pickup and drop-off are within eligibility area;
     * 2. The ride is after <code>earliestDeparture</code> and before <code>latestDeparture</code>;
     * 3. The car has enough seats available [restriction: only 1 booking allowed now]; 
     * 4. The ride has not been deleted;
     * 5. The passenger and driver should travel in more or less the same direction. 
     * @param fromPlace The location for pickup
     * @param toPlace The location for drop-off
     * @param earliestDeparture The date and time to depart earliest
     * @param latestDeparture The date and time to depart latest 
     * @param nrSeatsRequested the number of seats required
     * @param maxResults pagination: maximum number of results
     * @param offset pagination: The offset to start (start at 0)
     * @param graphName the graph name of the entity graph to use.
     * @return A list of potential matches.
     */
    public PagedResult<Long> search(GeoLocation fromPlace, GeoLocation toPlace, int maxBearingDifference, 
    		Instant earliestDeparture, Instant latestDeparture, Integer nrSeatsRequested, Integer maxResults, Integer offset) {
    	int searchBearing = Math.toIntExact(Math.round(EllipseHelper.getBearing(fromPlace.getPoint(), toPlace.getPoint())));
    	if (logger.isDebugEnabled()) {
	    	logger.debug(String.format("Search for ride from %s to %s D %s A %s #%d seats, bearing %d", fromPlace, toPlace, 
	    			earliestDeparture != null ? DateTimeFormatter.ISO_INSTANT.format(earliestDeparture) : "-",
	    			latestDeparture != null ? DateTimeFormatter.ISO_INSTANT.format(latestDeparture) : "-",
	    			nrSeatsRequested, searchBearing));
    	}
    	String baseQuery =     			
    			"from Ride r where contains(r.shareEligibility, :fromPoint) = true and " +
    			"contains(r.shareEligibility, :toPoint) = true and " +
    			"abs(r.carthesianBearing - :searchBearing) < :maxBearingDifference and " +
    			"(CAST(:fromDate as java.lang.String) is null or r.departureTime >= :earliestDeparture) and " +
    			"(CAST(:toDate as java.lang.String) is null or r.departureTime < :latestDeparture) and " +
    			"r.nrSeatsAvailable >= :nrSeatsRequested and " +
    			"(r.deleted is null or r.deleted = false)";
    	TypedQuery<Long> tq = null;
    	if (maxResults == 0) {
    		// Only request the possible number of results
    		tq = em.createQuery("select count(r.id) " + baseQuery, Long.class);
    	} else {
    		// Get the data IDs
    		tq = em.createQuery("select r.id " + baseQuery + " order by r.departureTime asc ", Long.class);
    	}
    	tq.setParameter("fromPoint", fromPlace.getPoint())
			.setParameter("toPoint", toPlace.getPoint())
			.setParameter("searchBearing", searchBearing)
			.setParameter("maxBearingDifference", maxBearingDifference)
			.setParameter("earliestDeparture", earliestDeparture)
			.setParameter("latestDeparture", latestDeparture)
			.setParameter("nrSeatsRequested", nrSeatsRequested);
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

	@Override
	public List<Ride> fetch(List<Long> ids, String graphName) {
		// Create an identity map using the generic fetch. Rows are returned, but not necessarily in the same order
		Map<Long, Ride> resultMap = super.fetch(ids, graphName).stream().collect(Collectors.toMap(Ride::getId, Function.identity()));
		// Now return the rows in the same order as the ids.
		return ids.stream().map(id -> resultMap.get(id)).collect(Collectors.toList());
	}
    
    public List<Long> findFollowingRideIds(RideTemplate template, Instant departureTime) {
    	TypedQuery<Long> tq = em.createQuery(
    			"select r.id from Ride r where r.rideTemplate = :template and r.departureTime > :departureTime " + 
    					"and (r.deleted is null or r.deleted = false)", Long.class)
    			.setParameter("template", template)
    			.setParameter("departureTime", departureTime);
    	return tq.getResultList();
    }
    
    public List<Long> findPrecedingRideIds(RideTemplate template, Instant departureTime) {
    	TypedQuery<Long> tq = em.createQuery(
    			"select r.id from Ride r where r.template = :template and r.departureTime < :departureTime " + 
    					"and (r.deleted is null or r.deleted = false)", Long.class)
    			.setParameter("template", template)
    			.setParameter("departureTime", departureTime);
    	return tq.getResultList();
    }
    
    /**
     * Finds all rides that have the same driver as in the template and have an overlap with the current template 
     * or are more in the future than the current template.
     * This call will also find the rides that are manually created and created with other templates.
     * @param template The reference template 
     * @return A list of rides, possibly empty.
     */
    public List<Ride> findRidesBeyondTemplate(RideTemplate template) {
    	TypedQuery<Ride> tq = em.createQuery(
    			"from Ride r where r.driver = r.rideTemplate.driver and r.arrivalTime >= :templateDepartureTime"
    			, Ride.class)
    			.setParameter("templateDepartureTime", template.getDepartureTime());
    	return tq.getResultList();
    }
}