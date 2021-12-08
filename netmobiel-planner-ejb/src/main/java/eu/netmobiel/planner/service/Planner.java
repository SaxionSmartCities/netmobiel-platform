package eu.netmobiel.planner.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.EllipseHelper;
import eu.netmobiel.commons.util.EllipseHelper.EligibleArea;
import eu.netmobiel.commons.util.ExceptionUtil;
import eu.netmobiel.commons.util.GeometryHelper;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.planner.model.Itinerary;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.OtpCluster;
import eu.netmobiel.planner.model.PlannerReport;
import eu.netmobiel.planner.model.PlannerResult;
import eu.netmobiel.planner.model.RideshareResult;
import eu.netmobiel.planner.model.Stop;
import eu.netmobiel.planner.model.ToolType;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.repository.OpenTripPlannerDao;
import eu.netmobiel.planner.repository.OtpClusterDao;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.service.RideManager;

/**
 * The multi-modal planner of NetMobiel. 
 *  
 * @author Jaap Reitsma
 *
 */
@ApplicationScoped
@Logging
public class Planner {
	public static final Integer MAX_RESULTS = 10; 
	private static final float PASSENGER_RELATIVE_MAX_DETOUR = 1.0f;
	private static final Integer CAR_TO_TRANSIT_SLACK = 10 * 60; // [seconds]
	private static final Integer TRANSIT_TO_CAR_SLACK = 10 * 60; // [seconds]
	private static final int MAX_RIDESHARES = 5;	
	private static final boolean RIDESHARE_LENIENT_SEARCH = true;	
	
	@Inject
    private Logger log;

    @Inject
    private TripPlanHelper tripPlanHelper;
    @Inject
    private RideManager rideManager;
    @Inject
    private OpenTripPlannerDao otpDao;
    @Inject
    private OtpClusterDao otpClusterDao;

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
    
