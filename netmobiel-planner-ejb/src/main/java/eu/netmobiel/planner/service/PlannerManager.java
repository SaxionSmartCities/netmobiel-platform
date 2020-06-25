package eu.netmobiel.planner.service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ejb.EJBAccessException;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.NotFoundException;
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
import eu.netmobiel.planner.model.PlanType;
import eu.netmobiel.planner.model.PlannerReport;
import eu.netmobiel.planner.model.PlannerResult;
import eu.netmobiel.planner.model.RideshareResult;
import eu.netmobiel.planner.model.Stop;
import eu.netmobiel.planner.model.ToolType;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.model.User;
import eu.netmobiel.planner.repository.OpenTripPlannerDao;
import eu.netmobiel.planner.repository.OtpClusterDao;
import eu.netmobiel.planner.repository.TripPlanDao;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.service.RideManager;

@Stateless
@Logging
public class PlannerManager {
	public static final Integer MAX_RESULTS = 10; 
	private static final float PASSENGER_RELATIVE_MAX_DETOUR = 1.0f;
	private static final Integer CAR_TO_TRANSIT_SLACK = 10 * 60; // [seconds]
	private static final int MAX_RIDESHARES = 5;	
	private static final boolean RIDESHARE_LENIENT_SEARCH = true;	
	private static final int RIDESHARE_MAX_SLACK_BACKWARD = 6;	// hours
	private static final int RIDESHARE_MAX_SLACK_FORWARD = 6;	// hours
	private static final int DEFAULT_MAX_WALK_DISTANCE = 1000;
	@Inject
    private Logger log;

    @Inject
    private TripPlanDao tripPlanDao;
    @Inject
    private RideManager rideManager;
    @Inject
    private OpenTripPlannerDao otpDao;
    @Inject
    private OtpClusterDao otpClusterDao;
    
