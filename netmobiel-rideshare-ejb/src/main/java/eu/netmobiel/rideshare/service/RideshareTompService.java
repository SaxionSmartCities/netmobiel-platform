package eu.netmobiel.rideshare.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.ExceptionUtil;
import eu.netmobiel.commons.util.GeometryHelper;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.BookingState;
import eu.netmobiel.rideshare.model.Leg;
import eu.netmobiel.rideshare.model.PlanRequest;
import eu.netmobiel.rideshare.model.PlannerReport;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideshareResult;
import eu.netmobiel.rideshare.model.RideshareUser;
import eu.netmobiel.rideshare.model.ToolType;
import eu.netmobiel.rideshare.repository.BookingDao;
import eu.netmobiel.rideshare.repository.OpenTripPlannerDao;
import eu.netmobiel.rideshare.repository.PlanRequestDao;
import eu.netmobiel.rideshare.repository.RideDao;
import eu.netmobiel.rideshare.repository.RideshareUserDao;
/**
 * The service for the offering the TOMP Transport Operator API. 
 * Although it is possible to realise a ride planner inside the MaaS service, we have chosen to put the function inside the the rideshare service itself.
 * That means that selecting the right driver, given a passenger's trip request, is some kind of proprietary technology applied by the rideshare supplier.
 * The following considerations apply:
 * - The rideshare knows the ellipse algoritm to create the long list with eligable drivers.
 * - The rideshare knows the (proprietary) characteristics of the driver (detour distance and time) to create the short list.
 * - No need for the MaaS provider to collect all the rides from the transport operator through a backchannel. At the same time no need 
 *   for the TO to open up his database for MaaS providers.
 * 
 * @author Jaap Reitsma
 *
 */
@Stateless
@Logging
public class RideshareTompService {
	public static final Integer MAX_RESULTS = 10; 

	@Inject
    private Logger log;

    @Resource
	protected SessionContext sessionContext;

    @Inject
    private RideshareUserDao userDao;

    @Inject
    private OpenTripPlannerDao otpDao;

    @Inject
    private RideDao rideDao;
    
    @Inject
    private BookingDao bookingDao;

    @Inject
    private RideItineraryHelper rideItineraryHelper;

//    @Inject
//    private IdentityHelper identityHelper;
//
    @Inject
    private PlanRequestDao planrequestDao;

    @Resource
    private SessionContext context;

    @Inject
    private BookingManager bookingManager;
    
    /**
     * Filter for acceptable itineraries, testing on max detour in meters.
     */
    private class DetourMetersAcceptable implements Predicate<Collection<Leg>> {
    	private Ride originalRide;
    	private PlannerReport report;

    	public DetourMetersAcceptable(Ride aRide, PlannerReport report) {
    		this.originalRide = aRide;
    		this.report = report;
    	}
    	
		@Override
		public boolean test(Collection<Leg> legs) {
	       	boolean accepted = true;
	    	Integer maxDetour = originalRide.getMaxDetourMeters();
			report.setMaxDetourMeters(maxDetour);
			if (maxDetour != null && maxDetour > 0) {
				// Determine new distance
				double distance = legs.stream().mapToDouble(leg -> leg.getDistance()).sum();
				int detour = (int)Math.round(distance - originalRide.getDistance());
				if (detour > originalRide.getMaxDetourMeters()) {
					String msg = String.format("Reject ride %d, detour of %d is exceeded by %d meters (%d%%)", originalRide.getId(), 
							maxDetour, detour - maxDetour, (detour * 100) / maxDetour); 
					if (log.isDebugEnabled()) {
						log.debug(msg);
					}
					report.setRejected(true);
					report.setRejectionReason(msg);
					accepted = false;
				}
			}
			return accepted;
		}
    }

    /**
     * Filter for acceptable itineraries, testing on max detour in seconds.
     */
    private class DetourSecondsAcceptable implements Predicate<Collection<Leg>> {
    	private Ride originalRide;
    	private PlannerReport report;

    	public DetourSecondsAcceptable(Ride aRide, PlannerReport report) {
    		this.originalRide = aRide;
    		this.report = report;
    	}
    	
		@Override
		public boolean test(Collection<Leg> legs) {
	       	boolean accepted = true;
	    	Integer maxDetour = originalRide.getMaxDetourSeconds();
			report.setMaxDetourSeconds(maxDetour);
			if (maxDetour != null && maxDetour > 0) {
				// Determine new distance
				double duration = legs.stream().mapToDouble(leg -> leg.getDuration()).sum();
				int detour = (int)Math.round(duration - originalRide.getDuration());
				if (detour > maxDetour) {
					String msg = String.format("Reject ride %d, detour of %d is exceeded by %d seconds (%d%%)", originalRide.getId(), 
							maxDetour, detour - maxDetour, (detour * 100) / maxDetour);
					if (log.isDebugEnabled()) {
						log.debug(msg);
					}
					report.setRejected(true);
					report.setRejectionReason(msg);
					accepted = false;
				}
			}
			return accepted;
		}
    }
    
