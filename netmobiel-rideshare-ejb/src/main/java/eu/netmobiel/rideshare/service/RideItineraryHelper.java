package eu.netmobiel.rideshare.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.util.ClosenessFilter;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.BookingState;
import eu.netmobiel.rideshare.model.Leg;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.Stop;
import eu.netmobiel.rideshare.repository.BookingDao;
import eu.netmobiel.rideshare.repository.LegDao;
import eu.netmobiel.rideshare.repository.OpenTripPlannerDao;
import eu.netmobiel.rideshare.repository.RideDao;
import eu.netmobiel.rideshare.repository.StopDao;

/**
 * Class used to keep the itinerary of a ride up-to-date with regard to ride and booking properties.
 * All methods are assumed to run in transaction context.
 * 
 * @author Jaap Reitsma
 *
 */
@ApplicationScoped
@Logging
public class RideItineraryHelper {
	private static final int MAX_BOOKING_LOCATION_SHIFT = 100;	// Maximum 100 meter deviation of original pickup/drop-off

	@Inject
    private Logger log;
	@Inject
    private RideDao rideDao;
    @Inject
    private BookingDao bookingDao;
    @Inject
    private LegDao legDao;
    @Inject
    private StopDao stopDao;
    @Inject
    private OpenTripPlannerDao otpDao;

    @Inject
    private Event<Booking> quoteRequestedEvent;

    /**
     * Saves a fresh new ride, including the legs and stops.
     * @param r
     * @throws BusinessException 
     */
    public void saveNewRide(Ride r) throws BusinessException {
    	rideDao.save(r);
    	r.getStops().forEach(stop -> stopDao.save(stop));
    	r.getLegs().forEach(leg -> legDao.save(leg));
    }

    /**
     * Calculates the itinerary from the input ride. The collection legs and stops are not used to manage the legs and stops.
     * Instead, the legs and and stops are managed from the many side. 
     * @param ride the input from the client. Must be persistence context. The legs and stops are NOT initialized!
     * @throws BadRequestException In case of bad parameters
     */
    public void updateRideItinerary(Ride ride) throws BadRequestException {
    	if (! rideDao.contains(ride)) {
    		throw new IllegalStateException("Ride should be in persistence context at this point");
    	}
    	// Create the route to drive and create the leg graph
    	List<Leg> newLegs = null;
    	try {
			newLegs = Arrays.asList(otpDao.createItinerary(ride));
	    	// Set the sequence of the legs
	    	AtomicInteger index = new AtomicInteger(0);
	    	newLegs.forEach(leg -> leg.setLegIx(index.getAndIncrement()));
		} catch (NotFoundException | BadRequestException e) {
			throw new BadRequestException("Cannot compute itinerary", e);
		}
    	// Update the ride leg structure with the results from the planner.
    	// We do not touch the collection side of legs and stops.
    	// FIXME Allow transfer time for pickup and drop-off of the passenger

    	
    	//TODO verify the order of the stops as retrieved. Does it follow the orderby column?
    	List<Stop> oldStops = stopDao.listStops(ride);
    	List<Leg> oldLegs = legDao.listLegs(ride);
    	// Get rid of the old booking references
    	ride.getLegs().forEach(leg -> leg.getBookings().clear());
    	ride.getStops().clear();
    	ride.getLegs().clear();
    	
    	// Remove all booking info from old legs
    	oldLegs.forEach(leg -> leg.getBookings().clear());
    	
    	// Get the stops of the graph: The first departure and then all arrivals
    	List<Stop> newStops = new ArrayList<>();
    	newStops.add(newLegs.get(0).getFrom());
    	newLegs.forEach(leg -> newStops.add(leg.getTo()));

    	// Replace the old stop structure. Old stops are in persistence context by now.
    	// What would be quicker? Just a cascading delete and reinsert legs and stops or 
    	// step by step update the structure?
    	// Update method: Merge stops, add new stops. 
    	//                Then update legs: update legs, add new legs. 
    	//				  Finally remove legs, remove stops 
    	// The sequence matters because of non-null constraints on leg.from and leg.to.
    	int i, oldStepIx, oldLegIx;
        for (i = 0; i < oldStops.size() && i < newStops.size(); i++) {
        	Stop oldStop = oldStops.get(i);
        	Stop newStop = newStops.get(i);
        	// Overwrite the old stop with the attributes from the new stop, first set the keys
        	newStop.setId(oldStop.getId());
    		ride.addStop(newStop);
    		stopDao.merge(newStops.get(i));
		}
        oldStepIx = i;
        // New list is longer than old list, add remaining stops
    	// Increase the length of the list
    	for (i = oldStops.size(); i < newStops.size(); i++) {
    		Stop newStop = newStops.get(i);
    		ride.addStop(newStop);
   			stopDao.save(newStop);
		}

    	// Replace the old leg structure. Old legs are in persistence context by now.
    	for (i = 0; i < oldLegs.size() && i < newLegs.size(); i++) {
        	// Overwrite the old leg with the attributes from the new leg, first set the keys
        	Leg oldLeg = oldLegs.get(i);
        	Leg newLeg = newLegs.get(i);
    		newLeg.setId(oldLeg.getId());
    		ride.addLeg(newLeg);
    		legDao.merge(newLeg);
		}
        oldLegIx = i;
    	for (i = oldLegs.size(); i < newLegs.size(); i++) {
    		Leg newLeg = newLegs.get(i);
    		ride.addLeg(newLeg);
   			legDao.save(newLeg);
		}

    	// Finally, remove obsoleted legs and stops (in that sequence)
    	for (i = oldLegIx; i < oldLegs.size(); i++) {
    		Leg oldLeg = oldLegs.get(i);
    		legDao.remove(oldLeg);
		}
        // Decrease the length of the list
    	for (i = oldStepIx; i < oldStops.size(); i++) {
    		Stop oldStop = oldStops.get(i);
    		stopDao.remove(oldStop);
		}
    	
    	// Compute the leg - booking relationship
    	updateBookedLegs(ride);

    	int distance = newLegs.stream().collect(Collectors.summingInt(Leg::getDistance));
    	ride.setDistance(distance);
    	// Old cars don't have the CO2 emission specification
    	if (ride.getCar().getCo2Emission() != null) {
    		ride.setCO2Emission(Math.toIntExact(Math.round(ride.getDistance() * ride.getCar().getCo2Emission() / 1000.0)));
    	}
    	// Set the correct departure/arrival time
    	if (ride.isArrivalTimePinned()) {
    		Stop departureStop = newLegs.get(0).getFrom();
    		ride.setDepartureTime(departureStop.getDepartureTime());
    	} else {
    		Stop arrivalStop = newLegs.get(newLegs.size() - 1).getTo();
    		ride.setArrivalTime(arrivalStop.getArrivalTime());
    	}
    	if (log.isDebugEnabled()) {
    		log.debug("Updated itinerary: " + ride.toString());
    	}
    }

