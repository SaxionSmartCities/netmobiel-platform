package eu.netmobiel.planner.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ejb.EJBAccessException;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.UpdateException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.util.EventFireWrapper;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.planner.event.BookingProposalRejectedEvent;
import eu.netmobiel.planner.event.TravelOfferEvent;
import eu.netmobiel.planner.model.Itinerary;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.PlanType;
import eu.netmobiel.planner.model.PlannerResult;
import eu.netmobiel.planner.model.PlannerUser;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.model.TripState;
import eu.netmobiel.planner.repository.ItineraryDao;
import eu.netmobiel.planner.repository.OpenTripPlannerDao;
import eu.netmobiel.planner.repository.TripPlanDao;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.service.RideManager;

/**
 * The manager of the trip plans, including the shout-outs.
 * 
 * 
 *  
 * @author Jaap Reitsma
 *
 */
@Stateless
@Logging
public class TripPlanManager {
	public static final Integer MAX_RESULTS = 10; 
	private static final int DEFAULT_MAX_WALK_DISTANCE = 1000;
	
	public static final String DEFAULT_TIME_ZONE = "Europe/Amsterdam";
	public static final LocalTime DAY_START = LocalTime.parse("08:00");
	public static final LocalTime DAY_END = LocalTime.parse("18:00");
	public static final int DAY_TIME_SLACK = 3;	// hours
	public static final int REST_TIME_SLACK = 1;	// hours
	public static final int STANDARD_TRAVEL_DURATION = 3600;	// seconds

	@Inject
    private Logger log;

    @Inject
    private Planner planner;
    @Inject
    private TripPlanHelper tripPlanHelper;
    @Inject
    private TripPlanDao tripPlanDao;
    @Inject
    private ItineraryDao itineraryDao;
    @Inject
    private OpenTripPlannerDao otpDao;

    @Inject
    private Event<TripPlan> shoutOutRequestedEvent;
    @Inject
    private Event<TravelOfferEvent> travelOfferProposedEvent;
    @Inject
    private Event<BookingProposalRejectedEvent> bookingRejectedEvent;

    private static String formatDateTime(Instant instant) {
    	return DateTimeFormatter.ISO_INSTANT.format(instant);
    }
    
    /**
     * Calculates an itinerary for a rideshare ride from the driver's departure location to his his destination, in between picking up a 
     * passenger and dropping-off the passenger at a different location.
     * Where should this calculation be done? Is it core NetMobiel, or should the transport provider give the estimation? Probably the latter.
     * For shout-out the planning question should be asked at the rideshare service. Their planner will provide an answer.
     * @param now The time perspective of the call (in regular use always the current time)
     * @param fromPlace The deaprture location of the driver
     * @param toPlace the destination of the driver
     * @param travelTime the travel time of the driver, to be interpreted as departure time.
     * @param maxWalkDistance The maximum distance to walk, if necessary.
     * @param via The pickup and drop-off locations of the passenger.
     * @return A planner result object.
     */
    private PlannerResult planRideshareItinerary(Instant now, GeoLocation fromPlace, GeoLocation toPlace, Instant travelTime, boolean useAsArrivalTime, Integer maxWalkDistance, List<GeoLocation> via) {
    	Set<TraverseMode> modes = new HashSet<>(Arrays.asList(new TraverseMode[] { TraverseMode.WALK, TraverseMode.CAR }));
    	// Calculate for each ride found the itinerary when the passenger would ride along, i.e., add the pickup and drop-off location
    	// as intermediate places to the OTP planner and calculate the itinerary.
    	PlannerResult driverSharedRidePlanResult = otpDao.createPlan(now, fromPlace, toPlace, travelTime,  useAsArrivalTime, modes, false, maxWalkDistance, null, via, 1);
    	for (Itinerary it:  driverSharedRidePlanResult.getItineraries()) {
			it.getLegs().stream()
			.filter(leg -> leg.getTraverseMode() == TraverseMode.CAR)
			.forEach(leg -> {
				leg.setAgencyName(RideManager.AGENCY_NAME);
				leg.setAgencyId(RideManager.AGENCY_ID);
			});
//	    	log.debug("planRideshareItinerary: \n" + it.toString());
		}
    	return driverSharedRidePlanResult;
    }

