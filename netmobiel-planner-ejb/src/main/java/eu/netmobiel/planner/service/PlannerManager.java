package eu.netmobiel.planner.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import eu.netmobiel.rideshare.model.RideTemplate;
import eu.netmobiel.rideshare.service.RideManager;

@Stateless
@Logging
public class PlannerManager {
	private static final float PASSENGER_RELATIVE_MAX_DETOUR = 1.0f;
	private static final Integer CAR_TO_TRANSIT_SLACK = 10 * 60; // [seconds]
	private static final int MAX_RIDESHARES = 5;	
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
    
    private List<GeoLocation> filterIntermediatePlaces(Ride ride, GeoLocation[] places, int distanceInMeters) {
    	double distanceInKm = distanceInMeters / 1000.0;
    	GeoLocation from = ride.getRideTemplate().getFromPlace().getLocation();
    	GeoLocation to = ride.getRideTemplate().getToPlace().getLocation();
    	return Arrays.stream(places)
				.filter(loc -> loc.getDistanceFlat(from) > distanceInKm && loc.getDistanceFlat(to) > distanceInKm)
			.collect(Collectors.toList());
    }
    
    private String formatDateTime(Instant instant) {
    	return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(instant.atZone(ZoneId.systemDefault()).toLocalDateTime());
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
    
    public List<Itinerary> searchRideshareOnly(GeoLocation fromPlace, GeoLocation toPlace, 
    		Instant fromDate, Instant toDate, Integer maxWalkDistance, Integer nrSeats) {
    	// Try to find a ride from passenger departure to passenger destination
    	// Each possibility is an itinerary
    	// For CAR we assume (as approximation) the travel time is independent of the departure time. This is correct for OTP. 
    	// Only more advanced planners include the congestion dimension.
    	// We need to make a spatiotemporal selection from the possible ride. The spatial dimension is covered by the ellipse estimator.
    	// For the temporal dimension we have to set an eligibility interval: A difficult issue as we do not know the passengers 
    	// intentions and concerns. 
    	// A few rules then:
    	// - The departure time of the driver is beyond now. No, omit this one for now, we can search in the past if we wish to. 
    	// - The departure time is on the same day as fromDate or toDate
    	// 
    	LocalDateTime startldt = (fromDate != null ? fromDate : toDate).atZone(ZoneId.systemDefault()).toLocalDateTime();
    	LocalDateTime endldt = (fromDate != null ? fromDate : toDate).atZone(ZoneId.systemDefault()).toLocalDateTime();
    	
    	LocalDateTime startOfDay = startldt.toLocalDate().atStartOfDay();
    	LocalDateTime endOfDay = endldt.toLocalDate().atStartOfDay().plusDays(1);
    	PagedResult<Ride> rides = rideManager.search(fromPlace, toPlace,  startOfDay, endOfDay, nrSeats, MAX_RIDESHARES, 0);
    	// These are potential candidates. Now try to determine the complete route, including the intermediate places for pickup and dropoff
    	// The passenger is only involved in some (one) of the legs: the pickup or the drop-off. We assume the car is for the first or last mile.
    	// What if the pickup point and the driver's departure point are the same? A test revealed that we get an error TOO_CLOSE. Silly.
    	// So... Add intermediatePlaces only if far enough away. How far? More than 10 meter, let's take 20 meter.
    	
    	List<Itinerary> itineraries = new ArrayList<>();
    	for (Ride ride : rides.getData()) {
    		if (log.isDebugEnabled()) {
    			log.debug("Ride option: " + ride.toString());
    		}
    		RideTemplate ride_t = ride.getRideTemplate(); 
    		// Only accept intermediateLocations that are far enough away from departure and arrival of rider. 
    		List<GeoLocation> intermediatePlaces = filterIntermediatePlaces(ride, new GeoLocation[] { fromPlace, toPlace }, 20);
    		// Use always the riders departure time. The itineraries will be scored later on.
        	GeoLocation from = ride_t.getFromPlace().getLocation();
        	GeoLocation to = ride_t.getToPlace().getLocation();
        	TraverseMode[] modes = new TraverseMode[] { TraverseMode.WALK, TraverseMode.CAR };
        	TripPlan ridePlan = null;
        	try {
        		Instant rideDepTime = ride.getDepartureTime().atZone(ZoneId.systemDefault()).toInstant();
            	ridePlan = otpDao.createPlan(from, to, rideDepTime,  null, modes, false, maxWalkDistance, intermediatePlaces, 1);
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
        		boolean accepted = false;
            	if (log.isDebugEnabled()) {
            		log.debug("Ride plan: \n" + ridePlan.toString());
            	}
        		if (ride_t.getMaxDetourMeters() != null) {
        			// Determine new distance
        			double distance = ridePlan.getItineraries().get(0).getLegs().stream().mapToDouble(leg -> leg.getDistance()).sum();
        			int detour = (int)Math.round(distance - ride_t.getEstimatedDistance());
//        			log.debug(String.format("Ride %d detour %d meter",  ride.getId(), detour));
        			if (detour > ride_t.getMaxDetourMeters()) {
        				log.debug(String.format("Reject ride %d, detour is exceeded by %d meters (%d%%)", ride.getId(), 
        						detour - ride_t.getMaxDetourMeters(), (detour * 100) / ride_t.getMaxDetourMeters()));
        			} else {
        				accepted = true;
        			}
        		}
        		if (ride_t.getMaxDetourSeconds() != null) {
        			double duration = ridePlan.getItineraries().get(0).getLegs().stream().mapToDouble(leg -> leg.getDuration()).sum();
        			int detour = (int)Math.round(duration - ride_t.getEstimatedDrivingTime());
        			log.debug(String.format("Ride %d exceeeded detour %s seconds",  ride.getId(), detour));
        			if (detour > ride_t.getMaxDetourSeconds()) {
        				log.debug(String.format("Reject ride %d, detour is exceeded by %d seconds (%d%%)", ride.getId(), 
        						detour - ride_t.getMaxDetourSeconds(), (detour * 100) / ride_t.getMaxDetourSeconds()));
        			} else {
        				accepted = true;
        			}
        		}
            	if (accepted) {
    	        	// We have the plan for the driver now. Add the itineraries but keep only the intermediate leg(s).
            		Itinerary passengerRideIt = ridePlan.getItineraries().get(0);
            		passengerRideIt = passengerRideIt.filterLegs(fromPlace, toPlace);
            		passengerRideIt.getLegs().forEach(leg -> {
            			leg.setAgencyName(RideManager.AGENCY_NAME);
            			leg.setAgencyId(RideManager.AGENCY_ID);
            			leg.setDriverId(ride_t.getDriverRef());
            			leg.setDriverName(ride_t.getDriver().getName());
            			leg.setVehicleId(ride_t.getCarRef());
            			leg.setVehicleLicensePlate(ride_t.getCar().getLicensePlate());
            			leg.setVehicleName(ride_t.getCar().getName());
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
    	// Let's force the use of an arrival date
    	if (arrTime == null) {
    		throw new IllegalArgumentException("Only a search with an arrival time is supported now");
    	}

    	// Get a reference route for the passenger
    	TripPlan thePlan = null;
		try {
			thePlan = otpDao.createPlan(fromPlace, toPlace, depTime,  arrTime, 
					new TraverseMode[] { TraverseMode.WALK, TraverseMode.TRANSIT }, false, maxWalkDistance, null, null);
		} catch (NotFoundException e) {
			log.warn(String.format("No reference plan found from %s to %s - %s", fromPlace, toPlace, e.getMessage()));
			thePlan = new TripPlan();
			thePlan.setFrom(fromPlace);
			thePlan.setTo(toPlace);
		} catch (BadRequestException e) {
			throw new SystemException("No reference plan found", e);
		}
		thePlan.setArrivalTime(arrTime);
		thePlan.setDepartureTime(depTime);
		thePlan.setTraverseModes(modalities);
		thePlan.setNrSeats(nrSeats);
		thePlan.setMaxWalkDistance(maxWalkDistance);

    	if (log.isDebugEnabled()) {
    		log.debug("Reference plan: \n" + thePlan.toString());
    	}
    	Set<Stop> places = findTransitBoardingStops(thePlan);
    	if (log.isDebugEnabled()) {
    		log.debug("Onboarding places from reference plan: \n\t" + places.stream().map(p -> p.toString()).collect(Collectors.joining("\n\t")));
    	}
    	if (places.size() < 6) {
	    	// Perhaps we should also query for the 5 nearest hubs in case the number of places is small (just one)
	    	List<OtpCluster> clusters = searchImportantTransitStops(fromPlace, toPlace, 10);
	    	places = combineClustersIntoStops(places, clusters);
	    	if (log.isDebugEnabled()) {
	    		log.debug("Onboarding places extra: \n\t" + places.stream().map(p -> p.toString()).collect(Collectors.joining("\n\t")));
	    	}
    	}

    	// Extend the transit plan with CAR (ride sharing) modality and combinations of the ride and transit itineraries
    	// Add all direct rides
    	thePlan.getItineraries().addAll(searchRideshareOnly(fromPlace, toPlace, depTime, arrTime, maxWalkDistance, nrSeats));

    	// Try to find a ride from pickup point to each transit place (first mile by car)
    	log.debug("Search for first leg by Car");
    	for (Stop place : places) {
    		// Try to find a shared ride from passenger's departure to a transit hub
        	List<Itinerary> passengerCarItineraries = searchRideshareOnly(fromPlace, place.getLocation(), depTime, arrTime, maxWalkDistance, nrSeats);
    		// Create a transit plan from shared ride dropoff to passenger's destination
    		// Add x minutes waiting time at drop off
        	// Extract the leg for the passenger and create a complete itinerary for the passenger
        	for (Itinerary dit : passengerCarItineraries) {
            	Instant transitStart  = dit.getLegs().get(0).getEndTime().plusSeconds(CAR_TO_TRANSIT_SLACK);
        		TraverseMode[] modes = new TraverseMode[] { TraverseMode.WALK, TraverseMode.TRANSIT };
            	try {
            		TripPlan transitPlan = otpDao.createPlan(place.getLocation(), toPlace, transitStart,  null, modes, 
		        			false, maxWalkDistance, null, 2 /* To see repeating */);
		        	if (log.isDebugEnabled()) {
		        		log.debug("Car -> Transit plan: \n" + transitPlan.toString());
		        	}
		        	thePlan.getItineraries().addAll(dit.appendTransits(transitPlan.getItineraries()));
            	}  catch(Exception ex) {
            		log.error(ex.toString());
            		log.warn("Skip itinerary due to OTP error: " + dumpPlanRequest(transitStart, null, place.getLocation(), toPlace, null, modes));
            	}
			}
		}

    	// Try to find a ride from transit place to drop-off (last mile by car)
    	log.debug("Search for a last leg by Car");
    	for (Stop place : places) {
    		// Try to find a shared ride from transit hub to passenger's destination
        	List<Itinerary> passengerCarItineraries = searchRideshareOnly(place.getLocation(), toPlace, depTime, arrTime, maxWalkDistance, nrSeats);
    		// Create a transit plan from passenger departure to shared ride pickup
    		// Add x minutes waiting time at pick off
        	// Extract the leg for the passenger and create a complete itinerary for the passenger
        	for (Itinerary dit : passengerCarItineraries) {
        		Instant transitEnd  = dit.getLegs().get(0).getEndTime().plusSeconds(CAR_TO_TRANSIT_SLACK);
        		TraverseMode[] modes = new TraverseMode[] { TraverseMode.WALK, TraverseMode.TRANSIT };
            	try {
	            	TripPlan transitPlan = otpDao.createPlan(fromPlace, place.getLocation(), null,  transitEnd, modes,  
	            			false, maxWalkDistance, null, 2 /* To see repeating */);
	            	if (log.isDebugEnabled()) {
	            		log.debug("Transit -> Car plan: \n" + transitPlan.toString());
	            	}
	            	thePlan.getItineraries().addAll(dit.prependTransits(transitPlan.getItineraries()));
            	}  catch(Exception ex) {
            		log.error(ex.toString());
            		log.warn("Skip itinerary due to OTP error: " + dumpPlanRequest(null, transitEnd, fromPlace, place.getLocation(), null, modes));
            	}
			}
		}

    	rankItineraries(thePlan, depTime, arrTime);
    	thePlan.getItineraries().sort(new Comparator<Itinerary>() {
			@Override
			public int compare(Itinerary it1, Itinerary it2) {
				return -Double.compare(it1.score, it2.score);
			}
		});
    	return thePlan;
    }

    public void rankItineraries(TripPlan thePlan, Instant fromDate, Instant toDate) {
    	BasicItineraryRankingAlgorithm ranker = new BasicItineraryRankingAlgorithm();
    	for (Itinerary it: thePlan.getItineraries()) {
    		ranker.calculateScore(it, fromDate, toDate);
    	}
    }
    
    public Set<Stop> findTransitBoardingStops(TripPlan otherPlan) {
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
    
    public List<OtpCluster> searchImportantTransitStops(GeoLocation fromPlace, GeoLocation toPlace, int maxResults) {
    	EligibleArea ea = EllipseHelper.calculateEllipse(fromPlace.getPoint(), toPlace.getPoint(), null, PASSENGER_RELATIVE_MAX_DETOUR / 2);
//    	log.debug("Passenger ellipse: " + GeometryHelper.createWKT(ea.eligibleAreaGeometry));
    	// Find all hub-alike transit clusters inside this ellipse
    	return otpClusterDao.findImportantHubs(fromPlace, toPlace, ea.eligibleAreaGeometry, maxResults);
    }

	public Set<Stop> combineClustersIntoStops(Collection<Stop> otherStops, Collection<OtpCluster> clusters) {
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