    @Inject
    private Event<TripPlan> shoutOutRequestedEvent;
    
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
	 * @param latestArrival The latest possible arrival time. If not set then ir will be set to
	 * 						 RIDESHARE_MAX_SLACK_FORWARD hours after earliest departure time.
	 * @param maxWalkDistance The maximum distance the passenger is prepared to walk
	 * @param nrSeats The number of seates required
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
    			log.debug("searchRides option: " + ride.toString());
    		}
    		// Use always the riders departure time. The itineraries will be scored later on.
        	GeoLocation from = ride.getFrom();
        	GeoLocation to = ride.getTo();
        	Set<TraverseMode> modes = new HashSet<>(Arrays.asList(new TraverseMode[] { TraverseMode.WALK, TraverseMode.CAR }));
        	// Calculate for each ride found the ride when the passenger would ride along, i.e., add the pickup and drop-off location
        	// as intermediate places to the OTP planner and calculate the itinerary.
    		Instant rideDepTime = ride.getDepartureTime();
        	PlannerResult planResult = otpDao.createPlan(now, from, to, rideDepTime,  false, modes, false, maxWalkDistance, null, intermediatePlaces, 1);
        	results.add(planResult);
    		if (planResult.hasError()) {
        		log.warn("Skip itinerary (RS) due to OTP error: " + planResult.getReport().shortReport());
    		} else {
        		Itinerary itinerary = planResult.getItineraries().get(0);
        		boolean accepted = Stream.of(new DetourMetersAcceptable(ride, planResult.getReport()), new DetourSecondsAcceptable(ride, planResult.getReport()))
        				.reduce(x -> true, Predicate::and)
        				.test(itinerary);
            	if (accepted) {
    	        	// We have the plan for the driver now. Add the itineraries but keep only the intermediate leg(s).
            		itinerary = itinerary.filterLegs(fromPlace, toPlace);
            		itinerary.getLegs().forEach(leg -> {
            			leg.setAgencyName(RideManager.AGENCY_NAME);
            			leg.setAgencyId(RideManager.AGENCY_ID);
            			leg.setDriverId(ride.getDriverRef());
            			leg.setDriverName(ride.getDriver().getName());
            			leg.setVehicleId(ride.getCarRef());
            			leg.setVehicleLicensePlate(ride.getCar().getLicensePlate());
            			leg.setVehicleName(ride.getCar().getName());
            			leg.setTraverseMode(TraverseMode.RIDESHARE);
            			// For Rideshare booking is always required.
            			leg.setBookingRequired(true);
            			leg.setTripId(ride.getRideRef());
            		});
            		// Set the arrival time of the first stop to null
            		// Set the departure time of the last stop to null
            		Leg firstLeg = itinerary.getLegs().get(0);
            		firstLeg.getFrom().setArrivalTime(null);
            		Leg lastLeg = itinerary.getLegs().get(itinerary.getLegs().size() - 1);  
            		lastLeg.getTo().setDepartureTime(null);
            		planResult.addItineraries(Collections.singletonList(itinerary));
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
        			plan.addItineraries(dit.appendTransits(transitResult.getItineraries()));
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
        			plan.addItineraries(dit.prependTransits(transitResult.getItineraries()));
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
	    	plan.getItineraries().addAll(passengerItineraries);

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

    	rankItineraries(plan);
    	plan.getItineraries().sort(new Comparator<Itinerary>() {
			@Override
			public int compare(Itinerary it1, Itinerary it2) {
				return -Double.compare(it1.getScore(), it2.getScore());
			}
		});
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
    	}
    	if (plan.getEarliestDepartureTime() == null) {
        	if (plan.getLatestArrivalTime() == null) {
        		plan.setEarliestDepartureTime(now);
        	} else if (now.isAfter(plan.getLatestArrivalTime())) { 
        		throw new BadRequestException("Latest arrival time must be after now: " + formatDateTime(now));
        	} else {
        		plan.setEarliestDepartureTime(plan.getTravelTime().minusSeconds(RIDESHARE_MAX_SLACK_BACKWARD * 60 * 60));
    			if (plan.getEarliestDepartureTime().isBefore(now)) {
            		plan.setEarliestDepartureTime(now);
    			}
    		}
    	} 
    	assert(plan.getEarliestDepartureTime() != null);
    	if (plan.getLatestArrivalTime() == null) {
    		plan.setLatestArrivalTime(plan.getEarliestDepartureTime().plusSeconds(RIDESHARE_MAX_SLACK_FORWARD * 60 * 60));
    	}
    	assert(plan.getLatestArrivalTime() != null);
    	if (plan.getTravelTime().isBefore(plan.getEarliestDepartureTime())) {
    		throw new BadRequestException("Earliest departure time must be before travel time: " + formatDateTime(plan.getTravelTime()));
    	}
    	if (plan.getTravelTime().isAfter(plan.getLatestArrivalTime())) {
    		throw new BadRequestException("Latest arrival time must be after travel time: " + formatDateTime(plan.getTravelTime()));
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
    	if (plan.getNrSeats() == null) {
    		plan.setNrSeats(1);
    	}
    	if (plan.getTraverseModes() == null || plan.getTraverseModes().isEmpty()) {
    		plan.setTraverseModes(new HashSet<>(Arrays.asList(new TraverseMode[] { TraverseMode.WALK, TraverseMode.RIDESHARE, TraverseMode.TRANSIT })));
    	}
    	if (! plan.getTraverseModes().contains(TraverseMode.RIDESHARE)) {
    		plan.setFirstLegRideshareAllowed(false);
    		plan.setLastLegRideshareAllowed(false);
    	}
    }

    public TripPlan createAndReturnTripPlan(User traveller, TripPlan plan, Instant now) throws NotFoundException, BadRequestException {
    	plan.setTraveller(traveller);
    	plan.setRequestTime(now);
    	sanitizePlanInput(plan);
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("searchMultiModal:\n Now %s %s D %s A %s from %s to %s; seats #%d, max walk distance %sm; modalities %s; maxTransfers %s; first/lastLegRS %s/%s",
    						formatDateTime(plan.getRequestTime()),
    						plan.getPlanType().toString(),
    						plan.isUseAsArrivalTime() ? "A" : "D",
    						plan.getTravelTime(), 
    						plan.getFrom().toString(), 
    						plan.getTo().toString(),
    						plan.getNrSeats() != null ? plan.getNrSeats() : 1, 
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
        	plan.setRequestDuration(Instant.now().toEpochMilli() - plan.getRequestTime().toEpochMilli());
       	}
       	tripPlanDao.save(plan);
       	if (plan.getPlanType() == PlanType.SHOUT_OUT) {
       		shoutOutRequestedEvent.fire(plan);
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
     * @throws NotFoundException In case one of the referenced object cannot be found.
     * @throws BadRequestException In case of bad parameters.
     */
    public Long createTripPlan(User traveller, TripPlan plan, Instant now) throws NotFoundException, BadRequestException {
    	return createAndReturnTripPlan(traveller, plan, now).getId();
    }

    /**
     * Closes a trip plan, i.e., the plan is no longer receivibng itineraries in case of a shout-out. 
     * @param id the id of the trip plan
     * @throws NotFoundException In case of an invalid trip plan ID.
     */
    public void closeTripPlan(Long id) throws NotFoundException {
    	TripPlan plandb = tripPlanDao.find(id)
    			.orElseThrow(() -> new NotFoundException("No such trip plan: " + id));
    	plandb.setRequestDuration(Instant.now().toEpochMilli() - plandb.getRequestTime().toEpochMilli());
    }
    

    /**
     * Retrieves a trip plan. All available details are retrieved.
     * @param id the id of the trip plan
     * @return The TripPlan object
     * @throws NotFoundException In case of an invalid trip plan ID.
     */
    public TripPlan getTripPlan(Long id) throws NotFoundException {
    	TripPlan plandb = tripPlanDao.fetchGraph(id, TripPlan.DETAILED_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such trip plan: " + id));
    	return plandb;
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
    			results = tripPlanDao.fetch(tripIds.getData(), null, TripPlan::getId);
    		}
    	}
    	return new PagedResult<TripPlan>(results, maxResults, offset, totalCount);
    }

}
