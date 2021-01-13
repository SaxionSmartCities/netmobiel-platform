package eu.netmobiel.planner.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ejb.EJBAccessException;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.UpdateException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.util.EllipseHelper;
import eu.netmobiel.commons.util.EllipseHelper.EligibleArea;
import eu.netmobiel.commons.util.EventFireWrapper;
import eu.netmobiel.commons.util.ExceptionUtil;
import eu.netmobiel.commons.util.GeometryHelper;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.planner.event.BookingProposalRejectedEvent;
import eu.netmobiel.planner.event.ShoutOutResolvedEvent;
import eu.netmobiel.planner.event.TravelOfferEvent;
import eu.netmobiel.planner.model.Itinerary;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.OtpCluster;
import eu.netmobiel.planner.model.PlanType;
import eu.netmobiel.planner.model.PlannerReport;
import eu.netmobiel.planner.model.PlannerResult;
import eu.netmobiel.planner.model.PlannerUser;
import eu.netmobiel.planner.model.RideshareResult;
import eu.netmobiel.planner.model.Stop;
import eu.netmobiel.planner.model.ToolType;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.model.TripState;
import eu.netmobiel.planner.repository.ItineraryDao;
import eu.netmobiel.planner.repository.OpenTripPlannerDao;
import eu.netmobiel.planner.repository.OtpClusterDao;
import eu.netmobiel.planner.repository.TripPlanDao;
import eu.netmobiel.planner.util.PlannerUrnHelper;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.service.RideManager;

@Stateless
@Logging
public class TripPlanManager {
	public static final Integer MAX_RESULTS = 10; 
	private static final float PASSENGER_RELATIVE_MAX_DETOUR = 1.0f;
	private static final Integer CAR_TO_TRANSIT_SLACK = 10 * 60; // [seconds]
	private static final int MAX_RIDESHARES = 5;	
	private static final boolean RIDESHARE_LENIENT_SEARCH = true;	
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
    private TripPlanDao tripPlanDao;
    @Inject
    private ItineraryDao itineraryDao;
    @Inject
    private RideManager rideManager;
    @Inject
    private OpenTripPlannerDao otpDao;
    @Inject
    private OtpClusterDao otpClusterDao;

    
    @Inject
    private Event<TripPlan> shoutOutRequestedEvent;
    @Inject
    private Event<TravelOfferEvent> travelOfferProposedEvent;
    @Inject
    private Event<BookingProposalRejectedEvent> bookingRejectedEvent;
    @Inject
    private Event<Leg> quoteRequestedEvent;

    

    protected List<Stop> filterImportantStops(TripPlan plan) {
    	List<Stop> places = new ArrayList<>(); 
    	for (Itinerary it: plan.getItineraries()) {
    		for (Leg leg: it.getLegs()) {
    			if (leg.getTraverseMode() == TraverseMode.WALK) {
    				continue;
    			}
    			places.add(leg.getFrom());
    			places.add(leg.getTo());
    			places.addAll(leg.getIntermediateStops());
    		}
    	}
    	return null;
    }
    
    private String formatDateTime(Instant instant) {
    	return DateTimeFormatter.ISO_INSTANT.format(instant);
    }
    
    /**
     * Filter for acceptable itineraries, testing on max detour in meters.
     */
    private class DetourMetersAcceptable implements Predicate<Itinerary> {
    	private Ride ride;
    	private PlannerReport report;

    	public DetourMetersAcceptable(Ride aRide, PlannerReport report) {
    		this.ride = aRide;
    		this.report = report;
    	}
    	