    private RideshareResult searchRides(TripPlan plan, GeoLocation fromPlace, GeoLocation toPlace, boolean lenient, Integer maxResults, Integer offset) {
    	PlannerReport report = new PlannerReport(plan);
    	report.setFrom(fromPlace);
    	report.setTo(toPlace);
    	report.setToolType(ToolType.NETMOBIEL_RIDESHARE);
    	report.setMaxResults(maxResults);
    	report.setStartPosition(offset);
    	report.setLenientSearch(lenient);
    	report.setRequestGeometry(GeometryHelper.createLines(fromPlace.getPoint().getCoordinate(), toPlace.getPoint().getCoordinate(), null));
    	RideshareResult result = new RideshareResult(report);
    	PagedResult<Ride> ridePage = null;;
    	long start = System.currentTimeMillis();
    	try { 
    		ridePage = rideManager.search(plan.getTraveller().getManagedIdentity(), fromPlace, toPlace,  
    				plan.getEarliestDepartureTime(), plan.getLatestArrivalTime(), plan.getNrSeats(), 
    				lenient, maxResults, offset);
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
     * Calculates an itinerary for a rideshare ride from the driver's departure location to the driver's destination, in between picking up a 
     * passenger and dropping-off the passenger at a different location.
     * Where should this calculation be done? Is it core NetMobiel, or should the transport provider give the estimation? Probably the latter.
     * For shout-out the planning question should be asked at the rideshare service. Their planner will provide an answer.
     * @param now The time perspective of the call (in regular use always the current time)
     * @param fromPlace The departure location of the driver
     * @param toPlace the destination of the driver
     * @param travelTime the travel time of the driver
     * @param useAsArrivalTime Whether to interpret the travel time as arrival time
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
	 * Try to find a ride from passenger departure to passenger destination. Each
	 * possibility is an itinerary. For traverse mode CAR we assume (as approximation) the travel
	 * time is independent of the departure time. This is correct for OTP. Only more
	 * advanced planners include the congestion dimension. We need to make a
	 * spatiotemporal selection from the possible rides. The spatial dimension is
	 * covered by the ellipse estimator. For the temporal dimension we have to set
	 * an eligibility interval: A difficult issue as we do not know the passengers
	 * intentions and concerns. Therefore we use earliestDeparture and latestArrival.
	 * 
	 * @param plan the pan of the traveller
	 * @param fromPlace The departure location of the passenger. Not necessarily the departure location of the plan (in case of a multi-legged journey).
	 * @param toPlace The arrival location of the passenger.  Not necessarily the departure location of the plan (in case of a multi-legged journey).
	 * @return A list of possible itineraries.
	 */
    private List<PlannerResult> searchRideshareOnly(TripPlan plan, GeoLocation fromPlace, GeoLocation toPlace) {
    	RideshareResult ridesResult = searchRides(plan, fromPlace, toPlace,  RIDESHARE_LENIENT_SEARCH, MAX_RIDESHARES, 0);
		List<GeoLocation> intermediatePlaces = Arrays.asList(new GeoLocation[] { fromPlace, toPlace });
		List<PlannerResult> results = new ArrayList<>();
		results.add(new PlannerResult(ridesResult.getReport()));
    	for (Ride ride : ridesResult.getPage().getData()) {
    		if (log.isDebugEnabled()) {
    			log.debug("searchRides option: " + ride.toStringCompact());
    		}
    		// For each ride, calculate an itinerary for the shared ride
        	GeoLocation from = ride.getFrom();
        	GeoLocation to = ride.getTo();
        	// Calculate for each ride found the itinerary when the passenger would ride along, i.e., add the pickup and drop-off location
        	// as intermediate places to the OTP planner and calculate the itinerary.
    		Instant driverTravelTime = ride.isArrivalTimePinned() ? ride.getArrivalTime() : ride.getDepartureTime();
        	PlannerResult driverSharedRidePlanResult = planRideshareItinerary(plan.getRequestTime(), from, to, driverTravelTime, ride.isArrivalTimePinned(), plan.getMaxWalkDistance(), intermediatePlaces);
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
    	        	// We have the plan for the driver now. extract the passenger itineraries (intermediate leg(s)). This is a deep copy!
            		Itinerary passengerItinerary = driverItinerary.createSingleLeggedItinerary(fromPlace, toPlace);
            		if (passengerItinerary.getLegs().size() != 1) {
            			throw new IllegalStateException("Expected to find a single leg, instead of " + passengerItinerary.getLegs().size());
            		}
            		// Add the rideshare details to the passengers leg(s)
            		passengerItinerary.getLegs().forEach(leg -> tripPlanHelper.assignRideToPassengerLeg(leg, ride));
            		if (isValid(passengerItinerary)) {
            			// OK, it looks a sane itinerary, add it to the results
            			passengerSharedRidePlanResult.addItineraries(Collections.singletonList(passengerItinerary));
            		}
            	}
    		}
		}
    	// These are itineraries for the passenger, not the complete ones for the driver
    	return results;
    }

    boolean isValid(Itinerary it) {
    	boolean valid = false;
    	try {
	    	it.validate();
			valid = true;
    	} catch (IllegalStateException ex) {
    		log.error(ex.toString());
    		log.error("Bad "+ it.toString());
    	}
		return valid;
    }
    
