package eu.netmobiel.planner.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.UpdateException;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.model.event.BookingCancelledEvent;
import eu.netmobiel.commons.model.event.BookingRequestedEvent;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.planner.model.Itinerary;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.model.TripState;
import eu.netmobiel.planner.model.User;
import eu.netmobiel.planner.repository.ItineraryDao;
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
    private ItineraryDao itineraryDao;
    
    @Inject
    private Event<BookingRequestedEvent> bookingRequestedEvent;

    @Inject
    private Event<BookingCancelledEvent> bookingCancelledEvent;

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
		PagedResult<Long> prs = tripDao.findTrips(traveller, state, since, until, deletedToo, sortDirection, 0, 0);
		totalCount = prs.getTotalCount();
    	if (totalCount > 0 && maxResults > 0) {
    		// Get the actual data
    		PagedResult<Long> tripIds = tripDao.findTrips(traveller, state, since, until, deletedToo, sortDirection, maxResults, offset);
    		if (tripIds.getData().size() > 0) {
    			results = tripDao.fetch(tripIds.getData(), Trip.DETAILED_ENTITY_GRAPH, Trip::getId);
    		}
    	}
    	return new PagedResult<Trip>(results, maxResults, offset, totalCount);
    }

    /**
     * Creates a trip on behalf of a user. If a trip contains bookable legs, the leg will automatically be booked  if the autobook flag is set. 
     * @param user the user for whom the trip is created
     * @param trip the new trip
     * @param autobook If set then start the booking and scheduling process of each leg.
     * @return The ID of the trip just created.
     * @throws NotFoundException In case one of the referenced object cannot be found.
     * @throws BadRequestException In case of bad parameters.
     */
    public Long createTrip(User traveller, Trip trip, boolean autobook) throws NotFoundException, BadRequestException {
    	trip.setTraveller(traveller);
    	if (trip.getItineraryRef() != null) {
    		// Create a trip for this itinerary
        	Itinerary it = itineraryDao.find(PlannerUrnHelper.getId(Itinerary.URN_PREFIX, trip.getItineraryRef()))
        			.orElseThrow(() -> new NotFoundException("No such itinerary: " + trip.getItineraryRef()));
        	trip.setState(TripState.PLANNING);
        	trip.setItinerary(it);
        	// Load trip plan (lazy loaded, only plan itself)
        	TripPlan plan = it.getTripPlan();
        	if (plan == null) {
        		throw new NotFoundException("Itinerary has no plan attached!: " + trip.getItineraryRef());
        	}
            trip.setArrivalTimeIsPinned(plan.isUseAsArrivalTime());
        	trip.setNrSeats(plan.getNrSeats());
            trip.setFrom(plan.getFrom());
            trip.setTo(plan.getTo());
    	} else {
    		throw new BadRequestException("Specify an itinerary reference");
    	}
       	tripDao.save(trip);
       	tripDao.flush();
       	List<BookingRequestedEvent> bookingEvents = new ArrayList<>();
   		for (Leg leg : trip.getItinerary().getLegs()) {
    	    if (autobook) {
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
       				bookingEvents.add(b);
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
       	trip.updateTripState();
       	// Update the trip state before sending the event. Just for consistency.
       	bookingEvents.stream().forEach(event -> bookingRequestedEvent.fire(event));
    	return trip.getId();
    }

    /**
     * Assign a booking reference to the leg with the specified transport provider tripId.  
     * @param tripRef The traveller trip reference, i.e. our  trip reference.
     * @param transportProviderTripRef The transport provider's trip reference, i.e. their trip. 
     * @param bookingRef The booking reference at the transport provider.
     * @param bookingConfirmed If true the booking is already confirmed.
     * @throws UpdateException 
     */
    public void assignBookingReference(String tripRef, String transportProviderTripRef, String bookingRef, boolean bookingConfirmed) throws UpdateException {
    	Trip trip = tripDao.find(PlannerUrnHelper.getId(Trip.URN_PREFIX, tripRef))
    			.orElseThrow(() -> new IllegalArgumentException("No such trip: " + tripRef));
    	Leg leg = trip.getItinerary().findLegByTripId(transportProviderTripRef)
    			.orElseThrow(() -> new IllegalArgumentException("No such leg with tripId " + transportProviderTripRef));
    	if (trip.getState() != TripState.BOOKING) {
    		throw new UpdateException(String.format("Unexpected trip state %s, expected BOOKING; cannot assign booking %s to trip %s", 
    				trip.getState().toString(), bookingRef, tripRef));
    	}
    	if (!leg.getTripId().equals(transportProviderTripRef)) {
    		throw new UpdateException(String.format("Unexpected leg tripId %s, expected %s; cannot assign booking %s to trip %s", 
    				leg.getTripId(), transportProviderTripRef, bookingRef, tripRef));
    	}
    	leg.setBookingId(bookingRef);
    	if (bookingConfirmed) {
    		leg.setState(TripState.SCHEDULED);
    		trip.updateTripState();
    	}
    }

    public void cancelBooking(String tripRef, String bookingRef, String reason, boolean cancelledByDriver) throws NotFoundException {
    	Trip trip = tripDao.find(PlannerUrnHelper.getId(Trip.URN_PREFIX, tripRef))
    			.orElseThrow(() -> new NotFoundException("No such trip: " + tripRef));
    	Leg leg = trip.getItinerary().findLegByBookingId(bookingRef)
    			.orElseThrow(() -> new NotFoundException("No such leg with bookingId " + bookingRef));
		leg.setState(TripState.CANCELLED);
		trip.updateTripState();
    }

  
    /**
     * Retrieves a ride. Anyone can read a ride, given the id. All details are retrieved.
     * @param id
     * @return
     * @throws NotFoundException
     */
    public Trip getTrip(Long id) throws NotFoundException {
    	Trip tripdb = tripDao.loadGraph(id, Trip.DETAILED_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such trip: " + id));
    	return tripdb;
    }
    
    /**
     * Removes a trip. Trips are always soft-deleted for reasons of analysis.
     * This method is supposedly to be called by the traveller. 
     * @param tripId The trip to remove.
     * @param reason The reason for cancelling the trip (optional).
     * @throws NotFoundException The trip does not exist.
     */
    public void removeTrip(Long tripId, String reason) throws NotFoundException {
    	Trip tripdb = tripDao.find(tripId)
    			.orElseThrow(() -> new NotFoundException("No such trip: " + tripId));
    	tripdb.setCancelReason(reason);
       	if (tripdb.getItinerary().getLegs() != null) {
       		for (Leg leg : tripdb.getItinerary().getLegs()) {
		    	if (leg.getBookingId() != null) {
		    		// There is a booking being requested or already confirmed. Cancel it.
					if (leg.getState().isPreTravelState()) {
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
		    			// Continue, somewhere else in the process it will be discovered that the trip has been gone.
		    		}
		    	}
		    	if (leg.getState() == TripState.IN_TRANSIT || leg.getState().isPostTravelState()) {
		    		log.warn("Removing a trip at an invalid moment: " + leg.getState() + "; leg not cancelled");
				} else {
					leg.setState(TripState.CANCELLED);
				}
			}
       	}
   		tripdb.setDeleted(true);
   		tripdb.updateTripState();
    }
 
}