    /**
     * Searches for matching rides. The following rules apply:<br/>
     * 1. Pickup and drop-off are within eligibility area of the ride (calculated on creation of the ride);
     * 2.1 lenient = false: The ride departs after <code>earliestDeparture</code> and arrives before <code>latestArrival</code>;
     * 2.2 lenient = true: The ride arrives after <code>earliestDeparture</code> and departs before <code>latestArrival</code>;
     * 3. The car has enough seats available
     * 4. The ride has not been deleted;
     * 5. The passenger and driver should travel in more or less the same direction. 
     * 6. The ride has less than <code>maxBookings</code> active bookings.
     * 7. earliestDeparture is before latestArrival.  
     * 8. Rides driven by the traveller are skipped. 
     * @param traveller The traveller asking the question. Rides by this user are skipped.
     * @param pickup The location for pickup
     * @param dropOff The location for drop-off
     * @param earliestDeparture The date and time to depart earliest
     * @param latestArrival The date and time to arrive latest 
     * @param nrSeatsRequested the number of seats required
     * @param lenient if true then also retrieve rides that partly overlap the passenger's travel window. Otherwise it must be fully inside the passenger's travel window. 
     * @param maxResults pagination: maximum number of results
     * @param offset pagination: The offset to start (start at 0)
     * @param graphName the graph name of the entity graph to use.
     * @return A list of potential matches.
     */

    private PagedResult<Ride> search(PlanRequest pr, boolean lenient) throws BadRequestException {
    	List<Ride> results = Collections.emptyList();
        PagedResult<Long> prs = rideDao.search(pr.getRequestor(), pr.getFrom(), pr.getTo(), 
        		RideManager.MAX_BEARING_DIFFERENCE, pr.getEarliestDepartureTime(), pr.getLatestArrivalTime(), 
        		pr.getNrSeats(), lenient, RideManager.MAX_BOOKINGS, 0, 0);
        Long totalCount = prs.getTotalCount();
    	if (totalCount > 0 && pr.getMaxResults() > 0) {
    		PagedResult<Long> rideIds = rideDao.search(pr.getRequestor(), pr.getFrom(), pr.getTo(), 
    				RideManager.MAX_BEARING_DIFFERENCE, pr.getEarliestDepartureTime(), pr.getLatestArrivalTime(), 
    				pr.getNrSeats(), lenient, RideManager.MAX_BOOKINGS, pr.getMaxResults(), 0);
        	if (! rideIds.getData().isEmpty()) {
        		results = rideDao.loadGraphs(rideIds.getData(), Ride.SEARCH_RIDES_ENTITY_GRAPH, Ride::getId);
        	}
    	}
    	return new PagedResult<>(results, pr.getMaxResults(), 0, totalCount);
    }

    private RideshareResult searchRides(PlanRequest planRequest, boolean lenient) {
    	PlannerReport report = new PlannerReport();
    	report.setFrom(planRequest.getFrom());
    	report.setTo(planRequest.getTo());
    	report.setLenientSearch(lenient);
    	report.setMaxResults(planRequest.getMaxResults());
    	report.setRequestGeometry(GeometryHelper.createLines(
    			planRequest.getFrom().getPoint().getCoordinate(), 
    			planRequest.getTo().getPoint().getCoordinate(), null));
    	report.setToolType(ToolType.NETMOBIEL_RIDESHARE);
    	RideshareResult result = new RideshareResult(report);
    	long start = System.currentTimeMillis();
    	try { 
            PagedResult<Ride> ridePage = search(planRequest, lenient);
			report.setStatusCode(Response.Status.OK.getStatusCode());
	    	report.setNrResults(ridePage.getCount());
	    	// TODO: In case there are many more rides, we need a criterium to sort them on most probable candidate!
	    	// These are potential candidates. Now try to determine the complete route, including the intermediate places for pickup and dropoff
	    	// The passenger is only involved in some (one) of the legs: the pickup or the drop-off. We assume the car is for the first or last mile of the passenger.
	    	// What if the pickup point and the driver's departure point are the same? A test revealed that we get an error TOO_CLOSE. Silly.
	    	// A minimum distance filter is added in the OTP client. We don't care here, but do not make expectations about the number of legs 
	    	// in the itinerary.
    		result.setPage(ridePage);
    	} catch (BadRequestException ex) {
			report.setErrorText(ex.getMessage());
			report.setStatusCode(Response.Status.BAD_REQUEST.getStatusCode());
    	} catch (Exception ex) {
			report.setErrorText(String.join(" - ", ExceptionUtil.unwindExceptionMessage("Error calling Rideshare", ex)));
			report.setErrorVendorCode(ex.getClass().getSimpleName());
			report.setStatusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    	}
    	if (result.getPage() == null) {
    		result.setPage(PagedResult.empty());
    	}
    	report.setExecutionTime(System.currentTimeMillis() - start);
    	return result;
    }