    /**
     * Compute the leg - booking relationship. For each booking: Determine the first leg and the last leg, 
     * then add intermediate legs. In case of a single booking there is always just one leg.
     * @param ride the ride in question, must be in persistence context.
     */
    public void updateBookedLegs(Ride ride) {
    	ClosenessFilter closenessFilter = new ClosenessFilter(MAX_BOOKING_LOCATION_SHIFT);
    	List<Booking> bookings = bookingDao.findByRide(ride);
    	for (Booking booking : bookings) {
    		// Only a confirmed booking can be part, and a proposed too
    		if (booking.getState() != BookingState.CONFIRMED && booking.getState() != BookingState.PROPOSED) {
    			continue;
    		}
    		Leg pickup = ride.getLegs().stream()
    				.filter(leg -> closenessFilter.test(leg.getFrom().getLocation(), booking.getPickup()))
    				.findFirst()
    				.orElseThrow(() -> new IllegalStateException("Cannot find first leg for booking"));
    		Leg dropOff = ride.getLegs().stream()
    				.filter(leg -> closenessFilter.test(leg.getTo().getLocation(), booking.getDropOff()))
    				.findFirst()
    				.orElseThrow(() -> new IllegalStateException("Cannot find last leg for booking"));
    		// Get the start, the end and everything in between and add them to the booking
//    		log.info("Booking: " + booking.toString());
//    		log.info("Pickup leg: " + pickup.toString());
//    		log.info("Pickup leg index: " + ride.getLegs().indexOf(pickup));
//    		log.info("Drop-off leg: " + dropOff.toString());
//    		log.info("Drop-off leg index: " + ride.getLegs().indexOf(dropOff));
			ride.getLegs()
				.subList(ride.getLegs().indexOf(pickup), ride.getLegs().indexOf(dropOff) + 1)
				.forEach(leg -> leg.addBooking(booking));
//			ride.getLegs()
//			.forEach(leg -> log.info(String.format("Leg %s Bookings %s", leg.getId(), 
//					String.join(", ", leg.getBookings()
//							.stream()
//							.map(b -> b.getId().toString())
//							.collect(Collectors.toList())))));
		}
    }

    /**
     * Create a booking fromthe calculated ride legs. 
     * @param ride the calculated ride with the legs
     * @param pickup The requested location for pickup
     * @param dropOff The requested location for drop-off
     * 
     */
    public Booking createBooking(Ride ride, GeoLocation pickup, GeoLocation dropOff) {
    	ClosenessFilter closenessFilter = new ClosenessFilter(MAX_BOOKING_LOCATION_SHIFT);
   		// Get the start, the end and everything in between and add them to the booking
  		Leg pickupLeg = ride.getLegs().stream()
			.filter(leg -> closenessFilter.test(leg.getFrom().getLocation(), pickup))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("Cannot find first leg of passenger"));
   		Leg dropOffLeg = ride.getLegs().stream()
			.filter(leg -> closenessFilter.test(leg.getTo().getLocation(), dropOff))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("Cannot find last legof passenger"));
//		log.info("Pickup leg: " + pickup.toString());
//		log.info("Pickup leg index: " + ride.getLegs().indexOf(pickup));
//		log.info("Drop-off leg: " + dropOff.toString());
//		log.info("Drop-off leg index: " + ride.getLegs().indexOf(dropOff));
   		Booking b = new Booking();
   		b.setDepartureTime(pickupLeg.getStartTime());
   		b.setPickup(pickupLeg.getFrom().getLocation());
   		b.setArrivalTime(dropOffLeg.getEndTime());
   		b.setDropOff(dropOffLeg.getTo().getLocation());
   		b.setRide(ride);
   		b.setState(BookingState.NEW);
		ride.getLegs()
			.subList(ride.getLegs().indexOf(pickupLeg), ride.getLegs().indexOf(dropOffLeg) + 1)
			.forEach(leg -> leg.addBooking(b));
		// Now the booking has a reference to the legs 
		quoteRequestedEvent.fire(b);
//			ride.getLegs()
//			.forEach(leg -> log.info(String.format("Leg %s Bookings %s", leg.getId(), 
//					String.join(", ", leg.getBookings()
//							.stream()
//							.map(b -> b.getId().toString())
//							.collect(Collectors.toList())))));
		return b;
    }

}
