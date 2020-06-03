package eu.netmobiel.planner.service;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripState;
import eu.netmobiel.planner.model.User;
import eu.netmobiel.planner.repository.TripDao;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.service.BookingManager;

@Stateless
@Logging
public class TripManager {
	public static final Integer MAX_RESULTS = 10; 
	@SuppressWarnings("unused")
	@Inject
    private Logger log;

    @Inject
    private TripDao tripDao;

    @Inject
    private BookingManager bookingManager;

    /**
     * List all trips owned by the specified user. Soft deleted trips are omitted.
     * @return A list of trips owned by the specified user.
     */
    public PagedResult<Trip> listTrips(User traveller, TripState state, Instant since, Instant until, Boolean deletedToo, Integer maxResults, Integer offset) throws BadRequestException {
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
        List<Trip> results = Collections.emptyList();
        Long totalCount = 0L;
    	if (traveller != null && traveller.getId() != null) {
    		PagedResult<Long> prs = tripDao.findByTraveller(traveller, state, since, until, deletedToo, 0, 0);
    		totalCount = prs.getTotalCount();
        	if (totalCount > 0 && maxResults > 0) {
        		// Get the actual data
        		PagedResult<Long> tripIds = tripDao.findByTraveller(traveller, state, since, until, deletedToo, maxResults, offset);
        		if (tripIds.getData().size() > 0) {
        			results = tripDao.fetch(tripIds.getData(), Trip.LIST_TRIPS_ENTITY_GRAPH);
        		}
        	}
    	} 
    	return new PagedResult<Trip>(results, maxResults, offset, totalCount);
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

    protected void startBookingProcessIfNecessary(User traveller, Trip trip, Leg leg) throws CreateException, BadRequestException {
    	if (leg.getTraverseMode() == TraverseMode.RIDESHARE) {
    		leg.setState(TripState.BOOKING);
			try {
				Booking b = new Booking();
				b.setDepartureTime(leg.getStartTime());
				b.setPickup(leg.getFrom().getLocation());
				b.setArrivalTime(leg.getEndTime());
				b.setDropOff(leg.getTo().getLocation());
				b.setNrSeats(trip.getNrSeats());
				String bookingRef = bookingManager.createBooking(leg.getTripId(), traveller, b);
				leg.setBookingId(bookingRef);
    			leg.setState(TripState.SCHEDULED);
    			//FIXME Verify the actual timing and locations
			} catch (NotFoundException | CreateException e) {
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
     * Removes a trip. Whether or not a trip is soft-deleted or hard-deleted depends on the trip state.
     * 
     * @param tripId The trip to remove.
     * @throws NotFoundException The trip does not exist.
     */
    public void removeTrip(Long tripId) throws NotFoundException {
    	Trip tripdb = tripDao.find(tripId)
    			.orElseThrow(NotFoundException::new);
//    	userManager.checkOwnership(tripdb.getTraveller(), Trip.class.getSimpleName());
    	if (tripdb.getState() == TripState.PLANNING) {
    		// Hard delete
			tripDao.remove(tripdb);
    	} else {
    		// TODO Add cancelling of the legs!
    		if (tripdb.getState() == TripState.BOOKING || tripdb.getState() == TripState.SCHEDULED) {
    	       	if (tripdb.getLegs() != null) {
    	       		for (Leg leg : tripdb.getLegs()) {
//    					cancelBookingProcessIfNecessary(leg);
    				}
//    	           	updateTripState(trip);
    	       	}
    		}
    		tripdb.setDeleted(true);
    	}
    }
 
//    protected void cancelBookingsIfNecessary(Leg leg) throws CreateException {
//    	if (leg.getTraverseMode() == TraverseMode.RIDESHARE && 
//    			(leg.getState() == TripState.BOOKING || leg.getState() == TripState.SCHEDULED)) {
//    		leg.setState(TripState.CANCELLED);
//			try {
//				String bookingRef = bookingManager.createBooking(leg.getTripId(), traveller, 
//						leg.getFrom().getLocation(), leg.getTo().getLocation(), 1);
//				leg.setBookingId(bookingRef);
//    			leg.setState(TripState.SCHEDULED);
//			} catch (NotFoundException | CreateException e) {
//				throw new CreateException("cannot create booking", e);
//			}
//    	} else {
//			leg.setState(TripState.SCHEDULED);
//    	}
//    }

    /**
     * Lists a page of trips in planning state (of anyone) that have a departure or arrival location within a circle with radius 
     * <code>arrdepRadius</code> meter around the <code>location</code> and where both departure and arrival location are within
     * a circle with radius <code>travelRadius</code> meter. 
     * @param location the reference location of the driver asking for the trips.
     * @param startTime the time from where to start the search. 
     * @param depArrRadius the small circle containing at least departure or arrival location of the traveller.
     * @param travelRadius the larger circle containing both departure and arrival location of the traveller.
     * @param maxResults For paging: maximum results.
     * @param offset For paging: the offset in the results to return.
     * @return A list of trips matching the parameters.
     */
    public PagedResult<Trip> listShoutOuts(GeoLocation location, Instant startTime, Integer depArrRadius, 
    		Integer travelRadius, Integer maxResults, Integer offset) {
        if (maxResults == null) {
        	maxResults = MAX_RESULTS;
        }
        if (offset == null) {
        	offset = 0;
        }
        List<Trip> results = Collections.emptyList();
        Long totalCount = 0L;
   		PagedResult<Long> prs = tripDao.findShoutOutTrips(location, startTime, depArrRadius, travelRadius, 0, 0);
		totalCount = prs.getTotalCount();
    	if (totalCount > 0 && maxResults > 0) {
    		// Get the actual data
    		PagedResult<Long> tripIds = tripDao.findShoutOutTrips(location, startTime, depArrRadius, travelRadius, maxResults, offset);
    		if (tripIds.getData().size() > 0) {
    			results = tripDao.fetch(tripIds.getData(), Trip.LIST_TRIP_DETAIL_ENTITY_GRAPH);
    		}
    	}
    	return new PagedResult<Trip>(results, maxResults, offset, totalCount);
    }

}
