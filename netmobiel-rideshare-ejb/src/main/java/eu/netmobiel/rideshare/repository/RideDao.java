package eu.netmobiel.rideshare.repository;

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
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;

import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.commons.util.EllipseHelper;
import eu.netmobiel.rideshare.annotation.RideshareDatabase;
import eu.netmobiel.rideshare.filter.RideFilter;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.Booking_;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideBase_;
import eu.netmobiel.rideshare.model.RideState;
import eu.netmobiel.rideshare.model.RideTemplate;
import eu.netmobiel.rideshare.model.Ride_;
import eu.netmobiel.rideshare.model.RideshareUser;

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

    public PagedResult<Long> findByDriver(RideFilter filter, Cursor cursor) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Ride> root = cq.from(Ride.class);
        List<Predicate> predicates = new ArrayList<>();
        Predicate predDriver = cb.equal(root.get(RideBase_.driver), filter.getDriver());
        predicates.add(predDriver);
        if (filter.getSince() != null) {
	        predicates.add(cb.greaterThanOrEqualTo(root.get(RideBase_.departureTime), filter.getSince()));
        }        
        if (filter.getUntil() != null) {
	        predicates.add(cb.lessThanOrEqualTo(root.get(RideBase_.departureTime), filter.getUntil()));
        }        
        if (filter.getRideState() != null) {
	        predicates.add(cb.equal(root.get(Ride_.state), filter.getRideState()));
        }        
        if (filter.getBookingState() != null) {
        	Join<Ride, Booking> booking = root.join(Ride_.bookings);
	        predicates.add(cb.equal(booking.get(Booking_.state), filter.getBookingState()));
        }        
        if (filter.getSiblingRideId() != null) {
        	// Note: Database null == null is false!
        	Root<Ride> sibling = cq.from(Ride.class);
	        predicates.add(cb.equal(sibling.get(Ride_.id), filter.getSiblingRideId()));
	        predicates.add(cb.equal(root.get(Ride_.rideTemplate), sibling.get(Ride_.rideTemplate)));
        }        
        if (!filter.isDeletedToo()) {
            Predicate predNotDeleted = cb.or(cb.isNull(root.get(Ride_.deleted)), cb.isFalse(root.get(Ride_.deleted)));
	        predicates.add(predNotDeleted);
        }
        if (filter.isSkipCancelled()) {
 	        predicates.add(cb.notEqual(root.get(Ride_.state), RideState.CANCELLED));
        }
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (cursor.isCountingQuery()) {
            cq.select(cb.count(root.get(Ride_.id)));
            totalCount = em.createQuery(cq).getSingleResult();
        } else {
            cq.select(root.get(Ride_.id));
            if (filter.getSortDir() == SortDirection.DESC) {
            	cq.orderBy(cb.desc(root.get(RideBase_.departureTime)));
            } else {
            	cq.orderBy(cb.asc(root.get(RideBase_.departureTime)));
            }
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(cursor.getOffset());
			tq.setMaxResults(cursor.getMaxResults());
			results = tq.getResultList();
        }
        return new PagedResult<>(results, cursor, totalCount);
    }


    /**
     * Searches for matching rides. The following rules apply:<br/>
     * 1. Pickup and drop-off are within eligibility area;
     * 2.1 lenient = false: The ride departs after <code>earliestDeparture</code> and arrives before <code>latestArrival</code>;
     * 2.2 lenient = true: The ride arrives after <code>earliestDeparture</code> and departs before <code>latestArrival</code>;
     * 3. The car has enough seats available [restriction: only 1 booking allowed now]; 
     * 4. The ride has not been deleted;
     * 5. The passenger and driver should travel in more or less the same direction. 
     * 6. The ride has less than <code>maxBookings</code> active bookings.
     * 7. Rides driven by the traveller are skipped. 
     * @param travellerIdentity The managed identity of the traveller asking the question. Rides by this user are skipped.
     * @param fromPlace The location for pickup
     * @param toPlace The location for drop-off
     * @param maxBearingDifference The maximum difference in bearing direction between driver and passenger vectors.
     * @param earliestDeparture The date and time to depart earliest
     * @param latestArrival The date and time to arrive latest 
     * @param nrSeatsRequested the number of seats required
     * @param lenient if true then also retrieve rides that partly overlap the passenger's travel window. Otherwise it must be fully inside the passenger's travel window. 
     * @param maxResults pagination: maximum number of results
     * @param offset pagination: The offset to start (start at 0)
     * @param graphName the graph name of the entity graph to use.
     * @return A list of potential matches.
     */
    public PagedResult<Long> search(RideshareUser traveller, GeoLocation fromPlace, GeoLocation toPlace, int maxBearingDifference, 
    		Instant earliestDeparture, Instant latestArrival, Integer nrSeatsRequested, boolean lenient, Integer maxBookings, Integer maxResults, Integer offset) {
    	int searchBearing = Math.toIntExact(Math.round(EllipseHelper.getBearing(fromPlace.getPoint(), toPlace.getPoint())));
//    	if (logger.isDebugEnabled()) {
//	    	logger.debug(String.format("Search for ride from %s to %s D %s A %s #%d seats %s, bearing %d, max %s ", fromPlace, toPlace, 
//	    			earliestDeparture != null ? DateTimeFormatter.ISO_INSTANT.format(earliestDeparture) : "-",
//	    			latestArrival != null ? DateTimeFormatter.ISO_INSTANT.format(latestArrival) : "-",
//	    			nrSeatsRequested, lenient ? "lenient" : "strict", searchBearing, maxResults != null ? maxResults.toString() : "?"));
//    	}
    	String baseQuery =     			
    			"from Ride r where contains(r.shareEligibility, :fromPoint) = true and " +
    			"contains(r.shareEligibility, :toPoint) = true and " +
    			"abs(r.carthesianBearing - :searchBearing) < :maxBearingDifference and " +
    			"(CAST(:earliestDeparture as java.lang.String) is null or (:lenient = false and r.departureTime >= :earliestDeparture) or (:lenient = true and r.arrivalTime >= :earliestDeparture)) and " +
    			"(CAST(:latestArrival as java.lang.String) is null or (:lenient = false and r.arrivalTime <= :latestArrival) or (:lenient = true and r.departureTime < :latestArrival)) and " +
    			"r.nrSeatsAvailable >= :nrSeatsRequested and " +
    			"(r.deleted is null or r.deleted = false) and " +
    			"(:maxBookings is null or (select cast(count(b) as java.lang.Integer) from r.bookings b where b.state <> eu.netmobiel.rideshare.model.BookingState.CANCELLED) < :maxBookings) and " +
    			"(:traveller is null or r.driver != :traveller) and " +
    			"r.state = :state";
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
			.setParameter("latestArrival", latestArrival)
			.setParameter("nrSeatsRequested", nrSeatsRequested)
			.setParameter("lenient", lenient)
    		.setParameter("maxBookings", maxBookings)
    		.setParameter("traveller", traveller)
    		.setParameter("state", RideState.SCHEDULED);
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
     * Finds all rides that have the same driver as in the template and have an temporal overlap with the given departure 
     * time or are more in the future than the given departure time.
     * This call will also find the rides that are manually created and created with other templates.
     * @param template The reference template 
     * @return A list of rides, possibly empty.
     */
    public List<Ride> findRidesSameDriverBeyond(Instant departureTime) {
    	TypedQuery<Ride> tq = em.createQuery(
    			"from Ride r where r.driver = r.rideTemplate.driver and r.arrivalTime >= :departureTime"
    			, Ride.class)
    			.setParameter("departureTime", departureTime);
    	return tq.getResultList();
    }

    /**
     * Finds the ride of the specific booking.
     * @param bookingId The ID of the booking.
     * @return The ride.
     * @throws NoResultException when the booking does not exist
     */
    public Ride findRideByBookingId(Long bookingId) {
    	Booking b = new Booking();
    	b.setId(bookingId);
    	TypedQuery<Ride> tq = em.createQuery(
    			"from Ride r where :booking member of r.bookings"
    			, Ride.class)
    			.setParameter("booking", b);
    	return tq.getSingleResult();
    }

//    /**
//     * Find rides that should be monitored. 
//     * @param departureBefore the threshold time of the ride to start monitoring.
//     * @return A list of rides to start monitoring
//     */
//    public List<Ride> findMonitorableRides(Instant departureBefore) {
//    	List<Ride> rides = em.createQuery(
//    			"from Ride r " + 
//    			"where state = :state and monitored = false and r.departureTime < :departureTime " +
//    			" and (r.deleted is null or r.deleted = false) " +
//    			"order by r.departureTime asc", Ride.class)
//    			.setParameter("state", RideState.SCHEDULED)
//    			.setParameter("departureTime", departureBefore)
//    			.getResultList();
//    	return rides; 
//    }
//
//    /**
//     * Find all rides that are monitored right now, according to their monitor status.
//     * @return A list of rides with the monitor flag set.
//     */
//    public List<Ride> findMonitoredRides() {
//    	List<Ride> rides = em.createQuery(
//    			"from Ride r where monitored = true order by r.departureTime asc", Ride.class)
//    			.getResultList();
//    	return rides; 
//    }

	public List<Long> findRidesToMonitor(Instant departureBefore) {
		List<Long> rides = em
				.createQuery("select r.id from Ride r "
						+ "where state in :stateSet and r.departureTime < :departureTime "
						+ "order by r.departureTime asc", Long.class)
				.setParameter("departureTime", departureBefore)
				.setParameter("stateSet", EnumSet.of(RideState.SCHEDULED,
						RideState.DEPARTING, RideState.IN_TRANSIT, RideState.ARRIVING, RideState.VALIDATING))
				.getResultList();
		return rides;
	}


    public boolean existsTemporalOverlap(Ride ride) {
    	Long count = em.createQuery(
    			"select count(r) from Ride r where r != :myRide and r.driver = :driver " + 
    			"and not (r.departureTime > :arrivalTime or r.arrivalTime < :departureTime) " +
    			"and (r.deleted is null or r.deleted = false) " +
    			"and state <> :state", Long.class)
    			.setParameter("myRide", ride)
    			.setParameter("driver", ride.getDriver())
    			.setParameter("departureTime", ride.getDepartureTime())
    			.setParameter("arrivalTime", ride.getArrivalTime())
    			.setParameter("state", RideState.CANCELLED)
    			.getSingleResult();
    	return count > 0;
    }
    
    /**
     * Find the first ride without a departure or arrival postal code.
     * @return A ride without a departure or arrival postal code or an empty Optional.
     */
    public Optional<Ride> findFirstRideWithoutPostalCode() {
    	List<Ride> rides = em.createQuery(
    			"from Ride r where r.departurePostalCode is null or r.arrivalPostalCode is null order by r.id asc", Ride.class)
    			.setMaxResults(1)
    			.getResultList();
    	return rides.isEmpty() ? Optional.empty() : Optional.of(rides.get(0)); 
    }

    /**
     * Find the first ride without a departure postal code.
     * @return A ride without a departure postal code or an empty Optional.
     */
    public Optional<Ride> findFirstRideWithoutDeparturePostalCode() {
    	List<Ride> rides = em.createQuery(
    			"from Ride r where r.departurePostalCode is null order by r.id asc", Ride.class)
    			.setMaxResults(1)
    			.getResultList();
    	return rides.isEmpty() ? Optional.empty() : Optional.of(rides.get(0)); 
    }

    /**
     * Find the first ride without an arrival postal code.
     * @return A ride without an  arrival postal code or an empty Optional.
     */
    public Optional<Ride> findFirstRideWithoutArrivalPostalCode() {
    	List<Ride> rides = em.createQuery(
    			"from Ride r where r.arrivalPostalCode is null order by r.id asc", Ride.class)
    			.setMaxResults(1)
    			.getResultList();
    	return rides.isEmpty() ? Optional.empty() : Optional.of(rides.get(0)); 
    }
    
    /**
     * Find the ride with the same departure location (point).
     * @return A list of rides with the same departure location.
     */
    public List<Ride> findWithSameDepartureLocation(GeoLocation location) {
    	List<Ride> rides = em.createQuery(
    			"from Ride r where equals(:myPoint, r.from.point) = true", Ride.class)
    			.setParameter("myPoint", location.getPoint())
   			.getResultList();
    	return rides; 
    }

    /**
     * Find the rides with the same arrival location (point).
     * @return A list of rides with the same arrival location.
     */
    public List<Ride> findWithSameArrivalLocation(GeoLocation location) {
    	List<Ride> rides = em.createQuery(
    			"from Ride r where equals(:myPoint, r.to.point) = true order by r.id asc", Ride.class)
    			.setParameter("myPoint", location.getPoint())
   			.getResultList();
    	return rides; 
    }
    
    public int updateDeparturePostalCode(GeoLocation departureLocation, String postalCode) {
    	return em.createQuery(
    			"update Ride r set r.departurePostalCode = :postalCode where equals(:myPoint, r.from.point) = true")
    			.setParameter("myPoint", departureLocation.getPoint())
    			.setParameter("postalCode", postalCode)
   			.executeUpdate();
    }

    public int updateArrivalPostalCode(GeoLocation arrivalLocation, String postalCode) {
    	return em.createQuery(
    			"update Ride r set r.arrivalPostalCode = :postalCode where equals(:myPoint, r.to.point) = true")
    			.setParameter("myPoint", arrivalLocation.getPoint())
    			.setParameter("postalCode", postalCode)
   			.executeUpdate();
    }
    
    public PagedResult<Long> listRides(Instant since, Instant until, Integer maxResults, Integer offset) {
    	String baseQuery =     			
    			"from Ride r where r.departureTime >= :since and r.departureTime < :until and " +
    			" state = :state";
    	TypedQuery<Long> tq = null;
    	if (maxResults == 0) {
    		// Only request the possible number of results
    		tq = em.createQuery("select count(r.id) " + baseQuery, Long.class);
    	} else {
    		// Get the data IDs
    		tq = em.createQuery("select r.id " + baseQuery + " order by r.departureTime asc, r.id asc", Long.class);
    	}
    	tq.setParameter("since", since)
			.setParameter("until", until)
			.setParameter("state", RideState.COMPLETED);
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

    /**
	 * Count for a specific user the number of occasions (i.e, at each recurrent ride) how often there were at least x recurrent rides 
	 * within y days, given a date range. All parqameteres must be set, i.e. no nulls allowed.
     * @param driver the driver
     * @param firstDate The date to start the evaluation
     * @param lastDate The last date (exclusive) 
     * @param evaluationPeriod the size of the evaluation period in days, e.g. 30.
     * @param minimumRides the minimum number of rides to find in the period.
     * @return if true than at 1 period has at least the minum number of recurrent rides. 
     */
	public boolean matchesRecurrentRideCondition(RideshareUser driver, Instant firstDate, 
			Instant lastDate, int evaluationPeriod, int minimumRides) {
		try {
	        Object count = em.createNativeQuery(
	        		  "SELECT count(placed)\\:\\:integer FROM ( "
					+ "	SELECT r.departure_time, "
					+ "	 (SELECT count(*) FROM ride rs "
					+ "	  WHERE rs.departure_time >= r.departure_time AND "
					+ "	   rs.departure_time < r.departure_time + make_interval(days => :evaluationPeriod) "
					+ "     AND rs.ride_template IS NOT null AND rs.driver = r.driver "
					+ "   HAVING count(*) >= :minimumRides "
					+ "	) AS placed"
					+ "	FROM ride r "
					+ "	WHERE r.ride_template IS NOT null AND r.driver = :driver "
					+ "	 AND r.departure_time >= :firstDate " 
					+ "  AND r.departure_time < :lastDate "
					+ ") rec_rides")
	        		.setParameter("evaluationPeriod", evaluationPeriod)
	        		.setParameter("minimumRides", minimumRides)
	        		.setParameter("driver", driver.getId())
	        		.setParameter("firstDate", firstDate)
	        		.setParameter("lastDate", lastDate)
	        		.getSingleResult();
	        return (Integer)count > 0;
		} catch (Exception ex) {
			logger.error("Error in matchesRecurrentRideCondition", ex);
			throw ex;
		}
    }
		

}