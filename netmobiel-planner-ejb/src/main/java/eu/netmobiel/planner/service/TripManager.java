package eu.netmobiel.planner.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.ApplicationException;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.model.event.BookingCancelledEvent;
import eu.netmobiel.commons.model.event.BookingRequestedEvent;
import eu.netmobiel.commons.model.event.ShoutOutRequestedEvent;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.Stop;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripState;
import eu.netmobiel.planner.model.User;
import eu.netmobiel.planner.repository.TripDao;
import eu.netmobiel.planner.util.PlannerUrnHelper;

@Stateless
@Logging
public class TripManager {
	public static final Integer MAX_RESULTS = 10; 

	@Inject
    private Logger log;

    @Inject
    private TripDao tripDao;

    @Inject
    private Event<BookingRequestedEvent> bookingRequestedEvent;

    @Inject
    private Event<BookingCancelledEvent> bookingCancelledEvent;

    @Inject
    private Event<ShoutOutRequestedEvent> shoutOutRequestedEvent;
    
    /**
     * List all trips owned by the specified user. Soft deleted trips are omitted.
     * @return A list of trips owned by the specified user.
     */
    public PagedResult<Trip> listTrips(User traveller, TripState state, Instant since, Instant until, Boolean deletedToo, 
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
        List<Trip> results = Collections.emptyList();
        Long totalCount = 0L;
    	if (traveller != null && traveller.getId() != null) {
    		PagedResult<Long> prs = tripDao.findByTraveller(traveller, state, since, until, deletedToo, sortDirection, 0, 0);
    		totalCount = prs.getTotalCount();
        	if (totalCount > 0 && maxResults > 0) {
        		// Get the actual data
        		PagedResult<Long> tripIds = tripDao.findByTraveller(traveller, state, since, until, deletedToo, sortDirection, maxResults, offset);
        		if (tripIds.getData().size() > 0) {
        			results = tripDao.fetch(tripIds.getData(), Trip.LIST_TRIPS_ENTITY_GRAPH);
        		}
        	}
    	} 
    	return new PagedResult<Trip>(results, maxResults, offset, totalCount);
    }

    private void validateCreateUpdateTrip(Trip trip)  throws BadRequestException {
    	if (trip.getDepartureTime() == null || trip.getArrivalTime() == null) {
    		throw new BadRequestException("Constraint violation: A new trip must have a 'departureTime' as well as an 'arrivalTIme'");
    	}
    	if (trip.getFrom() == null || trip.getTo() == null) {
    		throw new BadRequestException("Constraint violation: A new trip must have a 'from' and a 'to'");
    	}
    }

    /**
     * Fire a shout-out event asynchronously.
     * @param trip The trip of the traveller
     * @param leg the leg for which to issue a shout-out.
     */
    private void issueShoutOutRequest(Trip trip, Leg leg) {
    	ShoutOutRequestedEvent sor = new ShoutOutRequestedEvent(trip.getTraveller(), trip.getTripRef());
    	sor.setNrSeats(trip.getNrSeats());
    	sor.setPickup(leg.getFrom().getLocation());
    	sor.setDepartureTime(leg.getStartTime());
    	sor.setDropOff(leg.getTo().getLocation());
    	sor.setArrivalTime(leg.getEndTime());
    	shoutOutRequestedEvent.fire(sor);
    }

    /**
     * Creates a trip on behalf of a user. If a trip contains bookable legs, the leg will automatically be booked  if the autobook flag is set. 
     * @param user the user for whom the trip is created
     * @param trip the new trip
     * @param autobook If set then start the booking and scheduling process of each leg.
     * @return The ID of the trip just created.
     * @throws CreateException In case of trouble, like wrong parameter values.
     * @throws BadRequestException In case of bad parameters.
     */
    public Long createTrip(User traveller, Trip trip, boolean autobook) throws BadRequestException, CreateException {
    	validateCreateUpdateTrip(trip);
    	trip.setTraveller(traveller);
    	trip.setState(TripState.PLANNING);
       	if (trip.getLegs() == null || trip.getLegs().isEmpty()) {
       		// There is no itinerary at all. Issue a shout-out.
       		trip.setLegs(new ArrayList<>());
       		trip.setStops(new ArrayList<>());
       		Stop from = new Stop(trip.getFrom(), trip.getDepartureTime(), null);
       		Stop to = new Stop(trip.getTo(), null, trip.getArrivalTime());
       		Leg leg = new Leg(from, to);
       		trip.getStops().add(from);
       		trip.getStops().add(to);
       		trip.getLegs().add(leg);
			leg.setState(TripState.PLANNING);
       	} 
       	tripDao.save(trip);
       	tripDao.flush();
   		for (Leg leg : trip.getLegs()) {
    	    if (leg.getTraverseMode() == null) {
    	    	// There is a leg, but the transport is undefined yet. Issue a shout-out. 
    	    	// The trip is already persisted, so we have an ID 
        		issueShoutOutRequest(trip, leg);
    	    } else if (autobook) {
    	    	if (leg.isBookingRequired()) {
       	    		// Ok, we need to take additional steps before the leg can be scheduled. Start a booking procedure.
       	    		leg.setState(TripState.BOOKING);
       				// Use the trip as reference, we are not sure the leg ID is a stable, permanent identifier in case of an update of a trip.
       				// Add the reference to the trip of the provider, e.g. the ride in case of rideshare.
       				BookingRequestedEvent b = new BookingRequestedEvent(traveller, trip.getTripRef(), leg.getTripId());
       				b.setArrivalTime(leg.getEndTime());
       				b.setDepartureTime(leg.getStartTime());
       				b.setDropOff(leg.getTo().getLocation());
       				b.setNrSeats(trip.getNrSeats());
       				b.setPickup(leg.getFrom().getLocation());
       				bookingRequestedEvent.fire(b);
    	    	} else {
       	    		// If no booking is required then no further action is required. Schedule the leg.
       				leg.setState(TripState.SCHEDULED);
    	    	}
   	    	} else {
	    		log.warn(String.format("Trip %s Leg %s requires explicit booking", trip.getTripRef(), leg.getTripId()));
   	    	}
       		// So what is exactly the content of the persistence context after the firing of the event?
       		// Should we merge/refresh?
       	}
       	updateTripState(trip);
    	return trip.getId();
    }