    /**
     * Searches for matching rides. The following rules apply:<br/>
     * 1. Pickup and drop-off are within eligibility area of the ride (calculated on creation of the ride);
     * 2.1 lenient = false: The ride departs after <code>earliestDeparture</code> and arrives before <code>latestArrival</code>;
     * 2.2 lenient = true: The ride arrives after <code>earliestDeparture</code> and departs before <code>latestArrival</code>;
     * 3. The car has enough seats available
     * 4. The ride has not been deleted;
     * 5. The passenger and driver should travel in more or less the same direction. 
     * 6. The ride has less than <code>maxBookings</code> active bookings.
     * 7. earliestDeparture is before latestArrival.  
     * 8. Rides driven by the traveller are skipped.
     * 
     *  Nothing is persisted yet, except for the reports about the planning process.
     *  
     * @param travellerIdentity The managed identity of the traveller asking the question. Rides by this user are skipped.
     * @param pickup The location for pickup of the passenger
     * @param dropOff The location for drop-off of the passenger
     * @param earliestDeparture The date and time to depart earliest
     * @param latestArrival The date and time to arrive latest 
     * @param nrSeatsRequested the number of seats required
     * @param maxResults pagination: maximum number of results
     * @param offset pagination: The offset to start (start at 0)
     * @param graphName the graph name of the entity graph to use.
     * @return A list of potential matches.
     */

    public List<Booking> searchTompRides(String travellerIdentity, @NotNull GeoLocation pickup, @NotNull GeoLocation dropOff, 
    		Instant earliestDeparture, Instant latestArrival, Integer maxWalkDistance, Integer nrSeats, boolean persistBooking, Integer maxResults) throws BadRequestException {
    	long start = System.currentTimeMillis();
    	if (earliestDeparture != null && latestArrival != null && earliestDeparture.isAfter(latestArrival)) {
    		throw new BadRequestException("Departure time must be before arrival time");
    	}
    	if (nrSeats == null) {
    		nrSeats = 1;
    	}
    	if (maxResults == null) {
    		maxResults = MAX_RESULTS;
    	}
    	RideshareUser traveller = travellerIdentity != null ? userDao.findByManagedIdentity(travellerIdentity).orElse(null) : null;
    	PlanRequest rq = new PlanRequest();
    	rq.setEarliestDepartureTime(earliestDeparture);
    	rq.setFrom(pickup);
    	rq.setLatestArrivalTime(latestArrival);
    	rq.setMaxWalkDistance(maxWalkDistance != null ? maxWalkDistance : OpenTripPlannerDao.OTP_MAX_WALK_DISTANCE);
    	rq.setNrSeats(nrSeats);
    	rq.setRequestor(traveller);
    	rq.setRequestTime(Instant.now());
    	rq.setTo(dropOff);
    	rq.setMaxResults(maxResults);
    	RideshareResult ridesResult = searchRides(rq, true);
		List<GeoLocation> intermediatePlaces = List.of(pickup, dropOff);
		rq.addPlannerReport(ridesResult.getReport());
    	List<Booking> bookings = new ArrayList<>();
    	for (Ride orgRide : ridesResult.getPage().getData()) {
    		if (log.isDebugEnabled()) {
    			log.debug("searchTompRides option: " + orgRide.toStringCompact());
    		}
        	// Calculate for each ride found the itinerary when the passenger would ride along, i.e., add the pickup and drop-off location
        	// as intermediate places to the OTP planner and calculate the itinerary.
    		// For each ride, calculate an itinerary for the shared ride
    		RideshareResult rr = otpDao.createSharedRide(Instant.now(), orgRide, intermediatePlaces);
    		rq.addPlannerReport(rr.getReport());
    		if (!rr.hasError()) {
        		Ride ride = rr.getPage().getData().get(0);
        		// This ride is (must) NOT in the persistence context 
        		boolean accepted = Stream.of(
        				new DetourMetersAcceptable(orgRide, rr.getReport()), 
        				new DetourSecondsAcceptable(orgRide, rr.getReport()))
        				.reduce(x -> true, Predicate::and)
        				.test(ride.getLegs());
        		// Note: the report is updated on the fly with the validations
            	if (accepted) {
    	        	// We have the plan for the driver now. Extract the passenger leg (always a single leg now)
            		// We do not store the passenger yet, that is not really necessary.
            		// When really creating the booking, the passenger must be defined. 
            		Booking b = rideItineraryHelper.createBooking(ride, pickup, dropOff);
            		b.setNrSeats(rq.getNrSeats());
            		if (persistBooking) {
            			bookingDao.save(b);
            		}
            		bookings.add(b);
            	}
    		}
    	}
    	rq.setRequestDuration(System.currentTimeMillis() - start);
    	// Final step: Save the report
    	planrequestDao.save(rq);
    	return bookings;
    }
 
    /**
     * Check the health of the transport operator.
     * No exception means healthy.
     */
    public void ping() {
    	// Get a total count to test the database connection.
    	rideDao.findAll(0, 0);
    }
    
    public Booking createBooking(String bookingRef) throws BusinessException {
    	Booking b = bookingManager.getShallowBooking(bookingRef);
    	if (b.getState() != BookingState.NEW) {
    		throw new CreateException(String.format("Resource %s has unexpected state: %s", bookingRef, b.getState()));
    	}
    	b.setState(BookingState.REQUESTED);
    	return b;
    		
    }
}