    /**
     * Heuristically determine a earliest departure time. Rules:
     * If travelling during the day (8-18) then I can depart DAY_TIME_SLACK hours earlier, at other time of the day
     * I can depart at most REST_TIME_SLACK hours earlier.
     * @param travelTime the time to depart or arrive
     * @param useAsArrivalTime If true then subtract STANDARD_TRAVEL_DURATION from the travel time to compensate for
     * 		the travelling time.
     * @return The earliest departure time.
     */
    static Instant calculateEarliestDepartureTime(Instant travelTime, boolean useAsArrivalTime) {
    	if (useAsArrivalTime) {
    		travelTime = travelTime.minusSeconds(STANDARD_TRAVEL_DURATION);
    	}
    	LocalTime localTravelTime = LocalTime.from(travelTime.atZone(ZoneId.of(DEFAULT_TIME_ZONE)));
    	Instant earliestTime;
    	if (localTravelTime.isBefore(DAY_START)) {
    		earliestTime = travelTime.minusSeconds(REST_TIME_SLACK * 60 * 60L); 
    	} else if (localTravelTime.isAfter(DAY_END)) {
    		earliestTime = travelTime.minusSeconds(REST_TIME_SLACK * 60 * 60L); 
    	} else {
    		int slack = DAY_TIME_SLACK * 60 * 60;
    		if (localTravelTime.minusSeconds(slack).isAfter(DAY_START.minusSeconds(REST_TIME_SLACK * 60 * 60L))) {
        		earliestTime = travelTime.minusSeconds(slack);
    		} else {
    			LocalDate date = LocalDate.from(travelTime.atZone(ZoneId.of(DEFAULT_TIME_ZONE))); 
        		earliestTime = LocalDateTime.of(date, DAY_START.minusSeconds(REST_TIME_SLACK * 60 * 60L)).atZone(ZoneId.of(DEFAULT_TIME_ZONE)).toInstant();
    		}
    	}
    	return earliestTime;
    }
    
    /**
     * Heuristically determine a latest arrival time. Rules:
     * If travelling during the day (8-18) then I can arrive DAY_TIME_SLACK hours later, at other time of the day
     * I want to arrive at most REST_TIME_SLACK hours later.
     * @param travelTime the time to depart or arrive
     * @param useAsArrivalTime If false then add STANDARD_TRAVEL_DURATION to the travel time to compensate for
     * 		the travelling time.
     * @return The earliest departure time.
     */
    static Instant calculateLatestArrivalTime(Instant travelTime, boolean useAsArrivalTime) {
    	if (!useAsArrivalTime) {
    		travelTime = travelTime.plusSeconds(STANDARD_TRAVEL_DURATION);
    	}
    	LocalTime localTravelTime = LocalTime.from(travelTime.atZone(ZoneId.of(DEFAULT_TIME_ZONE)));
    	Instant latestTime;
    	if (localTravelTime.isAfter(DAY_END)) {
    		latestTime = travelTime.plusSeconds(REST_TIME_SLACK * 60 * 60L); 
    	} else if (localTravelTime.isBefore(DAY_START)) {
    		latestTime = travelTime.plusSeconds(REST_TIME_SLACK * 60 * 60L); 
    	} else {
    		int slack = DAY_TIME_SLACK * 60 * 60;
    		if (localTravelTime.plusSeconds(slack).isBefore(DAY_END.plusSeconds(REST_TIME_SLACK * 60 * 60L))) {
        		latestTime = travelTime.plusSeconds(slack);
    		} else {
    			LocalDate date = LocalDate.from(travelTime.atZone(ZoneId.of(DEFAULT_TIME_ZONE))); 
        		latestTime = LocalDateTime.of(date, DAY_END.plusSeconds(REST_TIME_SLACK * 60 * 60L)).atZone(ZoneId.of(DEFAULT_TIME_ZONE)).toInstant();
    		}
    	}
    	return latestTime;
    }