    /**
     * Assign a booking reference to the leg with the specified transport provider tripId.  
     * @param tripRef The traveller trip reference, i.e. our  trip reference.
     * @param transportProviderTripRef The transport provider's trip reference, i.e. their trip. 
     * @param bookingRef The booking reference at the transport provider.
     * @param bookingConfirmed If true the booking is already confirmed.
     */
    public void assignBookingReference(String tripRef, String transportProviderTripRef, String bookingRef, boolean bookingConfirmed) {
    	Trip trip = tripDao.find(PlannerUrnHelper.getId(Trip.URN_PREFIX, tripRef),
    							tripDao.createLoadHint(Trip.LIST_TRIPS_ENTITY_GRAPH))
    			.orElseThrow(() -> new IllegalArgumentException("No such trip: " + tripRef));
    	Leg leg = trip.findLegByTripId(transportProviderTripRef)
    			.orElseThrow(() -> new IllegalArgumentException("No such leg with tripId " + transportProviderTripRef));
    	leg.setBookingId(bookingRef);
    	if (bookingConfirmed) {
    		leg.setState(TripState.SCHEDULED);
    		updateTripState(trip);
    	}
    }

    public void cancelBooking(String tripRef, String bookingRef, String reason, boolean cancelledByDriver) {
    	Trip trip = tripDao.find(PlannerUrnHelper.getId(Trip.URN_PREFIX, tripRef),
    							tripDao.createLoadHint(Trip.LIST_TRIPS_ENTITY_GRAPH))
    			.orElseThrow(() -> new IllegalArgumentException("No such trip: " + tripRef));
    	Leg leg = trip.findLegByBookingId(bookingRef)
    			.orElseThrow(() -> new IllegalArgumentException("No such leg with bookingId " + bookingRef));
		leg.setState(TripState.CANCELLED);
		updateTripState(trip);
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
     * This method is supposedly to be called by the traveller. 
     * @param tripId The trip to remove.
     * @param reason The reason for cancelling the trip (optional).
     * @throws NotFoundException The trip does not exist.
     */
    public void removeTrip(Long tripId, String reason) throws ApplicationException {
    	Trip tripdb = tripDao.find(tripId)
    			.orElseThrow(() -> new NotFoundException("No such trip: " + tripId));
    	tripdb.setCancelReason(reason);
       	if (tripdb.getLegs() != null) {
       		for (Leg leg : tripdb.getLegs()) {
		    	if (leg.getBookingId() != null) {
		    		// There is a booking being request or already confirmed. Cancel it.
					if (leg.getState() == TripState.BOOKING || leg.getState() == TripState.SCHEDULED) {
						if (log.isDebugEnabled()) {
							log.debug("Cancelling a booking. State = " + leg.getState());
						}
						BookingCancelledEvent bce = new BookingCancelledEvent(leg.getBookingId(), 
								tripdb.getTraveller(), PlannerUrnHelper.createUrn(Trip.URN_PREFIX, tripId),
								reason, false, false);
						// For now use a synchronous removal
						bookingCancelledEvent.fire(bce);						
					} else {
						log.warn(String.format("Cannot cancel booking %s, because of current state: %s", leg.getBookingId(), leg.getState()));
					}
		    	} else {
		    		// There is a small opening between setting Booking and the setting of a booking ID.
		    		if (leg.getState() == TripState.BOOKING) {
		    			log.warn("Trip is in BOOKING state, but no booking reference has been set!");
		    		}
		    	}
		    	if (leg.getState() == TripState.IN_TRANSIT || leg.getState() == TripState.COMPLETED) {
		    		log.warn("Removing a trip at an invalid moment: " + leg.getState() + "; leg not cancelled");
				} else {
					leg.setState(TripState.CANCELLED);
				}
			}
       	}
    	if (tripdb.getState() == TripState.PLANNING) {
    		// Hard delete
			tripDao.remove(tripdb);
    	} else {
    		tripdb.setDeleted(true);
       		updateTripState(tripdb);
    	}
    }
 
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
