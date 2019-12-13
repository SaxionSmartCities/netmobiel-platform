package eu.netmobiel.rideshare.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideTemplate;
import eu.netmobiel.rideshare.model.RideTemplate_;
import eu.netmobiel.rideshare.model.Ride_;
import eu.netmobiel.rideshare.model.User;
import eu.netmobiel.rideshare.util.EllipseHelper;

@ApplicationScoped
@Typed(RideDao.class)
public class RideDao extends AbstractDao<Ride, Long> {
    @Inject
    private Logger logger;
    
    @Inject
    private EntityManager em;

    public RideDao() {
		super(Ride.class);
	}

    public List<Ride> findByDriver(User driver, LocalDate since, LocalDate until, boolean deletedToo, String graphName) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Ride> cq = cb.createQuery(Ride.class);
        Root<Ride> rides = cq.from(Ride.class);
        cq.select(rides);
        List<Predicate> predicates = new ArrayList<>();
        Predicate predDriver = cb.equal(rides.get(Ride_.rideTemplate).get(RideTemplate_.driver), driver);
        predicates.add(predDriver);
        if (since != null) {
	        Predicate predSince = cb.greaterThanOrEqualTo(rides.get(Ride_.departureTime), since.atStartOfDay());
	        predicates.add(predSince);
        }        
        if (until != null) {
	        Predicate predUntil = cb.lessThanOrEqualTo(rides.get(Ride_.departureTime), until.atStartOfDay());
	        predicates.add(predUntil);
        }        
        if (! deletedToo) {
            Predicate predNotDeleted = cb.or(cb.isNull(rides.get(Ride_.deleted)), cb.isFalse(rides.get(Ride_.deleted)));
	        predicates.add(predNotDeleted);
        }
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        cq.orderBy(cb.asc(rides.get(Ride_.departureTime)));
        
        TypedQuery<Ride> tq = em.createQuery(cq);
        if (graphName != null) {
        	tq.setHint(JPA_HINT_LOAD, em.getEntityGraph(graphName));
        }
        return tq.getResultList();
    }

    /**
     * Searches for matching rides. The following rules apply:<br/>
     * 1. Pickup and dropoff are within eligibility area
     * 2. The ride is in the future (near the specified date)
     * 3. The car has enough seats available [restriction: only 1 booking allowed now]. 
     * 4. The ride has not been deleted.
     * 5. The passenger and driver should travel in the same direction: 
     * @param fromPlace The location for pickup
     * @param toPlace The location for drop-off
     * @param fromDate The (local) date and time to depart
     * @param toDate The (local) date and time to arrive
     * @param nrSeats the number of seats required
     * @param maxResults pagination: maximum number of results
     * @param offset pagination: The offset to start (start at 0)
     * @param graphName the graph name of the entity graph to use.
     * @return A list of potential matches.
     */
    public List<Long> search(GeoLocation fromPlace, GeoLocation toPlace, int maxBearingDifference, LocalDateTime fromDate, LocalDateTime toDate, Integer nrSeats, Integer maxResults, Integer offset) {
    	int searchBearing = Math.toIntExact(Math.round(EllipseHelper.getBearing(fromPlace.getPoint(), toPlace.getPoint())));
    	if (logger.isDebugEnabled()) {
	    	logger.debug(String.format("Search for ride from %s to %s D %s A %s #%d seats, bearing %d", fromPlace, toPlace, 
	    			fromDate != null ? DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(fromDate) : "-",
	    			toDate != null ? DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(toDate) : "-",
	    			nrSeats, searchBearing));
    	}
    	TypedQuery<Long> tq = em.createQuery(
    			"select r.id from Ride r where contains(r.rideTemplate.shareEligibility, :fromPoint) = true and " +
    			"contains(r.rideTemplate.shareEligibility, :toPoint) = true and " +
    			"abs(r.rideTemplate.carthesianBearing - :searchBearing) < :maxBearingDifference and " +
    			"(CAST(:fromDate as java.lang.String) is null or r.departureTime >= :fromDate) and " +
    			"(CAST(:toDate as java.lang.String) is null or r.departureTime < :toDate) and " +
    			"r.rideTemplate.nrSeatsAvailable >= :nrSeats and " +
    			"(r.deleted is null or r.deleted = false)", Long.class)
    			.setParameter("fromPoint", fromPlace.getPoint())
    			.setParameter("toPoint", toPlace.getPoint())
    			.setParameter("searchBearing", searchBearing)
    			.setParameter("maxBearingDifference", maxBearingDifference)
    			.setParameter("fromDate", fromDate)
    			.setParameter("toDate", toDate)
    			.setParameter("nrSeats", nrSeats)
    			.setFirstResult(offset)
    			.setMaxResults(maxResults);
        return tq.getResultList();
    }

    public List<Ride> fetch(List<Long> rideIds, String graphName) {
    	TypedQuery<Ride> tq = em.createQuery(
    			"from Ride r where r.id in :rideIdList", Ride.class)
    			.setParameter("rideIdList", rideIds);
    	if (graphName != null) {
    		tq.setHint(JPA_HINT_LOAD, em.getEntityGraph(graphName));
    	}
    	return tq.getResultList();
    }

    public List<Long> findFollowingRideIds(RideTemplate template, LocalDateTime departureTime) {
    	TypedQuery<Long> tq = em.createQuery(
    			"select r.id from Ride r where r.rideTemplate = :template and r.departureTime > :departureTime and (r.deleted is null or r.deleted = false)", Long.class)
    			.setParameter("template", template)
    			.setParameter("departureTime", departureTime);
    	return tq.getResultList();
    }
    
    public List<Long> findPrecedingRideIds(RideTemplate template, LocalDateTime departureTime) {
    	TypedQuery<Long> tq = em.createQuery(
    			"select r.id from Ride r where r.template = :template and r.departureTime < :departureTime and (r.deleted is null or r.deleted = false)", Long.class)
    			.setParameter("template", template)
    			.setParameter("departureTime", departureTime);
    	return tq.getResultList();
    }
    
    /**
     * Returns a list of all recurrent rides with an open horizon. Of each template with
     * an open horizon (i.e, not set), the most future instance is returned.
     * @return A list of recurrent rides with each a different template. 
     */
    public List<Ride> findLastRecurrentRides() {
    	TypedQuery<Ride> tq = em.createQuery(
    			"from Ride r1 where (r1.rideTemplate, r1.departureTime) in " +
    			" (select rideTemplate, max(departureTime) from Ride where rideTemplate.recurrence.interval is not null and rideTemplate.recurrence.horizon is null group by rideTemplate)"
    			, Ride.class);
    	return tq.getResultList();
    }
    
    // Source: https://dev.mysql.com/doc/refman/8.0/en/example-maximum-column-group-row.html
    //    SELECT r1.* from otp_route AS r1 join (
    //    		SELECT max(long_name) as long_name, ov_type
    //    			FROM public.otp_route
    //    			GROUP BY ov_type) AS r2 on r1.long_name = r2.long_name AND r1.ov_type = r2.ov_type}
    // Problem: Can't do that in JPA. But subquery is ok.
}