    private void addRideshareAsFirstLeg(TripPlan plan, Set<Stop> transitBoardingStops, Set<TraverseMode> transitModalities) {
    	log.debug("Search for first leg by Car");
    	if (plan.getMaxTransfers() != null && plan.getMaxTransfers() < 0) {
    		throw new IllegalArgumentException("maxTransfers cannot be 0 at this point");
    	}
    	Integer maxPublicTransportTransfers = plan.getMaxTransfers() == null ? null : plan.getMaxTransfers() - 1;
    	for (Stop place : transitBoardingStops) {
    		// Try to find a shared ride from passenger's departure to a transit hub
        	List<PlannerResult> rideResults = searchRideshareOnly(plan, plan.getFrom(), place.getLocation());
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
	        			false, plan.getMaxWalkDistance(), maxPublicTransportTransfers, null, 1);
        		plan.addPlannerReport(transitResult.getReport());
        		if (transitResult.hasError()) {
            		log.warn("Skip itinerary (RS first) due to OTP error: " + transitResult.getReport().shortReport());
        		} else {
        			// if the OTP passenger itinerary is walk only, no slack is needed, move the itinerary backward
        			// the itinerary characteristics are recalculated in the prepend()
        			transitResult.getItineraries().stream()
        			.filter(Itinerary::isWalkOnly)
        			.forEach(it -> it.shiftLinear(Duration.ofSeconds(-CAR_TO_TRANSIT_SLACK)));
        			plan.addItineraries(transitResult.getItineraries()
        					.stream()
        					.filter(it -> isValid(it))
        					.map(it -> dit.append(it))
        					.filter(it -> isValid(it))
        					.collect(Collectors.toList()));
        		}
			}
		}
    }

    private void addRideshareAsLastLeg(TripPlan plan, Set<Stop> transitAlightingStops, Set<TraverseMode> transitModalities) {
    	// Try to find a ride from transit place to drop-off (last mile by car)
    	log.debug("Search for a last leg by Car");
    	if (plan.getMaxTransfers() != null && plan.getMaxTransfers() < 0) {
    		throw new IllegalArgumentException("maxTransfers cannot be 0 at this point");
    	}
    	Integer maxPublicTransportTransfers = plan.getMaxTransfers() == null ? null : plan.getMaxTransfers() - 1;
    	//FIXME Should be in fact alighting stops 
    	for (Stop place : transitAlightingStops) {
    		// Try to find a shared ride from transit hub to passenger's destination
    		List<PlannerResult> rideResults = searchRideshareOnly(plan, place.getLocation(), plan.getTo());
        	// Add all reports
        	rideResults.stream().forEach(pr -> plan.addPlannerReport(pr.getReport()));
        	// Collect all possible rides 
        	List<Itinerary> passengerCarItineraries = rideResults.stream()
        			.flatMap(pr -> pr.getItineraries().stream())
        			.collect(Collectors.toList());
    		// Create a transit plan from passenger departure to shared ride pickup
    		// Add x minutes waiting time at pick up
        	// Extract the leg for the passenger and create a complete itinerary for the passenger
        	for (Itinerary dit : passengerCarItineraries) {
        		Instant transitEnd  = dit.getLegs().get(0).getStartTime().minusSeconds(TRANSIT_TO_CAR_SLACK);
        		PlannerResult transitResult = otpDao.createPlan(plan.getRequestTime(), plan.getFrom(), place.getLocation(), transitEnd, true, transitModalities,  
            			false, plan.getMaxWalkDistance(), maxPublicTransportTransfers, null, 1);
        		plan.addPlannerReport(transitResult.getReport());
        		if (transitResult.hasError()) {
            		log.warn("Skip itinerary (RS last) due to OTP error: " + transitResult.getReport().shortReport());
        		} else {
        			// if the OTP passenger itinerary is walk only, no slack is needed, move the itinerary forward
        			// the itinerary characteristics are recalculated in the prepend()
        			transitResult.getItineraries().stream()
        			.filter(Itinerary::isWalkOnly)
        			.forEach(it -> it.shiftLinear(Duration.ofSeconds(TRANSIT_TO_CAR_SLACK)));
        			plan.addItineraries(transitResult.getItineraries()
        					.stream()
        					.map(it -> dit.prepend(it))
        					.filter(it -> isValid(it))
        					.collect(Collectors.toList()));
        		}
			}
		}
    }

    private Set<Stop> collectStops(TripPlan plan, Set<Stop> transitStops, List<OtpCluster> nearbyClusters) {
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

    private PlannerResult createTransitPlan(Instant now, GeoLocation fromPlace, GeoLocation toPlace, Instant travelTime, boolean isArrivalTime, 
    		Set<TraverseMode> modes, Integer maxWalkDistance, Integer maxTransfers, Integer maxItineraries) {
    	// For transit walk is necessary
		modes.add(TraverseMode.WALK);
		return otpDao.createPlan(now, fromPlace, toPlace, travelTime, isArrivalTime, 
					modes, false, maxWalkDistance, maxTransfers, null, maxItineraries);
    }
    
    private void addRidesharePlans(TripPlan plan, Set<TraverseMode> transitModalities) {
    	// Add the RIDESHARE only itineraries
		List<PlannerResult> rideResults = searchRideshareOnly(plan, plan.getFrom(), plan.getTo());
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

    private static void rankItineraries(TripPlan plan) {
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
    @SuppressWarnings("unused")
	private static Collection<Stop> findTransitStops(TripPlan transitPlan, Function<Leg, Stop> stopSelector) {
    	return transitPlan.getItineraries().stream()
    			.flatMap(it -> it.getLegs().stream())
    			.filter(leg -> leg.getTraverseMode().isTransit())
    			.map(stopSelector)
    			.collect(Collectors.toCollection(LinkedHashSet::new));
    }
    
   	private static Set<Stop> findTransitBoardingStops(List<Itinerary> itineraries) {
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
    
    private List<OtpCluster> searchImportantTransitStops(GeoLocation fromPlace, GeoLocation toPlace, Set<TraverseMode> modes, int maxResults) {
    	EligibleArea ea = EllipseHelper.calculateEllipse(fromPlace.getPoint(), toPlace.getPoint(), null, PASSENGER_RELATIVE_MAX_DETOUR / 2);
//    	log.debug("Passenger ellipse: " + GeometryHelper.createWKT(ea.eligibleAreaGeometry));
    	// Find all hub-alike transit clusters inside this ellipse
//    	TraverseMode[] transitModes = Arrays.stream(modes).filter(mode -> mode.isTransit()).toArray(TraverseMode[]::new);
    	return otpClusterDao.findImportantHubs(fromPlace, ea.eligibleAreaGeometry, maxResults);
    	
    }

    private static Set<Stop> combineClustersIntoStops(Collection<Stop> otherStops, Collection<OtpCluster> clusters) {
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
		return new LinkedHashSet<>(stops);
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
    public TripPlan searchMultiModal(TripPlan plan) throws BadRequestException {
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
			addRidesharePlans(plan, transitModalities);
		}
		
		// Calculate totals
		plan.getItineraries().forEach(it -> it.updateFare());
    	rankItineraries(plan);
    	// The itineraries are listed by the plan ordered by score descending (when listed from the database), see model.
    	// FIXME Sort this somehow
//    	plan.getItineraries().sort(new Comparator<Itinerary>() {
//			@Override
//			public int compare(Itinerary it1, Itinerary it2) {
//				return -Double.compare(it1.getScore(), it2.getScore());
//			}
//		});
    	return plan;
    }

    /**
     * Calculates an itinerary for a ride by car from the passenger's departure location to the passenger's destination.
     * This itinerary is intended as a reference for the passenger to compare offers from drivers.
     * @param now The time perspective of the call (in regular use always the current time)
     * @param fromPlace The departure location of the passenger
     * @param toPlace the destination of the passenger
     * @param travelTime the travel time of the passenger
     * @param useAsArrivalTime Whether to interpret the travel time as arrival time
     * @param maxWalkDistance The maximum distance to walk, if necessary.
     * @return A planner result object.
     */
    public PlannerResult planItineraryByCar(Instant now, GeoLocation fromPlace, GeoLocation toPlace, Instant travelTime, boolean useAsArrivalTime, Integer maxWalkDistance) {
    	PlannerResult planResult = planRideshareItinerary(now, fromPlace, toPlace, travelTime, useAsArrivalTime, maxWalkDistance, null);
		// Add the rideshare details to the passengers leg(s)
		planResult.getItineraries().forEach(it -> it.getLegs().forEach(leg -> tripPlanHelper.assignFareToRideshareLeg(leg)));
   	
    	return planResult;
    }
}