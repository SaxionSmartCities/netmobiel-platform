package eu.netmobiel.planner.service;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.ejb.EJB;
import javax.ejb.ObjectNotFoundException;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripState;
import eu.netmobiel.planner.model.User;
import eu.netmobiel.planner.repository.TripDao;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.Stop;
import eu.netmobiel.rideshare.service.BookingManager;

@Stateless
@Logging
public class TripManager {
	@SuppressWarnings("unused")
	@Inject
    private Logger log;

    @Inject
    private TripDao tripDao;
    
    @EJB(name = "java:app/netmobiel-planner-ejb/UserManager")
    private UserManager userManager;

    @Inject
    private BookingManager bookingManager;

    /**
     * List all trips owned by the specified user. Soft deleted trips are omitted.
     * @return A list of trips owned by the specified user.
     */
    public List<Trip> listTrips(User traveller, Instant since, Instant until, Boolean deletedToo, Integer maxResults, Integer offset) throws BadRequestException {
    	List<Trip> trips = Collections.emptyList();
    	if (until != null && since != null && !until.isAfter(since)) {
    		throw new BadRequestException("Constraint violation: 'until' must be later than 'since'.");
    	}
    	if (maxResults != null && maxResults > 100) {
    		throw new BadRequestException("Constraint violation: 'maxResults' <= 100.");
    	}
    	if (maxResults != null && maxResults <= 0) {
    		throw new BadRequestException("Constraint violation: 'maxResults' > 0.");
    	}
    	if (maxResults != null && offset < 0) {
    		throw new BadRequestException("Constraint violation: 'offset' >= 0.");
    	}
    	if (traveller != null) {
    		List<Long> tripIds = tripDao.findByTraveller(traveller, since, until, deletedToo, maxResults, offset);
    		if (tripIds.size() > 0) {
    			trips = tripDao.fetch(tripIds, Trip.LIST_TRIPS_ENTITY_GRAPH);
    		}
    	}
    	return trips;
    	
    }

    /**
     * List all trips owned by  the calling user. Soft deleted trips are omitted.
     * @return A list of trips owned by the calling user.
     */
    public List<Trip> listMyTrips(Instant since, Instant until, Boolean deletedToo, Integer maxResults, Integer offset) throws BadRequestException {
    	return listTrips(userManager.findCallingUser(), since, until, deletedToo, maxResults, offset);
    }
    
    private void validateCreateUpdateTrip(Trip trip)  throws BadRequestException {
    	if (trip.getDepartureTime() == null) {
    		throw new BadRequestException("Constraint violation: A new trip must have a 'departureTime'");
    	}
    	if (trip.getFrom() == null || trip.getTo() == null) {
    		throw new BadRequestException("Constraint violation: A new trip must have a 'from' and a 'to'");
    	}
    }

    /**
     * Creates a trip on behalf of a user. If a trip contains bookable legs, the leg will automatically be booked  if the autobook flag is set. 
     * @param user the user for whom the trip is created
     * @param trip the new trip
     * @param autobook If set then start the booking process of each leg.
     * @return The ID of the trip just created.
     * @throws CreateException In case of trouble, like wrong parameter values.
     * @throws BadRequestException In case of bad parameters.
     */
    public Long createTrip(User traveller, Trip trip, boolean autobook) throws BadRequestException, CreateException {
    	validateCreateUpdateTrip(trip);
    	trip.setTraveller(traveller);
    	trip.setState(TripState.PLANNING);
       	tripDao.save(trip);
       	tripDao.flush();
       	if (autobook && trip.getLegs() != null) {
       		for (Leg leg : trip.getLegs()) {
				startBookingProcessIfNecessary(traveller, trip, leg);
			}
           	updateTripState(trip);
       	}
    	return trip.getId();
    }

    public Long createTrip(User traveller, Trip trip) throws BadRequestException, CreateException {
    	return createTrip(traveller,  trip, true);
    }
    
    /**
     * Creates a trip. 
     * @param trip the new trip
     * @param autobook If set then start the booking process of each leg.
     * @return The ID of the trip just created.
     * @throws CreateException In case of trouble, like wrong parameter values.
     * @throws BadRequestException In case of bad parameters.
     */
    public Long createTrip(Trip trip, boolean autobook) throws BadRequestException, CreateException {
    	return createTrip(userManager.registerCallingUser(), trip, autobook);
    }

    protected void startBookingProcessIfNecessary(User traveller, Trip trip, Leg leg) throws CreateException {
    	if (leg.getTraverseMode() == TraverseMode.RIDESHARE) {
    		leg.setState(TripState.BOOKING);
        	Booking booking = new Booking();
        	booking.setNrSeats(trip.getNrSeats() == null ? 1 : trip.getNrSeats());
        	booking.setDropOff(new Stop());
        	booking.setDropOff(new Stop(leg.getFrom().getLocation()));
			try {
				String bookingRef = bookingManager.createBooking(leg.getTripId(), traveller, 
						leg.getFrom().getLocation(), leg.getTo().getLocation(), 1);
				leg.setBookingId(bookingRef);
    			leg.setState(TripState.SCHEDULED);
			} catch (ObjectNotFoundException | javax.ejb.CreateException e) {
				throw new CreateException("cannot create booking", e);
			}
    	} else {
			leg.setState(TripState.SCHEDULED);
    	}
    }

    /**
     * Assigns the lowest leg state (in ordinal terms) to the overall trip state. 
     * @param trip the trip to analyze.
     */
   	protected void updateTripState(Trip trip) {
   		if (trip.getLegs() == null) {
   			return;
   		}
   		Optional<Leg> minleg = trip.getLegs().stream().min(Comparator.comparingInt(leg -> leg.getState().ordinal()));
   		minleg.ifPresent(leg -> trip.setState(leg.getState()));
   	}

    
    /**
     * Retrieves a ride. Anyone can read a ride, given the id. All details are retrieved.
     * @param id
     * @return
     * @throws NotFoundException
     */
    public Trip getTrip(Long id) throws NotFoundException {
    	Trip tripdb = tripDao.find(id, tripDao.createLoadHint(Trip.LIST_TRIP_DETAIL_ENTITY_GRAPH))
    			.orElseThrow(NotFoundException::new);
    	return tripdb;
    }
    
    /**
     * Removes a trip. Whether or not a trip is soft-deleted or hard-deleted dependson the trip state.
     * @param tripId The trip to remove.
     * @throws NotFoundException The trip doesnot exist.
     */
    public void removeTrip(Long tripId) throws NotFoundException {
    	Trip tripdb = tripDao.find(tripId)
    			.orElseThrow(NotFoundException::new);
    	//    	security.checkOwnership(tripdb.getTraveller(), Trip.class.getSimpleName());
    	if (tripdb.getState() == TripState.PLANNING) {
    		// Hard delete
			tripDao.remove(tripdb);
    	} else {
    		tripdb.setDeleted(true);
    	}
    }
 
}