    static void sanitizePlanInput(TripPlan plan) throws BadRequestException {
    	if (plan.getId() != null) {
    		throw new IllegalStateException("New plan should not have a persistent ID");
    	}
    	Instant now = plan.getRequestTime();
    	if (now == null) {
    		throw new BadRequestException("Parameter 'now' is mandatory");
    	}
    	if (plan.getFrom() == null) {
    		throw new BadRequestException("Parameter 'from' is mandatory");
    	}
    	if (plan.getTo() == null) {
    		throw new BadRequestException("Parameter 'to' is mandatory");
    	}
    	if (plan.getTravelTime() == null) {
    		plan.setTravelTime(now);
    		plan.setUseAsArrivalTime(false);
    	} else if (plan.getTravelTime().isBefore(now)) {
    		throw new BadRequestException(String.format("Travel time %s cannot be before now %s", 
    				formatDateTime(plan.getTravelTime()), formatDateTime(now)));
    		
    	}

    	if (plan.getEarliestDepartureTime() == null) {
    		plan.setEarliestDepartureTime(calculateEarliestDepartureTime(plan.getTravelTime(), plan.isUseAsArrivalTime()));
   			if (plan.getEarliestDepartureTime().isBefore(now)) {
           		plan.setEarliestDepartureTime(now);
   			}
    	} 
    	assert(plan.getEarliestDepartureTime() != null);
    	if (plan.getLatestArrivalTime() == null) {
    		plan.setLatestArrivalTime(calculateLatestArrivalTime(plan.getTravelTime(), plan.isUseAsArrivalTime()));
    	}
    	assert(plan.getLatestArrivalTime() != null);
    	// Verify input
    	if (plan.getEarliestDepartureTime().isBefore(now)) {
    		throw new BadRequestException(String.format("Earliest departure time %s cannot be before %s", 
    				formatDateTime(plan.getEarliestDepartureTime()), formatDateTime(now)));
    	}
    	if (plan.getTravelTime().isBefore(plan.getEarliestDepartureTime())) {
    		throw new BadRequestException(String.format("Earliest departure time %s must be before travel time %s", 
    				formatDateTime(plan.getEarliestDepartureTime()), formatDateTime(plan.getTravelTime())));
    	}
    	if (plan.getTravelTime().isAfter(plan.getLatestArrivalTime())) {
    		throw new BadRequestException(String.format("Latest arrival time %s must be after travel time %s", 
    				formatDateTime(plan.getLatestArrivalTime()), formatDateTime(plan.getTravelTime())));
    	}
    	if (plan.getPlanType() == null) {
    		plan.setPlanType(PlanType.REGULAR);
    	}
    	if (plan.getMaxWalkDistance() == null) {
    		plan.setMaxWalkDistance(DEFAULT_MAX_WALK_DISTANCE);
    	}
    	if (plan.getMaxTransfers() != null && plan.getMaxTransfers() < 0) {
    		throw new IllegalArgumentException("maxTransfers cannot be negative");
    	}
    	if (plan.getTraverseModes() == null || plan.getTraverseModes().isEmpty()) {
    		plan.setTraverseModes(new HashSet<>(Arrays.asList(new TraverseMode[] { TraverseMode.WALK, TraverseMode.RIDESHARE, TraverseMode.TRANSIT })));
    	}
    	if (! plan.getTraverseModes().contains(TraverseMode.RIDESHARE)) {
    		plan.setFirstLegRideshareAllowed(false);
    		plan.setLastLegRideshareAllowed(false);
    	}
    }