		@Override
		public boolean test(Itinerary it) {
	       	boolean accepted = true;
	    	Integer maxDetour = ride.getMaxDetourMeters();
			report.setMaxDetourMeters(maxDetour);
			if (maxDetour != null && maxDetour > 0) {
				// Determine new distance
				double distance = it.getLegs().stream().mapToDouble(leg -> leg.getDistance()).sum();
				int detour = (int)Math.round(distance - ride.getDistance());
				if (detour > ride.getMaxDetourMeters()) {
					String msg = String.format("Reject ride %d, detour of %d is exceeded by %d meters (%d%%)", ride.getId(), 
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
    private class DetourSecondsAcceptable implements Predicate<Itinerary> {
    	private Ride ride;
    	private PlannerReport report;

    	public DetourSecondsAcceptable(Ride aRide, PlannerReport report) {
    		this.ride = aRide;
    		this.report = report;
    	}
    	
		@Override
		public boolean test(Itinerary it) {
	       	boolean accepted = true;
	    	Integer maxDetour = ride.getMaxDetourSeconds();
			report.setMaxDetourSeconds(maxDetour);
			if (maxDetour != null && maxDetour > 0) {
				// Determine new distance
				double duration = it.getLegs().stream().mapToDouble(leg -> leg.getDuration()).sum();
				int detour = (int)Math.round(duration - ride.getDuration());
				if (detour > maxDetour) {
					String msg = String.format("Reject ride %d, detour of %d is exceeded by %d seconds (%d%%)", ride.getId(), 
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
    
    protected RideshareResult searchRides(Instant now, GeoLocation fromPlace, GeoLocation toPlace, Instant travelTime, boolean useAsArrivalTime,
    		Instant earliestDeparture, Instant latestArrival, Integer nrSeats, boolean lenient, Integer maxResults, Integer offset) {
    	PlannerReport report = new PlannerReport();
    	report.setRequestTime(now);
    	report.setTravelTime(travelTime);
    	report.setUseAsArrivalTime(useAsArrivalTime);
    	report.setEarliestDepartureTime(latestArrival);
    	report.setLatestArrivalTime(earliestDeparture);
    	report.setFrom(fromPlace);
    	report.setTo(toPlace);
    	report.setToolType(ToolType.NETMOBIEL_RIDESHARE);
    	report.setMaxResults(maxResults);
    	report.setStartPosition(offset);
    	report.setNrSeats(nrSeats);
    	report.setLenientSearch(lenient);
    	report.setRequestGeometry(GeometryHelper.createLines(fromPlace.getPoint().getCoordinate(), toPlace.getPoint().getCoordinate(), null));
    	RideshareResult result = new RideshareResult(report);
    	PagedResult<Ride> ridePage = null;;
    	long start = System.currentTimeMillis();
    	try { 
    		ridePage = rideManager.search(fromPlace, toPlace,  earliestDeparture, latestArrival, nrSeats, lenient, maxResults, offset);
			report.setStatusCode(Response.Status.OK.getStatusCode());
	    	report.setNrItineraries(ridePage.getCount());
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
    	report.setNrItineraries(result.getPage().getCount());
    	report.setExecutionTime(System.currentTimeMillis() - start);
    	return result;
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
    protected PlannerResult planRideshareItinerary(Instant now, GeoLocation fromPlace, GeoLocation toPlace, Instant travelTime, boolean useAsArrivalTime, Integer maxWalkDistance, List<GeoLocation> via) {
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
     * Assign rideshare attributes to the car/rideshare legs. This functionality should probably be put closer to the rideshare service itself.
     * @param leg The rideshare leg for the passenger 
     * @param ride The ride carrying the passenger. 
     */
    protected void assignRideToPassengerLeg(Leg leg, Ride ride) {
		leg.setDriverId(ride.getDriverRef());
		leg.setDriverName(ride.getDriver().getName());
		leg.setVehicleId(ride.getCarRef());
		leg.setVehicleLicensePlate(ride.getCar().getLicensePlate());
		leg.setVehicleName(ride.getCar().getName());
		leg.setTripId(ride.getUrn());
		leg.setTraverseMode(TraverseMode.RIDESHARE);
		// For Rideshare booking is always required.
		leg.setBookingRequired(true);
		// For Rideshare confirmation is requested from traveller and provider
		leg.setConfirmationByProviderRequested(true);
		leg.setConfirmationRequested(true);
		// Request synchronously a quote
		quoteRequestedEvent.fire(leg);
		// Quote received now
    }

    /**
	 * Try to find a ride from passenger departure to passenger destination. Each
	 * possibility is an itinerary. For traverse mode CAR we assume (as approximation) the travel
	 * time is independent of the departure time. This is correct for OTP. Only more
	 * advanced planners include the congestion dimension. We need to make a
	 * spatiotemporal selection from the possible rides. The spatial dimension is
	 * covered by the ellipse estimator. For the temporal dimension we have to set
	 * an eligibility interval: A difficult issue as we do not know the passengers
	 * intentions and concerns. Therefore we use earliestDeparture and latestArrival.
	 * 
	 * @param fromPlace The departure location of the passenger 
	 * @param toPlace The arrival location of the passenger
	 * @param earliestDeparture The earliest possible departure time. If not set then it will be set to
	 * 						 RIDESHARE_MAX_SLACK_BACKWARD hours before arrival time, with <code>now<code> as minimum.
	 * @param latestArrival The latest possible arrival time. If not set then it will be set to
	 * 						 RIDESHARE_MAX_SLACK_FORWARD hours after earliest departure time.
	 * @param maxWalkDistance The maximum distance the passenger is prepared to walk
	 * @param nrSeats The number of seats required
	 * @return A list of possible itineraries.
	 */
    protected List<PlannerResult> searchRideshareOnly(Instant now, GeoLocation fromPlace, GeoLocation toPlace,  Instant travelTime, boolean useAsArrivalTime,
    		Instant earliestDeparture, Instant latestArrival, Integer maxWalkDistance, Integer nrSeats) {
    	RideshareResult ridesResult = searchRides(now, fromPlace, toPlace,  travelTime, useAsArrivalTime, earliestDeparture, latestArrival, nrSeats, RIDESHARE_LENIENT_SEARCH, MAX_RIDESHARES, 0);
		List<GeoLocation> intermediatePlaces = Arrays.asList(new GeoLocation[] { fromPlace, toPlace });
		List<PlannerResult> results = new ArrayList<>();
		results.add(new PlannerResult(ridesResult.getReport()));
    	for (Ride ride : ridesResult.getPage().getData()) {
    		if (log.isDebugEnabled()) {
    			log.debug("searchRides option: " + ride.toStringCompact());
    		}
    		// Use always the riders departure time. The itineraries will be scored later on.
        	GeoLocation from = ride.getFrom();
        	GeoLocation to = ride.getTo();
        	// Calculate for each ride found the itinerary when the passenger would ride along, i.e., add the pickup and drop-off location
        	// as intermediate places to the OTP planner and calculate the itinerary.
    		Instant rideDepTime = ride.getDepartureTime();
        	PlannerResult driverSharedRidePlanResult = planRideshareItinerary(now, from, to, rideDepTime, false, maxWalkDistance, intermediatePlaces);
        	PlannerResult passengerSharedRidePlanResult = new PlannerResult(driverSharedRidePlanResult.getReport());
        	results.add(passengerSharedRidePlanResult);
    		if (driverSharedRidePlanResult.hasError()) {
        		log.warn("Skip itinerary (RS) due to OTP error: " + driverSharedRidePlanResult.getReport().shortReport());
    		} else {
        		Itinerary driverItinerary = driverSharedRidePlanResult.getItineraries().get(0);
        		boolean accepted = Stream.of(new DetourMetersAcceptable(ride, driverSharedRidePlanResult.getReport()), new DetourSecondsAcceptable(ride, driverSharedRidePlanResult.getReport()))
        				.reduce(x -> true, Predicate::and)
        				.test(driverItinerary);
            	if (accepted) {
    	        	// We have the plan for the driver now. Add the itineraries but keep only the intermediate leg(s). This is a deep copy!
            		Itinerary passengerItinerary = driverItinerary.createSingleLeggedItinerary(fromPlace, toPlace);
            		if (passengerItinerary.getLegs().size() != 1) {
            			throw new IllegalStateException("Expected to find a single leg, instead of " + passengerItinerary.getLegs().size());
            		}
            		passengerItinerary.getLegs().forEach(leg -> assignRideToPassengerLeg(leg, ride));
            		passengerSharedRidePlanResult.addItineraries(Collections.singletonList(passengerItinerary));
            	}
    		}
		}
    	// These are itineraries for the passenger, not the complete ones for the driver
    	return results;
    }

    
    protected void addRideshareAsFirstLeg(TripPlan plan, Set<Stop> transitBoardingStops, Set<TraverseMode> transitModalities) {
    	log.debug("Search for first leg by Car");
    	if (plan.getMaxTransfers() != null && plan.getMaxTransfers() < 0) {
    		throw new IllegalArgumentException("maxTransfers cannot be 0 at this point");
    	}
    	Integer maxPublicTransportTransfers = plan.getMaxTransfers() == null ? null : plan.getMaxTransfers() - 1;
    	for (Stop place : transitBoardingStops) {
    		// Try to find a shared ride from passenger's departure to a transit hub
        	List<PlannerResult> rideResults = searchRideshareOnly(plan.getRequestTime(), plan.getFrom(), place.getLocation(), plan.getTravelTime(), false,
        			plan.getEarliestDepartureTime(), plan.getLatestArrivalTime(), plan.getMaxWalkDistance(), plan.getNrSeats());
        	// Add all reports
        	rideResults.stream().forEach(pr -> plan.addPlannerReport(pr.getReport()));
        	// Collect all possible rides 
        	List<Itinerary> passengerCarItineraries = rideResults.stream()
        			.flatMap(pr -> pr.getItineraries().stream())
        			.collect(Collectors.toList());
    		// Create a transit plan from shared ride dropoff to passenger's destination
    		// Add x minutes waiting time at drop off
        	// Extract the leg for the passenger and create a complete itinerary for the passenger
        	for (Itinerary dit : passengerCarItineraries) {
            	Instant transitStart  = dit.getLegs().get(0).getEndTime().plusSeconds(CAR_TO_TRANSIT_SLACK);
        		PlannerResult transitResult = otpDao.createPlan(plan.getRequestTime(), place.getLocation(), plan.getTo(), transitStart,  false, transitModalities, 
	        			false, plan.getMaxWalkDistance(), maxPublicTransportTransfers, null, 2 /* To see repeating */);
        		plan.addPlannerReport(transitResult.getReport());
        		if (transitResult.hasError()) {
            		log.warn("Skip itinerary (RS first) due to OTP error: " + transitResult.getReport().shortReport());
        		} else {
        			plan.addItineraries(transitResult.getItineraries()
        					.stream()
        					.map(it -> dit.append(it))
        					.collect(Collectors.toList()));
        		}
			}
		}
    }

    protected void addRideshareAsLastLeg(TripPlan plan, Set<Stop> transitAlightingStops, Set<TraverseMode> transitModalities) {
    	// Try to find a ride from transit place to drop-off (last mile by car)
    	log.debug("Search for a last leg by Car");
    	if (plan.getMaxTransfers() != null && plan.getMaxTransfers() < 0) {
    		throw new IllegalArgumentException("maxTransfers cannot be 0 at this point");
    	}
    	Integer maxPublicTransportTransfers = plan.getMaxTransfers() == null ? null : plan.getMaxTransfers() - 1;
    	//FIXME Should be in fact alighting stops 
    	for (Stop place : transitAlightingStops) {
    		// Try to find a shared ride from transit hub to passenger's destination
    		List<PlannerResult> rideResults = searchRideshareOnly(plan.getRequestTime(), place.getLocation(), plan.getTo(), plan.getTravelTime(), true,
        		plan.getEarliestDepartureTime(), plan.getLatestArrivalTime(), plan.getMaxWalkDistance(), plan.getNrSeats());
        	// Add all reports
        	rideResults.stream().forEach(pr -> plan.addPlannerReport(pr.getReport()));
        	// Collect all possible rides 
        	List<Itinerary> passengerCarItineraries = rideResults.stream()
        			.flatMap(pr -> pr.getItineraries().stream())
        			.collect(Collectors.toList());
    		// Create a transit plan from passenger departure to shared ride pickup
    		// Add x minutes waiting time at pick off
        	// Extract the leg for the passenger and create a complete itinerary for the passenger
        	for (Itinerary dit : passengerCarItineraries) {
        		Instant transitEnd  = dit.getLegs().get(0).getEndTime().plusSeconds(CAR_TO_TRANSIT_SLACK);
        		PlannerResult transitResult = otpDao.createPlan(plan.getRequestTime(), plan.getFrom(), place.getLocation(), transitEnd, true, transitModalities,  
            			false, plan.getMaxWalkDistance(), maxPublicTransportTransfers, null, 2 /* To see repeating */);
        		plan.addPlannerReport(transitResult.getReport());
        		if (transitResult.hasError()) {
            		log.warn("Skip itinerary (RS last) due to OTP error: " + transitResult.getReport().shortReport());
        		} else {
        			plan.addItineraries(transitResult.getItineraries()
        					.stream()
        					.map(it -> dit.prepend(it))
        					.collect(Collectors.toList()));
        		}
			}
		}
    }

    protected Set<Stop> collectStops(TripPlan plan, Set<Stop> transitStops, List<OtpCluster> nearbyClusters) {
    	Set<Stop> stops = transitStops;
    	if (stops.size() < 6) {
	    	// Perhaps we should also query for the nearest hubs in case the number of places is small (e.g., just one)
    		if (nearbyClusters.isEmpty()) {
    			nearbyClusters.addAll(searchImportantTransitStops(plan.getFrom(), plan.getTo(), plan.getTraverseModes(), 10));
    		}
    		stops = combineClustersIntoStops(transitStops, nearbyClusters);
    	}
    	if (log.isDebugEnabled()) {
    		log.debug("Collected stops: \n\t" + stops.stream().map(p -> p.toString()).collect(Collectors.joining("\n\t")));
    	}
    	// TODO Sort on distance, limit the number
    	return stops;
    }

    protected PlannerResult createTransitPlan(Instant now, GeoLocation fromPlace, GeoLocation toPlace, Instant travelTime, boolean isArrivalTime, 
    		Set<TraverseMode> modes, Integer maxWalkDistance, Integer maxTransfers, Integer maxItineraries) {
    	// For transit walk is necessary
		modes.add(TraverseMode.WALK);
		return otpDao.createPlan(now, fromPlace, toPlace, travelTime, isArrivalTime, 
					modes, false, maxWalkDistance, maxTransfers, null, maxItineraries);
    }
    
    /**
     * Creates a multi-modal travel plan for a passenger. Both ride share and transit options are considered.
     * For now a shared ride is only an option as first or last leg. 
     * The passenger might be more flexible than the departure or arrival time. For that reason the itineraries for public transport
     * will be a good fit given departure or arrival time, but itineraries involving a shared ride vary much more in time.
     * An attempt is made to sort the itineraries by attractiveness to the passenger.   
     * @param now The reference point of time. Planning must occur beyond this point.
     * @param fromPlace The departure point of the passenger
     * @param toPlace The destination of the passenger
     * @param depTime The (intended) departure time. Specify either departure or arrival time. 
     * @param arrTime The (intended) arrival time. Specify either departure or arrival time.
     * @param modalities The eligible modalities to travel with.
     * @param maxWalkDistance The maximum distance the passenger is prepared to walk.
     * @param nrSeats The number of seats the passenger wants to use in a car.
     * @return
     */
    protected TripPlan searchMultiModal(TripPlan plan) throws BadRequestException {
		// 
		Set<TraverseMode> transitModalities = plan.getTraverseModes().stream().filter(m -> m.isTransit()).collect(Collectors.toSet());
		boolean rideshareEligable = plan.getTraverseModes().contains(TraverseMode.RIDESHARE);
		if (!transitModalities.isEmpty()) {
			// Transit is eligable. Add the transit itineraries as calculated by OTP.
			PlannerResult transitResult = createTransitPlan(plan.getRequestTime(), plan.getFrom(), plan.getTo(), plan.getTravelTime(), plan.isUseAsArrivalTime(), 
					transitModalities, plan.getMaxWalkDistance(), plan.getMaxTransfers(), null);
    		plan.addPlannerReport(transitResult.getReport());
    		if (transitResult.hasError()) {
        		log.warn("Skip itineraries (transit) due to OTP error: " + transitResult.getReport().shortReport());
    		} else {
    			plan.addItineraries(transitResult.getItineraries());
    		}
		}

		if (rideshareEligable) {
	    	// Add the RIDESHARE only itineraries
			List<PlannerResult> rideResults = searchRideshareOnly(plan.getRequestTime(), plan.getFrom(), plan.getTo(), plan.getTravelTime(), plan.isUseAsArrivalTime(),
					plan.getEarliestDepartureTime(), plan.getLatestArrivalTime(), plan.getMaxWalkDistance(), plan.getNrSeats());
        	rideResults.stream().forEach(pr -> plan.addPlannerReport(pr.getReport()));
        	List<Itinerary> passengerItineraries = rideResults.stream()
        			.flatMap(pr -> pr.getItineraries().stream())
        			.collect(Collectors.toList());
	    	plan.addItineraries(passengerItineraries);

			// If transit is an option too then collect possible pickup and drop-off places near transit stops
			if (!transitModalities.isEmpty() && plan.isRideshareLegAllowed() && 
					(plan.getMaxTransfers() == null || plan.getMaxTransfers() >= 1)) {
				Set<Stop> transitBoardingStops = null;
				// Calculate a transit reference plan to find potential boarding or alighting places. 
				PlannerResult transitRefResult = createTransitPlan(plan.getRequestTime(), plan.getFrom(), plan.getTo(), plan.getTravelTime(), plan.isUseAsArrivalTime(),
						transitModalities, 50000, plan.getMaxTransfers() == null ? null : plan.getMaxTransfers() - 1, 2);
	    		if (transitRefResult.hasError()) {
	        		log.warn("Skip itineraries (transit reference) due to OTP error: " + transitRefResult.getReport().shortReport());
	    		} else {
					List<OtpCluster> nearbyClusters = new ArrayList<>();
					// Collect the stops. If there are no stops or too few, collect potential clusters
					//FIXME The ordering of the clusters depends probably on first or last leg. Check.
			    	transitBoardingStops = collectStops(plan, findTransitBoardingStops(transitRefResult.getItineraries()), nearbyClusters);
		    		if (plan.isFirstLegRideshareAllowed()) {
		        		addRideshareAsFirstLeg(plan, transitBoardingStops, transitModalities);
		    		}
		    		if (plan.isLastLegRideshareAllowed()) {
		        		addRideshareAsLastLeg(plan, transitBoardingStops, transitModalities);
		    		}
	    		}
			}
		}
		// Calculate totals
		plan.getItineraries().forEach(it -> it.updateFare());
    	rankItineraries(plan);
    	// The itineraries are listed by the plan ordered by score descending
    	return plan;
    }

    protected void rankItineraries(TripPlan plan) {
    	BasicItineraryRankingAlgorithm ranker = new BasicItineraryRankingAlgorithm();
    	for (Itinerary it: plan.getItineraries()) {
    		ranker.calculateScore(it, plan.getTravelTime(), plan.isUseAsArrivalTime());
    	}
    }

    /**
     * Finds all selected stops in a transit itinerary.  
     * @param transitPlan
     * @param stopSelector
     * @return
     */
    protected Collection<Stop> findTransitStops(TripPlan transitPlan, Function<Leg, Stop> stopSelector) {
    	return transitPlan.getItineraries().stream()
    			.flatMap(it -> it.getLegs().stream())
    			.filter(leg -> leg.getTraverseMode().isTransit())
    			.map(stopSelector)
    			.collect(Collectors.toCollection(LinkedHashSet::new));
    }
    
   	protected Set<Stop> findTransitBoardingStops(List<Itinerary> itineraries) {
    	// Strategy A: Replace the first leg(s) with ride share options
    	Set<Stop> stops = new LinkedHashSet<>();
    	for (Itinerary it : itineraries) {
    		if (it.getTransfers() != null && it.getTransfers() > 1) {
    			for (int transfer = 0; transfer < it.getTransfers(); transfer++) {	
    				int skipTransitLegs = 0;
    				for (Iterator<Leg> legix = it.getLegs().iterator(); legix.hasNext();) {
    					Leg leg = legix.next();
    					if (leg.getTraverseMode() == TraverseMode.WALK) {
    						// Continue
    					} else if (skipTransitLegs < transfer + 1) {
       						skipTransitLegs++;
    					} else {
    						// Save the onboarding place
    						Stop s = leg.getFrom().copy();
    						s.setDepartureTime(null);
    						s.setArrivalTime(null);
    						stops.add(s);
    						break;
    					}
					}
				}
    		}
		}
    	return stops;
    }
    
    protected List<OtpCluster> searchImportantTransitStops(GeoLocation fromPlace, GeoLocation toPlace, Set<TraverseMode> modes, int maxResults) {
    	EligibleArea ea = EllipseHelper.calculateEllipse(fromPlace.getPoint(), toPlace.getPoint(), null, PASSENGER_RELATIVE_MAX_DETOUR / 2);
//    	log.debug("Passenger ellipse: " + GeometryHelper.createWKT(ea.eligibleAreaGeometry));
    	// Find all hub-alike transit clusters inside this ellipse
//    	TraverseMode[] transitModes = Arrays.stream(modes).filter(mode -> mode.isTransit()).toArray(TraverseMode[]::new);
    	return otpClusterDao.findImportantHubs(fromPlace, ea.eligibleAreaGeometry, maxResults);
    	
    }

    protected Set<Stop> combineClustersIntoStops(Collection<Stop> otherStops, Collection<OtpCluster> clusters) {
		List<Stop> stops = new ArrayList<>(otherStops);
		for (OtpCluster c : clusters) {
			boolean alreadyFound = false;
			for (Stop s : stops) {
				if (s.getLocation().getDistanceFlat(c.getLocation()) < 0.2) {
					// Is near existing place, skip this one
					alreadyFound = true;
					break;
				}
				
			}
			if (! alreadyFound) {
				stops.add(new Stop(c.getLocation()));
			}
		}
		return new LinkedHashSet<Stop>(stops);
	}

    public void throwRuntimeException() {
    	throw new RuntimeException("A bug in a EJB!");
    }
    public void throwAccessException() {
    	throw new EJBAccessException("Can't access this!");
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
    protected Instant calculateEarliestDepartureTime(Instant travelTime, boolean useAsArrivalTime) {
    	if (useAsArrivalTime) {
    		travelTime = travelTime.minusSeconds(STANDARD_TRAVEL_DURATION);
    	}
    	LocalTime localTravelTime = LocalTime.from(travelTime.atZone(ZoneId.of(DEFAULT_TIME_ZONE)));
    	Instant earliestTime;
    	if (localTravelTime.isBefore(DAY_START)) {
    		earliestTime = travelTime.minusSeconds(REST_TIME_SLACK * 60 * 60); 
    	} else if (localTravelTime.isAfter(DAY_END)) {
    		earliestTime = travelTime.minusSeconds(REST_TIME_SLACK * 60 * 60); 
    	} else {
    		int slack = DAY_TIME_SLACK * 60 * 60;
    		if (localTravelTime.minusSeconds(slack).isAfter(DAY_START.minusSeconds(REST_TIME_SLACK * 60 * 60))) {
        		earliestTime = travelTime.minusSeconds(slack);
    		} else {
    			LocalDate date = LocalDate.from(travelTime.atZone(ZoneId.of(DEFAULT_TIME_ZONE))); 
        		earliestTime = LocalDateTime.of(date, DAY_START.minusSeconds(REST_TIME_SLACK * 60 * 60)).atZone(ZoneId.of(DEFAULT_TIME_ZONE)).toInstant();
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
    protected Instant calculateLatestArrivalTime(Instant travelTime, boolean useAsArrivalTime) {
    	if (!useAsArrivalTime) {
    		travelTime = travelTime.plusSeconds(STANDARD_TRAVEL_DURATION);
    	}
    	LocalTime localTravelTime = LocalTime.from(travelTime.atZone(ZoneId.of(DEFAULT_TIME_ZONE)));
    	Instant latestTime;
    	if (localTravelTime.isAfter(DAY_END)) {
    		latestTime = travelTime.plusSeconds(REST_TIME_SLACK * 60 * 60); 
    	} else if (localTravelTime.isBefore(DAY_START)) {
    		latestTime = travelTime.plusSeconds(REST_TIME_SLACK * 60 * 60); 
    	} else {
    		int slack = DAY_TIME_SLACK * 60 * 60;
    		if (localTravelTime.plusSeconds(slack).isBefore(DAY_END.plusSeconds(REST_TIME_SLACK * 60 * 60))) {
        		latestTime = travelTime.plusSeconds(slack);
    		} else {
    			LocalDate date = LocalDate.from(travelTime.atZone(ZoneId.of(DEFAULT_TIME_ZONE))); 
        		latestTime = LocalDateTime.of(date, DAY_END.plusSeconds(REST_TIME_SLACK * 60 * 60)).atZone(ZoneId.of(DEFAULT_TIME_ZONE)).toInstant();
    		}
    	}
    	return latestTime;
    }

    protected void sanitizePlanInput(TripPlan plan) throws BadRequestException {
    	if (plan.getId() != null) {
    		throw new IllegalStateException("New plan should not have a persistent ID");
    	}
    	Instant now = plan.getRequestTime();
    	if (now == null) {
    		throw new BadRequestException("Parameter 'now' is mandatory");
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

    public TripPlan createAndReturnTripPlan(PlannerUser traveller, TripPlan plan, Instant now) throws BusinessException {
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
       	if (plan.getPlanType() != PlanType.SHOUT_OUT) {
       		// Start a search
       		plan = searchMultiModal(plan);
        	plan.close();
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
     * @param user the user for whom the plan is created
     * @param plan the new plan
     * @return The ID of the plan just created.
     * @throws BusinessException 
     */
    public Long createTripPlan(PlannerUser traveller, TripPlan plan, Instant now) throws BusinessException {
    	return createAndReturnTripPlan(traveller, plan, now).getId();
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
     * @return A list of trips matching tjhe criteria.
     */
    public PagedResult<TripPlan> listTripPlans(PlannerUser traveller, PlanType planType, Instant since, Instant until, Boolean inProgressOnly, 
    		SortDirection sortDirection, Integer maxResults, Integer offset) throws BadRequestException {
    	if (until != null && since != null && !until.isAfter(since)) {
    		throw new BadRequestException("Constraint violation: 'until' must be later than 'since'.");
    	}
    	if (maxResults != null && maxResults > 100) {
    		throw new BadRequestException("Constraint violation: 'maxResults' <= 100.");
    	}
    	if (maxResults != null && maxResults <= 0) {
    		throw new BadRequestException("Constraint violation: 'maxResults' > 0.");
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
    	return new PagedResult<TripPlan>(results, maxResults, offset, totalCount);
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
    	return new PagedResult<TripPlan>(results, maxResults, offset, totalCount);
    }

    /**
     * Retrieves a shout-out trip plan. Only the plan itself is retrieved. For all details see getTripPlan(). 
     * @param id the id of the shout-out trip plan
     * @return The TripPlan object
     * @throws NotFoundException In case of an invalid trip plan ID or when the actual type is not shout-out.
     */
    public TripPlan getShoutOutPlan(Long id) throws NotFoundException {
    	TripPlan plandb = tripPlanDao.find(id)
    			.orElse(null);
    	if (plandb == null || plandb.getPlanType() != PlanType.SHOUT_OUT) {
    		throw new NotFoundException("No such shout-out: " + id);
    	}
    	return plandb;
    }
    

    /**
     * Resolves a shout-out by issuing a trip plan as a potential solution for the shout-out by a ride by the driver. 
     * @param now The reference point in time. Especially used for testing.
     * @param driver The driver asking to verify a shout-out. 
     * @param shoutOutPlanRef A reference to the shout-out of a traveller.
     * @param driverPlan The input parameters of the driver
     * @return A trip plan calculated  to fill-in the shout-out.
     * @throws NotFoundException In case the shout-out could not be found.
     */
    public TripPlan resolveShoutOut(Instant now, PlannerUser driver, String shoutOutPlanRef, TripPlan driverPlan, TraverseMode traverseMode) throws NotFoundException, BusinessException {
    	Long pid = PlannerUrnHelper.getId(TripPlan.URN_PREFIX, shoutOutPlanRef);
    	TripPlan travPlan = tripPlanDao.find(pid).orElseThrow(() -> new NotFoundException("No such TripPlan: " + shoutOutPlanRef));
    	if (!travPlan.isInProgress()) {
    		throw new CreateException("Shout-out has already been closed");
    	}
    	if (traverseMode != TraverseMode.RIDESHARE) {
    		throw new BadRequestException("Only RIDESHARE modality is supported");
    	}
    	boolean adjustDepartureTime = false;
    	driverPlan.setTraveller(driver);
    	driverPlan.setRequestTime(now);
    	if (driverPlan.getFrom() == null) {
    		throw new IllegalArgumentException("Driver needs to specify a departure location");
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
    	driverPlan.setTraverseModes(Collections.singleton(TraverseMode.RIDESHARE));
    	List<GeoLocation> intermediatePlaces = new ArrayList<>();
    	intermediatePlaces.add(travPlan.getFrom());
    	intermediatePlaces.add(travPlan.getTo());
    	PlannerResult result = planRideshareItinerary(now, driverPlan.getFrom(), driverPlan.getTo(), 
    			driverPlan.getTravelTime(),  driverPlan.isUseAsArrivalTime(), driverPlan.getMaxWalkDistance(), intermediatePlaces);
    	if (adjustDepartureTime && result.getItineraries().size() > 0) {
    		// Shift all the timestamps in the plan in such a way that the pickup or drop-off time matches the travel time of the proposed passenger
    		Itinerary it = result.getItineraries().get(0);
//    		log.debug("Before: " + it.toString());
    		GeoLocation refLoc = travPlan.isUseAsArrivalTime() ? travPlan.getTo() : travPlan.getFrom();
    		it.shiftItineraryTiming(refLoc, travPlan.getTravelTime(), travPlan.isUseAsArrivalTime());
    		// Fix the travel time of the driver and set it to the departure time of the first leg
			driverPlan.setTravelTime(it.getDepartureTime());
			driverPlan.setUseAsArrivalTime(false);
			result.getReport().setTravelTime(driverPlan.getTravelTime());
			result.getReport().setUseAsArrivalTime(driverPlan.isUseAsArrivalTime());
//    		log.debug("After: " + it.toString());
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
     * 						and include his private departure and destination locations. The assumption for now is that the pickup and
     * 						drop-off locations of the traveller are part of the providers itinerary. If not the (short) walks have to 
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
    		assignRideToPassengerLeg(leg, ride);
        	leg.setBookingId(bookingRef);
    	});
    	// The legs are stil in planning state!
    	passengerItinerary.getLegs().forEach(leg -> leg.setState(TripState.PLANNING));
    }

    /**
     * 
     * @param plan
     * @param itineraryToKeep
     * @throws BusinessException 
     */
    protected void cancelBookedLegs(TripPlan plan, Optional<Itinerary> itineraryToKeep, String cancelReason) throws BusinessException {
    	List<Leg> bookedLegs = plan.getItineraries().stream()
        		.filter(it -> !itineraryToKeep.isPresent() || !it.equals(itineraryToKeep.get()))
        		.flatMap(it -> it.getLegs().stream())
    			.filter(leg -> leg.getBookingId() != null)
    			.collect(Collectors.toList());
    	for (Leg leg : bookedLegs) {
    		EventFireWrapper.fire(bookingRejectedEvent, new BookingProposalRejectedEvent(plan, leg, cancelReason));
        	leg.setState(TripState.CANCELLED);
		}
    }

    /**
     * Handler on the event for resolving an shout-out into an itinerary: Cancel other options.
     * @param event
     * @throws BusinessException
     */
    public void onShoutOutResolved(@Observes(during = TransactionPhase.IN_PROGRESS) ShoutOutResolvedEvent event) throws BusinessException {
    	TripPlan plan = getTripPlan(event.getSelectedItinerary().getTripPlan().getId());
    	if (plan.getPlanType() != PlanType.SHOUT_OUT) {
    		throw new IllegalStateException("ShoutOutResolvedEvent received with non-shout-out plan");
    	}
    	cancelBookedLegs(plan, Optional.of(event.getSelectedItinerary()), "Andere oplossing gekozen");
    	plan.close();
    }

    /**
     * Cancels the proposed booking leg in an itinerary and updates the state. This method is called in response to a cancellation from the transport provider.
     * This call is intended to update the trip plan state only.
     * @param tripplanRef
     * @param bookingRef
     * @throws NotFoundException
     */
    public void cancelBooking(String tripPlanRef, String bookingRef) throws NotFoundException {
    	TripPlan plan = getTripPlan(PlannerUrnHelper.getId(TripPlan.URN_PREFIX, tripPlanRef));
    	plan.getItineraries().stream()
    		.flatMap(it -> it.getLegs().stream())
    		.filter(leg -> bookingRef.equals(leg.getBookingId()))
    		.forEach(leg -> leg.setState(TripState.CANCELLED));
    }

    /**
     * Cancels a shout-out, i.e., the plan is no longer receiving itineraries. 
     * @param id the id of the trip plan
     * @throws BusinessException 
     */
    public void cancelShoutOut(Long id) throws BusinessException {
    	TripPlan plan = tripPlanDao.find(id)
    			.orElseThrow(() -> new NotFoundException("No such trip plan: " + id));
    	if (plan.getPlanType() != PlanType.SHOUT_OUT) {
    		throw new BadRequestException("Plan is not a shout-out: " + id);
    	}
    	if (plan.isOpen()) {
        	cancelBookedLegs(plan, Optional.empty(), "Plan is geannuleerd");
    		plan.close();
    	}
    }
    
}