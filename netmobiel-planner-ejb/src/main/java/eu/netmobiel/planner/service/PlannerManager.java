package eu.netmobiel.planner.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.SystemException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.EllipseHelper;
import eu.netmobiel.commons.util.EllipseHelper.EligibleArea;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.planner.model.Itinerary;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.OtpCluster;
import eu.netmobiel.planner.model.Stop;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.repository.OpenTripPlannerDao;
import eu.netmobiel.planner.repository.OtpClusterDao;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.service.RideManager;

@Stateless
@Logging
public class PlannerManager {
	private static final float PASSENGER_RELATIVE_MAX_DETOUR = 1.0f;
	private static final Integer CAR_TO_TRANSIT_SLACK = 10 * 60; // [seconds]
	private static final int MAX_RIDESHARES = 5;	
	private static final boolean RIDESHARE_LENIENT_SEARCH = true;	

	@Inject
    private Logger log;

    @Inject
    private RideManager rideManager;
    @Inject
    private OpenTripPlannerDao otpDao;
    @Inject
    private OtpClusterDao otpClusterDao;
    
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
    	return DateTimeFormatter.ISO_TIME.format(instant.atOffset(ZoneOffset.UTC));
    }
    
    private String dumpPlanRequest(Instant fromDate, Instant toDate, GeoLocation fromPlace, GeoLocation toPlace, List<GeoLocation> intermediatePlaces, TraverseMode[] modes) {
		StringBuilder sb = new StringBuilder();
		sb.append("D ").append(fromDate != null ? formatDateTime(fromDate) : "-");
		sb.append(" A ").append(toDate != null ? formatDateTime(toDate) : "-");
		sb.append(" ").append(fromPlace.toString());
		sb.append(" --> ").append(toPlace.toString());
		if (intermediatePlaces != null) {
			sb.append(" Via: ").append(intermediatePlaces.stream().map(p -> p.toString()).collect(Collectors.joining(" ")));
		}
		sb.append(" By: ").append(Arrays.stream(modes).map(m -> m.name()).collect(Collectors.joining(", ")));
		return sb.toString();
    }
    
    /**
     * Filter for acceptable itineraries, testing on max detour in meters.
     */
    private class DetourMetersAcceptable implements Predicate<Itinerary> {
    	private Ride ride;

    	public DetourMetersAcceptable(Ride aRide) {
    		this.ride = aRide;
    	}
    	
		@Override
		public boolean test(Itinerary it) {
	       	boolean accepted = true;
	    	Integer maxDetour = ride.getMaxDetourMeters();
			if (maxDetour != null && maxDetour > 0) {
				// Determine new distance
				double distance = it.getLegs().stream().mapToDouble(leg -> leg.getDistance()).sum();
				int detour = (int)Math.round(distance - ride.getDistance());
//	    			log.debug(String.format("Ride %d detour %d meter",  ride.getId(), detour));
				if (detour > ride.getMaxDetourMeters()) {
					log.debug(String.format("Reject ride %d, detour is exceeded by %d meters (%d%%)", ride.getId(), 
							detour - maxDetour, (detour * 100) / maxDetour));
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

    	public DetourSecondsAcceptable(Ride aRide) {
    		this.ride = aRide;
    	}
    	
		@Override
		public boolean test(Itinerary it) {
	       	boolean accepted = true;
	    	Integer maxDetour = ride.getMaxDetourSeconds();
			if (maxDetour != null && maxDetour > 0) {
				// Determine new distance
				double duration = it.getLegs().stream().mapToDouble(leg -> leg.getDuration()).sum();
				int detour = (int)Math.round(duration - ride.getDuration());
				log.debug(String.format("Ride %d exceeeded detour %s seconds",  ride.getId(), detour));
				if (detour > maxDetour) {
					log.debug(String.format("Reject ride %d, detour is exceeded by %d seconds (%d%%)", ride.getId(), 
							detour - maxDetour, (detour * 100) / maxDetour));
					accepted = false;
				}
			}
			return accepted;
		}
    }

	/**
	 * Try to find a ride from passenger departure to passenger destination. Each
	 * possibility is an itinerary. For traverse mode CAR we assume (as approximation) the travel
	 * time is independent of the departure time. This is correct for OTP. Only more
	 * advanced planners include the congestion dimension. We need to make a
	 * spatiotemporal selection from the possible rides. The spatial dimension is
	 * covered by the ellipse estimator. For the temporal dimension we have to set
	 * an eligibility interval: A difficult issue as we do not know the passengers
	 * intentions and concerns. A few rules then: 
	 * - The departure time of the driver is beyond now. No, omit this one for now, 
	 * 	 we can search in the past if we wish to. 
	 * - The departure time is on the same day as fromDate or toDate.
	 * 
	 * @param fromPlace The departure location of the passenger 
	 * @param toPlace The arrival location of the passenger
	 * @param fromDate ?
	 * @param toDate ?
	 * @param maxWalkDistance The maximum distance the passenger is prepared to walk
	 * @param nrSeats The number of seates required
	 * @return A list of possible itineraries.
	 */
    protected List<Itinerary> searchRideshareOnly(GeoLocation fromPlace, GeoLocation toPlace, 
    		Instant fromDate, Instant toDate, Integer maxWalkDistance, Integer nrSeats) {
    	Instant earliestDeparture = fromDate;
    	Instant latestArrival = toDate;
    	if (earliestDeparture == null && latestArrival == null) {
    		throw new IllegalArgumentException("Set fromDate and/or toDate");
    	}
    	if (earliestDeparture == null) {
    		earliestDeparture  = latestArrival.minusSeconds(24 * 60 * 60);	// One day
    	} else if (latestArrival == null) {
    		latestArrival = earliestDeparture.plusSeconds(24 * 60 * 60);
    	}
    	PagedResult<Ride> rides = rideManager.search(fromPlace, toPlace,  earliestDeparture, latestArrival, nrSeats, RIDESHARE_LENIENT_SEARCH, MAX_RIDESHARES, 0);
    	// These are potential candidates. Now try to determine the complete route, including the intermediate places for pickup and dropoff
    	// The passenger is only involved in some (one) of the legs: the pickup or the drop-off. We assume the car is for the first or last mile of the passenger.
    	// What if the pickup point and the driver's departure point are the same? A test revealed that we get an error TOO_CLOSE. Silly.
    	// A minimum distance filter is added in the OTP client. We don't care here, but do not make expectations about the number of legs 
    	// in the itinerary.

		List<GeoLocation> intermediatePlaces = Arrays.asList(new GeoLocation[] { fromPlace, toPlace });
    	List<Itinerary> itineraries = new ArrayList<>();
    	for (Ride ride : rides.getData()) {
    		if (log.isDebugEnabled()) {
    			log.debug("searchRides option: " + ride.toString());
    		}
    		// Use always the riders departure time. The itineraries will be scored later on.
        	GeoLocation from = ride.getFrom();
        	GeoLocation to = ride.getTo();
        	TraverseMode[] modes = new TraverseMode[] { TraverseMode.WALK, TraverseMode.CAR };
        	TripPlan ridePlan = null;
        	// Calculate for each ride found the ride when the passenger would ride along, i.e., add the pickup and drop-off location
        	// as intermediate places to the OTP planner and calculate the itinerary.
        	try {
        		Instant rideDepTime = ride.getDepartureTime();
            	ridePlan = otpDao.createPlan(from, to, rideDepTime,  false, modes, false, maxWalkDistance, intermediatePlaces, 1);
            	ridePlan.setDepartureTime(fromDate);
            	ridePlan.setArrivalTime(toDate);
            	ridePlan.setMaxWalkDistance(maxWalkDistance);
            	ridePlan.setNrSeats(nrSeats);
            	ridePlan.setTraverseModes(modes);
        	}  catch(Exception ex) {
        		log.error(ex.toString());
        		log.warn("Skip itinerary due to OTP error: " + 
        				dumpPlanRequest(fromDate, toDate, from, to, intermediatePlaces, modes));
        	}
        	if (ridePlan != null && ridePlan.getItineraries() != null && !ridePlan.getItineraries().isEmpty()) {
            	if (log.isDebugEnabled()) {
            		log.debug("Ride plan: \n" + ridePlan.toString());
            	}
        		Itinerary itinerary = ridePlan.getItineraries().get(0);
        		boolean accepted = Stream.of(new DetourMetersAcceptable(ride), new DetourSecondsAcceptable(ride))
        				.reduce(x -> true, Predicate::and)
        				.test(itinerary);
            	if (accepted) {
    	        	// We have the plan for the driver now. Add the itineraries but keep only the intermediate leg(s).
            		Itinerary passengerRideIt = ridePlan.getItineraries().get(0);
            		passengerRideIt = passengerRideIt.filterLegs(fromPlace, toPlace);
            		passengerRideIt.getLegs().forEach(leg -> {
            			leg.setAgencyName(RideManager.AGENCY_NAME);
            			leg.setAgencyId(RideManager.AGENCY_ID);
            			leg.setDriverId(ride.getDriverRef());
            			leg.setDriverName(ride.getDriver().getName());
            			leg.setVehicleId(ride.getCarRef());
            			leg.setVehicleLicensePlate(ride.getCar().getLicensePlate());
            			leg.setVehicleName(ride.getCar().getName());
            			leg.setTraverseMode(TraverseMode.RIDESHARE);
            			leg.setTripId(ride.getRideRef());
            		});
    	        	itineraries.add(passengerRideIt);
            	}
        	}
		}
    	// These are itineraries for the passenger, not the complete ones for the driver
    	return itineraries;
    }

    
    protected void addRideshareAsFirstLeg(TripPlan plan, Set<Stop> transitBoardingStops, TraverseMode[] transitModalities) {
    	log.debug("Search for first leg by Car");
    	for (Stop place : transitBoardingStops) {
    		// Try to find a shared ride from passenger's departure to a transit hub
        	List<Itinerary> passengerCarItineraries = searchRideshareOnly(plan.getFrom(), place.getLocation(), 
        			plan.getDepartureTime(), plan.getArrivalTime(), plan.getMaxWalkDistance(), plan.getNrSeats());
    		// Create a transit plan from shared ride dropoff to passenger's destination
    		// Add x minutes waiting time at drop off
        	// Extract the leg for the passenger and create a complete itinerary for the passenger
        	for (Itinerary dit : passengerCarItineraries) {
            	Instant transitStart  = dit.getLegs().get(0).getEndTime().plusSeconds(CAR_TO_TRANSIT_SLACK);
            	try {
            		TripPlan transitPlan = otpDao.createPlan(place.getLocation(), plan.getTo(), transitStart,  false, transitModalities, 
		        			false, plan.getMaxWalkDistance(), null, 2 /* To see repeating */);
		        	if (log.isDebugEnabled()) {
		        		log.debug("Car -> Transit plan: \n" + transitPlan.toString());
		        	}
		        	plan.getItineraries().addAll(dit.appendTransits(transitPlan.getItineraries()));
            	}  catch(Exception ex) {
            		log.error(ex.toString());
            		log.warn("Skip itinerary due to OTP error: " + dumpPlanRequest(transitStart, null, place.getLocation(), plan.getTo(), null, transitModalities));
            	}
			}
		}
    }

    protected void addRideshareAsLastLeg(TripPlan plan, Set<Stop> transitAlightingStops, TraverseMode[] transitModalities) {
    	// Try to find a ride from transit place to drop-off (last mile by car)
    	log.debug("Search for a last leg by Car");
    	//FIXME Should be in fact alighting stops 
    	for (Stop place : transitAlightingStops) {
    		// Try to find a shared ride from transit hub to passenger's destination
        	List<Itinerary> passengerCarItineraries = searchRideshareOnly(place.getLocation(), plan.getTo(), 
        		plan.getDepartureTime(), plan.getArrivalTime(), plan.getMaxWalkDistance(), plan.getNrSeats());
    		// Create a transit plan from passenger departure to shared ride pickup
    		// Add x minutes waiting time at pick off
        	// Extract the leg for the passenger and create a complete itinerary for the passenger
        	for (Itinerary dit : passengerCarItineraries) {
        		Instant transitEnd  = dit.getLegs().get(0).getEndTime().plusSeconds(CAR_TO_TRANSIT_SLACK);
            	try {
	            	TripPlan transitPlan = otpDao.createPlan(plan.getFrom(), place.getLocation(), transitEnd, true, transitModalities,  
	            			false, plan.getMaxWalkDistance(), null, 2 /* To see repeating */);
	            	if (log.isDebugEnabled()) {
	            		log.debug("Transit -> Car plan: \n" + transitPlan.toString());
	            	}
	            	plan.getItineraries().addAll(dit.prependTransits(transitPlan.getItineraries()));
            	}  catch(Exception ex) {
            		log.error(ex.toString());
            		log.warn("Skip itinerary due to OTP error: " + dumpPlanRequest(null, transitEnd, plan.getFrom(), place.getLocation(), null, transitModalities));
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
    	return stops;
    }

    protected TripPlan createTransitPlan(TripPlan plan, TraverseMode[] otpModalities, int maxWalkDistance) {
    	TripPlan transitPlan = null;
		try {
	    	boolean arrivalTimeIsPinned = false;
	    	Instant travelTime = plan.getDepartureTime();
	    	if (travelTime == null) {
	    		travelTime = plan.getArrivalTime();
	    		arrivalTimeIsPinned = true;
	    	}
			// Perhaps add WALK?
			transitPlan = otpDao.createPlan(plan.getFrom(), plan.getTo(), travelTime, arrivalTimeIsPinned, 
					otpModalities, false, maxWalkDistance, null, null);
	    	if (log.isDebugEnabled()) {
	    		log.debug("Transit plan: \n" + transitPlan.toString());
	    	}
		} catch (NotFoundException e) {
			log.warn(String.format("No transit plan found from %s to %s - %s", plan.getFrom(), plan.getTo(), e.getMessage()));
		} catch (BadRequestException e) {
			throw new SystemException("No transit plan could be created", e);
		}
		return transitPlan;
    }
    
    /**
     * Creates a multi-modal travel plan for a passenger. Both ride share and transit options are considered.
     * For now a shared ride is only an option as first or last leg. 
     * The passenger might be more flexible than the departure or arrival time. For that reason the itineraries for public transport
     * will be a good fit given departure or arrival time, but itineraries involving a shared ride vary much more in time.
     * An attempt is made to sort the itineraries by attractiveness to the passenger.   
     * @param fromPlace The departure point of the passenger
     * @param toPlace The destination of the passenger
     * @param depTime The (intended) departure time. Specify either departure or arrival time. 
     * @param arrTime The (intended) arrival time. Specify either departure or arrival time.
     * @param modalities The eligible modalities to travel with.
     * @param maxWalkDistance The maximum distance the passenger is prepared to walk.
     * @param nrSeats The number of seats the passenger wants to use in a car.
     * @return
     */
    public TripPlan searchMultiModal(GeoLocation fromPlace, GeoLocation toPlace, 
    		Instant depTime, Instant arrTime, 
    		TraverseMode[] modalities, Integer maxWalkDistance, Integer nrSeats) {
    	//FIXME For searching we need a earliestDeparture and latestArrival. For scoring we need to know a reference time and whether that is departure or arrival.
    	//FIXME Add also whether first mile or last mile mile (or both ) by rideshare are acceptable. MaxTransfer 1 could force it but that affects transit too.  
    	boolean allowRideshareForLastLeg = false;
    	boolean allowRideshareForFirstLeg = false;
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("searchMultiModal:\n D %s A %s from %s to %s; seats #%d, max walk distance %sm; modalities %s", 
    						depTime != null ? formatDateTime(depTime) : "*", 
    						arrTime != null ? formatDateTime(arrTime) : "*", 
    						fromPlace.toString(), 
    						toPlace.toString(),
    						nrSeats != null ? nrSeats : 1, 
    						maxWalkDistance != null ? maxWalkDistance.toString() : "?",
    						modalities != null ? Arrays.stream(modalities).map(tm -> tm.name()).collect(Collectors.joining(", ")) : "*"
    				)
    		);
    	}
    	// Create the basic trip plan
    	TripPlan thePlan = new TripPlan();
		thePlan.setFrom(fromPlace);
		thePlan.setTo(toPlace);
		thePlan.setArrivalTime(arrTime);
		thePlan.setDepartureTime(depTime);
		thePlan.setTraverseModes(modalities);
		thePlan.setNrSeats(nrSeats);
		thePlan.setMaxWalkDistance(maxWalkDistance);
		// 
		List<TraverseMode> transitModalities = Arrays.stream(modalities).filter(m -> m.isTransit()).collect(Collectors.toList());
		List<TraverseMode> otpModalitiesList = new ArrayList<>(transitModalities);
		// OTP needs WALK too
		otpModalitiesList.add(TraverseMode.WALK);
		TraverseMode[] otpModalities = otpModalitiesList.stream().toArray(TraverseMode[]::new);
		boolean rideshareEligable = Arrays.stream(modalities).filter(m -> m == TraverseMode.RIDESHARE).findFirst().isPresent();
		if (!transitModalities.isEmpty()) {
			// Transit is eligable. Add the transit itineraries as calculated by OTP.
			TripPlan transitPlan = createTransitPlan(thePlan, otpModalities, thePlan.getMaxWalkDistance());
			thePlan.getItineraries().addAll(transitPlan.getItineraries());
		}

		if (rideshareEligable) {
	    	// Add the RIDESHARE only itineraries
	    	thePlan.getItineraries().addAll(searchRideshareOnly(fromPlace, toPlace, depTime, arrTime, maxWalkDistance, nrSeats));

			// If transit is an option too then collect possible pickup and drop-off places near transit stops
	    	// FIXME Add maxNrTransers
			if (!transitModalities.isEmpty() && (allowRideshareForFirstLeg || allowRideshareForLastLeg) ) {
		    	TripPlan transitReferencePlan = null;
				Set<Stop> transitBoardingStops = null;
				// Calculate a transit reference plan to find potential boarding or alighting places. 
				transitReferencePlan = createTransitPlan(thePlan, otpModalities, 50000);
				List<OtpCluster> nearbyClusters = new ArrayList<>();
				// Collect the stops. If there are no stops or too few, collect potential clusters
				//FIXME The ordering of the clusters depends probably on first oflast leg. Check.
		    	transitBoardingStops = collectStops(thePlan, findTransitBoardingStops(transitReferencePlan), nearbyClusters);
	    		if (allowRideshareForFirstLeg) {
	        		addRideshareAsFirstLeg(thePlan, transitBoardingStops, otpModalities);
	    		}
	    		if (allowRideshareForLastLeg) {
	        		addRideshareAsLastLeg(thePlan, transitBoardingStops, otpModalities);
	    		}
			}
		}

    	rankItineraries(thePlan, depTime, arrTime);
    	thePlan.getItineraries().sort(new Comparator<Itinerary>() {
			@Override
			public int compare(Itinerary it1, Itinerary it2) {
				return -Double.compare(it1.getScore(), it2.getScore());
			}
		});
    	return thePlan;
    }

    protected void rankItineraries(TripPlan thePlan, Instant fromDate, Instant toDate) {
    	BasicItineraryRankingAlgorithm ranker = new BasicItineraryRankingAlgorithm();
    	for (Itinerary it: thePlan.getItineraries()) {
    		ranker.calculateScore(it, fromDate, toDate);
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
    
   	protected Set<Stop> findTransitBoardingStops(TripPlan otherPlan) {
   		if (otherPlan == null) {
   			return Collections.emptySet();
   		}
    	// Strategy A: Replace the first leg(s) with ride share options
    	Set<Stop> stops = new LinkedHashSet<>();
    	for (Itinerary it : otherPlan.getItineraries()) {
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
    
    protected List<OtpCluster> searchImportantTransitStops(GeoLocation fromPlace, GeoLocation toPlace, TraverseMode[] modes, int maxResults) {
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
}