    /**
     * Creates a trip plan on behalf of a user. If the type of the plan is a shout-out, then a shout-out event is sent. 
     * Otherwise the planner is called and a list of possible itineraries is prepared. 
     * @param requestor the user creating the plan.
     * @param traveller the user for whom the plan is created
     * @param plan the new plan
     * @param now the timestamp of this very moment, used for testing
     * @return The plan just created.
     * @throws BusinessException 
     */
    public TripPlan createAndReturnTripPlan(PlannerUser requestor, PlannerUser traveller, TripPlan plan, Instant now) throws BusinessException {
    	plan.setRequestor(requestor != null ? requestor : traveller);
    	plan.setTraveller(traveller);
    	plan.setRequestTime(now);
    	sanitizePlanInput(plan);
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("createAndReturnTripPlan:\n Now %s %s %s %s from %s to %s; seats #%d, max walk distance %sm; modalities %s; maxTransfers %s; first/lastLegRS %s/%s",
    						formatDateTime(plan.getRequestTime()),
    						plan.getPlanType().toString(),
    						plan.isUseAsArrivalTime() ? "A" : "D",
    						plan.getTravelTime(), 
    						plan.getFrom().toString(), 
    						plan.getTo().toString(),
    						plan.getNrSeats(), 
   							plan.getMaxWalkDistance(),
    						plan.getTraverseModes().stream().map(tm -> tm.name()).collect(Collectors.joining(", ")),
    						plan.getMaxTransfers() != null ? plan.getMaxTransfers().toString() : "-",
    						plan.isFirstLegRideshareAllowed() ? "Y" : "N",
    	    				plan.isLastLegRideshareAllowed() ? "Y" : "N"
   						)
    		);
    	}
		// Add a geodesic estimation (as the crow flies)
		plan.setGeodesicDistance(Math.toIntExact(Math.round(plan.getFrom().getDistanceTo(plan.getTo()) * 1000)));
       	if (plan.getPlanType() != PlanType.SHOUT_OUT) {
       		// Start a search
       		plan = planner.searchMultiModal(plan);
        	plan.close();
       	} else {
       		// Determine a reference itinerary for a shoutout
       		PlannerResult planResult = planner.planItineraryByCar(plan.getRequestTime(), plan.getFrom(), plan.getTo(), plan.getTravelTime(), plan.isUseAsArrivalTime(), plan.getMaxWalkDistance());
       		plan.addPlannerReport(planResult.getReport());
       		if (! planResult.hasError()) {
       			// Ok, car is the reference modality
       			plan.setReferenceItinerary(planResult.getItineraries().get(0));
       		}
       	}
       	tripPlanDao.save(plan);
       	if (plan.getPlanType() == PlanType.SHOUT_OUT) {
       		EventFireWrapper.fire(shoutOutRequestedEvent, plan);
       		// Note:the plan remains open, itineraries will hopefully arrive, one by one.
       	}
    	return plan;
    }
    
    /**
     * Creates a trip plan on behalf of a user. If the type of the plan is a shout-out, then a shout-out event is sent. 
     * Otherwise the planner is called and a list of possible itineraries is prepared. 
     * @param requestor the user creating the plan.
     * @param traveller the user for whom the plan is created
     * @param plan the new plan
     * @param now the timestamp of this very moment, used for testing
     * @return The ID of the plan just created.
     * @throws BusinessException 
     */
    public Long createTripPlan(PlannerUser requestor, PlannerUser traveller, TripPlan plan, Instant now) throws BusinessException {
    	return createAndReturnTripPlan(requestor, traveller, plan, now).getId();
    }

    /**
     * Retrieves a trip plan. All available details are retrieved.
     * @param id the id of the trip plan
     * @return The TripPlan object
     * @throws NotFoundException In case of an invalid trip plan ID.
     */
    public TripPlan getTripPlan(Long id) throws NotFoundException {
    	TripPlan plandb = tripPlanDao.loadGraph(id, TripPlan.DETAILED_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such trip plan: " + id));
    	return plandb;
    }
    
    /**
     * List all trip plans owned by the specified user. 
     * @return A list of trips matching the criteria.
     */
    public PagedResult<TripPlan> listTripPlans(PlannerUser traveller, PlanType planType, Instant since, Instant until, Boolean inProgressOnly, 
    		SortDirection sortDirection, Integer maxResults, Integer offset) throws BadRequestException {
    	if (until != null && since != null && !until.isAfter(since)) {
    		throw new BadRequestException("Constraint violation: 'until' must be later than 'since'.");
    	}
    	if (maxResults != null && maxResults > 100) {
    		throw new BadRequestException("Constraint violation: 'maxResults' <= 100.");
    	}
    	if (maxResults != null && maxResults < 0) {
    		throw new BadRequestException("Constraint violation: 'maxResults' >= 0.");
    	}
    	if (offset != null && offset < 0) {
    		throw new BadRequestException("Constraint violation: 'offset' >= 0.");
    	}
        if (maxResults == null) {
        	maxResults = MAX_RESULTS;
        }
        if (offset == null) {
        	offset = 0;
        }
        List<TripPlan> results = Collections.emptyList();
        Long totalCount = 0L;
		PagedResult<Long> prs = tripPlanDao.findTripPlans(traveller, planType, since, until, inProgressOnly, sortDirection, 0, 0);
		totalCount = prs.getTotalCount();
    	if (totalCount > 0 && maxResults > 0) {
    		// Get the actual data
    		PagedResult<Long> tripIds = tripPlanDao.findTripPlans(traveller, planType, since, until, inProgressOnly, sortDirection, maxResults, offset);
    		if (tripIds.getData().size() > 0) {
    			results = tripPlanDao.loadGraphs(tripIds.getData(), TripPlan.DETAILED_ENTITY_GRAPH, TripPlan::getId);
    		}
    	}
    	return new PagedResult<>(results, maxResults, offset, totalCount);
    }

    /**
     * Lists a page of trip plans in progress of the shout-out type that have a departure or arrival location within a circle with radius 
     * <code>arrdepRadius</code> meter around the <code>location</code> and where both departure and arrival location are within
     * a circle with radius <code>travelRadius</code> meter. Consider only plans with a travel time beyond now.
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
    public PagedResult<TripPlan> listShoutOuts(GeoLocation location, Instant startTime, Integer depArrRadius, 
    		Integer travelRadius, Integer maxResults, Integer offset) {
        if (maxResults == null) {
        	maxResults = MAX_RESULTS;
        }
        if (offset == null) {
        	offset = 0;
        }
        List<TripPlan> results = Collections.emptyList();
        Long totalCount = 0L;
   		PagedResult<Long> prs = tripPlanDao.findShoutOutPlans(location, startTime, depArrRadius, travelRadius, 0, 0);
		totalCount = prs.getTotalCount();
    	if (totalCount > 0 && maxResults > 0) {
    		// Get the actual data
    		PagedResult<Long> tripIds = tripPlanDao.findShoutOutPlans(location, startTime, depArrRadius, travelRadius, maxResults, offset);
    		if (tripIds.getData().size() > 0) {
    			// Return the plan and the traveller 
    			results = tripPlanDao.loadGraphs(tripIds.getData(), TripPlan.SHOUT_OUT_ENTITY_GRAPH, TripPlan::getId);
    		}
    	}
    	return new PagedResult<>(results, maxResults, offset, totalCount);
    }

    /**
     * Retrieves a shout-out trip plan. Only the plan itself is retrieved. For all details see getTripPlan(). 
     * @param id the id of the shout-out trip plan
     * @return The TripPlan object
     * @throws NotFoundException In case of an invalid trip plan ID or when the actual type is not shout-out.
     */
    public TripPlan getShoutOutPlan(Long id) throws NotFoundException {
    	TripPlan plandb = tripPlanDao.loadGraph(id, TripPlan.SHOUT_OUT_ENTITY_GRAPH)
    			.orElse(null);
    	if (plandb == null || plandb.getPlanType() != PlanType.SHOUT_OUT) {
    		throw new NotFoundException("No such shout-out: " + id);
    	}
    	return plandb;
    }
    

    /**
     * Resolves a shout-out (finds a possible itinerary) by issuing a trip plan as a potential solution for the 
     * shout-out by a ride by the driver. This trip plan is requested by the driver using his own departure and 
     * arrival location (if provided), and a proposed travel time. 
     * The intermediate stops for the passenger are taken from the shout out plan (issued by 
     * the traveller/passenger). The result is an integrated trip plan for the driver driving along the passenger's stops.
     * The driver can review the plan and decide to make it an offer to the passenger.  
     * @param now The reference point in time. Especially used for testing.
     * @param driver The driver asking to verify a shout-out. 
     * @param shoutOutPlanRef A reference to the shout-out of a traveller.
     * @param driverPlan The input parameters of the driver
     * @return A trip plan calculated  to fill-in the shout-out.
     * @throws NotFoundException In case the shout-out could not be found.
     */
    public TripPlan planShoutOutSolution(Instant now, PlannerUser driver, String shoutOutPlanRef, TripPlan driverPlan, TraverseMode traverseMode) throws NotFoundException, BusinessException {
    	Long pid = UrnHelper.getId(TripPlan.URN_PREFIX, shoutOutPlanRef);
    	TripPlan travPlan = tripPlanDao.find(pid).orElseThrow(() -> new NotFoundException("No such TripPlan: " + shoutOutPlanRef));
    	if (!travPlan.isInProgress()) {
    		throw new CreateException("Shout-out has already been closed");
    	}
    	if (traverseMode != TraverseMode.RIDESHARE) {
    		throw new BadRequestException("Only RIDESHARE modality is supported");
    	}
    	boolean adjustDepartureTime = false;
    	driverPlan.setRequestor(driver);
    	driverPlan.setTraveller(driver);
    	driverPlan.setRequestTime(now);
    	if (driverPlan.getFrom() == null) {
    		driverPlan.setFrom(travPlan.getFrom());
    	}
    	if (driverPlan.getTo() == null) {
    		driverPlan.setTo(travPlan.getTo());
    	}
    	if (driverPlan.getTravelTime() == null) {
    		driverPlan.setTravelTime(travPlan.getTravelTime());
    		driverPlan.setUseAsArrivalTime(travPlan.isUseAsArrivalTime());
    		adjustDepartureTime = true;
    	}
    	if (driverPlan.getTravelTime().isBefore(now)) {
    		throw new BadRequestException(String.format("Travel time %s cannot be before now %s", 
    				formatDateTime(driverPlan.getTravelTime()), formatDateTime(now)));
    		
    	}

    	driverPlan.setNrSeats(travPlan.getNrSeats());
    	driverPlan.setMaxWalkDistance(travPlan.getMaxWalkDistance());
    	driverPlan.setTraverseModes(new HashSet<>(Arrays.asList(new TraverseMode[] { TraverseMode.CAR, TraverseMode.RIDESHARE })));
    	List<GeoLocation> intermediatePlaces = new ArrayList<>();
    	intermediatePlaces.add(travPlan.getFrom());
    	intermediatePlaces.add(travPlan.getTo());
    	PlannerResult result = planRideshareItinerary(now, driverPlan.getFrom(), driverPlan.getTo(), 
    			driverPlan.getTravelTime(),  driverPlan.isUseAsArrivalTime(), driverPlan.getMaxWalkDistance(), intermediatePlaces);
    	if (result.getItineraries().size() > 0) {
    		// Shift all the timestamps in the plan in such a way that the pickup or drop-off time matches the travel time of the proposed passenger
    		Itinerary it = result.getItineraries().get(0);
    		if (adjustDepartureTime) {
//	    		log.debug("Before: " + it.toString());
				GeoLocation refLoc = travPlan.isUseAsArrivalTime() ? travPlan.getTo() : travPlan.getFrom();
				it.shiftItineraryTiming(refLoc, travPlan.getTravelTime(), travPlan.isUseAsArrivalTime());
				// Fix the travel time of the driver and set it to the departure time of the first leg
				driverPlan.setTravelTime(it.getDepartureTime());
				driverPlan.setUseAsArrivalTime(false);
				result.getReport().setTravelTime(driverPlan.getTravelTime());
				result.getReport().setUseAsArrivalTime(driverPlan.isUseAsArrivalTime());
//	    		log.debug("After: " + it.toString());
    		}
    		// Find the passenger legs (should be single one) and mark them as rideshare and add a fare.
    		List<Leg> passengerLegs = it.findConnectingLegs(travPlan.getFrom(), travPlan.getTo());
    		passengerLegs.forEach(leg -> tripPlanHelper.assignFareToRideshareLeg(leg));
        	// Normalize the shoutout reference
        	String shoutOutPlanRefNormalized = UrnHelper.createUrn(TripPlan.URN_PREFIX, pid);
        	// Mark the leg as a solution for the shout-out
        	passengerLegs.forEach(leg -> leg.setShoutOutRef(shoutOutPlanRefNormalized));
    	}
    	driverPlan.setPlanType(PlanType.SHOUT_OUT_SOLUTION);
    	driverPlan.addPlannerResult(result);
    	driverPlan.close();
       	tripPlanDao.save(driverPlan);
       	return driverPlan;
    }
    
    /**
     * Adds a solution to a shout-out plan. The solution is provided by a party other than the traveller, such as someone offering 
     * a ride with his car. More complex scenarios are possible with combinations with public transport.
     * @param shoutOutPlanId The original shout-out plan of the traveller, containing details about the desired travel.  
     * @param proposedPlanId The plan proposed by the transport provider. This plan is from the perspective of the transport provider
     * 						and includes his private departure and destination locations. The assumption for now is that the pickup and
     * 						drop-off locations of the traveller are part of the providers itinerary. If not, the (short) walks have to 
     * 						be inserted for the traveller, derived from the shout-out plan parameters.  
     * @param driverRef		The reference to the driver , if relevant.
     * @param vehicleRef	The reference to the vehicle to be used, if relevant.
     * @throws BusinessException
     */
    public void addShoutOutSolution(Long shoutOutPlanId, Long proposedPlanId, String driverRef, String vehicleRef) throws BusinessException {
    	// Only header and validate shout out type
    	TripPlan shoutOutPlan = getShoutOutPlan(shoutOutPlanId);
    	TripPlan proposedPlan = getTripPlan(proposedPlanId);
    	if (proposedPlan.getPlanType() != PlanType.SHOUT_OUT_SOLUTION) {
    		throw new NotFoundException("No such shout-out proposal plan: " + proposedPlanId);
    	}
    	// Prevent accidental changes in the plan lead to persist
    	tripPlanDao.detach(proposedPlan);

    	// Create a copy of the passenger part of the provider's itinerary. Assume for now that the passenger has no private legs. 
    	// All legs in the passengers's itinerary are shared with the transport provider
    	// Should the passenger's itinerary be created here? If there is a slight mismatch a few walks should be introduced.
    	// FIXME To be implemented: Search for the closest pickup and drop locations. Compare with shout-out. If more than 20 m away add a 
    	// WALK leg to the passenger's itinerary.
    	// For now: Complete overlap with provider's itinerary
    	Itinerary passengerIt = proposedPlan.getItineraries().iterator().next().createSingleLeggedItinerary(shoutOutPlan.getFrom(), shoutOutPlan.getTo());
		if (passengerIt.getLegs().size() != 1) {
			throw new IllegalStateException("Expected to find a single leg, instead of " + passengerIt.getLegs().size());
		}
    	passengerIt.setTripPlan(shoutOutPlan);
    	BasicItineraryRankingAlgorithm ranker = new BasicItineraryRankingAlgorithm();
   		ranker.calculateScore(passengerIt, shoutOutPlan.getTravelTime(), shoutOutPlan.isUseAsArrivalTime());
   		itineraryDao.save(passengerIt);
    	
    	// At this point the proposal is added to the passenger's shout-out, but the actual ride and booking hasn't been added yet
    	// So we want the transport provider to notify that this plan is going to be a ride with a booking proposal 
    	TravelOfferEvent toe = new TravelOfferEvent(shoutOutPlan, passengerIt, proposedPlan, driverRef, vehicleRef);
    	EventFireWrapper.fire(travelOfferProposedEvent, toe);
    }
    
    /**
     * Assign a rideshare ride and booking proposal reference to the leg with the specified transport provider tripId.
     * This model is not quite satisfactory. For now get it working quicky. The ride argument should not be here.  
     * @param agencyId The ID of the agency offering the proposal. Now always Netmobiel Rideshare.  
     * @param passengerItinerary The proposed itinerary of the passenger.
     * @param ride	the rideshare ride 
     * @param bookingRef The booking reference at the transport provider.
     * @throws UpdateException 
     */
    public void assignBookingProposalReference(String agencyId, Itinerary passengerItinerary, Ride ride, String bookingRef) throws UpdateException {
    	//FIXME Bad design, bad modelling of interaction with transport provider
    	if (!itineraryDao.isLoaded(passengerItinerary)) {
    		itineraryDao.refresh(passengerItinerary);
    	}
    	List<Leg> legs = passengerItinerary.getLegs().stream()
    			.filter(leg -> Objects.equals(leg.getAgencyId(), agencyId))
    			.collect(Collectors.toList());
    	legs.forEach(leg -> {
    		tripPlanHelper.assignRideToPassengerLeg(leg, ride);
        	leg.setBookingId(bookingRef);
    	});
    	// The legs are stil in planning state!
    	passengerItinerary.getLegs().forEach(leg -> leg.setState(TripState.PLANNING));
    }

    /**
     * Cancel all booked legs of the other itineraries of the shout-out plan. Booked legs must not be cancelled.  
     * @param plan the shout-out trip plan with all proposed itineraries. 
     * @param itineraryToKeep the itinerary selected for the trip to create (i.e., do not cancel this one) 
     * @throws BusinessException on trouble. 
     */
    private void cancelBookedLegs(TripPlan plan, Optional<Itinerary> itineraryToKeep, String cancelReason) throws BusinessException {
    	List<Leg> bookedLegs = plan.getItineraries().stream()
        		.filter(it -> !(itineraryToKeep.isPresent() && it.equals(itineraryToKeep.get())))
        		.flatMap(it -> it.getLegs().stream())
    			.filter(leg -> leg.getState() != TripState.CANCELLED && leg.getBookingId() != null)
    			.collect(Collectors.toList());
    	for (Leg leg : bookedLegs) {
    		EventFireWrapper.fire(bookingRejectedEvent, new BookingProposalRejectedEvent(plan, leg, cancelReason));
        	leg.setState(TripState.CANCELLED);
		}
    }

    /**
     * Handles the resolvement of a shout-out at the side of the trip manager: Close other options, close the plan.
     * @param shoutOutPlan the shout-out
     * @param selectedItinerary the itinerary not to cancel. Already in persistence context.
     * @throws BusinessException in case of trouble
     */
    public void resolveShoutOut(TripPlan aShoutOutPlan, Itinerary selectedItinerary) throws BusinessException {
    	TripPlan shoutOutPlan = getTripPlan(aShoutOutPlan.getId());
    	if (shoutOutPlan.getPlanType() != PlanType.SHOUT_OUT) {
    		throw new IllegalStateException("Expect to handle a shout-out plan");
    	}
    	cancelBookedLegs(shoutOutPlan, Optional.of(selectedItinerary), "Andere oplossing gekozen");
    	shoutOutPlan.close();
    }

    /**
     * Cancels the proposed booking leg in an itinerary and updates the state. This method is called in response to a cancellation from the transport provider.
     * This call is intended to update the trip plan state only.
     * @param tripplanRef
     * @param bookingRef
     * @throws NotFoundException
     * @throws BadRequestException 
     */
    public void cancelBooking(String tripPlanRef, String bookingRef) throws NotFoundException, BadRequestException {
    	TripPlan plan = getTripPlan(UrnHelper.getId(TripPlan.URN_PREFIX, tripPlanRef));
    	plan.getItineraries().stream()
    		.flatMap(it -> it.getLegs().stream())
    		.filter(leg -> bookingRef.equals(leg.getBookingId()))
    		.forEach(leg -> leg.setState(TripState.CANCELLED));
    }

    /**
     * Cancels a shout-out, i.e., the plan is no longer receiving itineraries. 
     * @param id the id of the trip plan
     * @param reason the optional reason to cancel the shout-out.
     * @throws BusinessException 
     */
    public void cancelShoutOut(Long id, String reason) throws BusinessException {
    	TripPlan plan = tripPlanDao.find(id)
    			.orElseThrow(() -> new NotFoundException("No such trip plan: " + id));
    	if (plan.getPlanType() != PlanType.SHOUT_OUT) {
    		throw new BadRequestException("Plan is not a shout-out: " + id);
    	}
    	if (plan.isOpen()) {
        	cancelBookedLegs(plan, Optional.empty(), 
        			reason != null && !reason.isBlank() ? reason : "Oproep is geannuleerd");
    		plan.close();
    	}
    }

    // A bit test code
    
    public void throwRuntimeException() {
    	throw new RuntimeException("A bug in a EJB!");
    }
    public void throwAccessException() {
    	throw new EJBAccessException("Can't access this!");
    }

}