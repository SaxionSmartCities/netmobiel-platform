package eu.netmobiel.planner.service;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.UpdateException;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.model.event.BookingCancelledEvent;
import eu.netmobiel.commons.model.event.BookingConfirmedEvent;
import eu.netmobiel.commons.model.event.BookingRequestedEvent;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.planner.event.ShoutOutResolvedEvent;
import eu.netmobiel.planner.event.TripConfirmedEvent;
import eu.netmobiel.planner.event.TripStateUpdatedEvent;
import eu.netmobiel.planner.model.Itinerary;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.PlanType;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripMonitorEvent;
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
	/**
	 * The duration of the departing state.
	 */
	private static final Duration DEPARTING_PERIOD = Duration.ofMinutes(15);
	/**
	 * The duration of the arriving state.
	 */
	private static final Duration ARRIVING_PERIOD = Duration.ofMinutes(15);
	/**
	 * The delay before sending a invitation for a confirmation.
	 */
	private static final Duration CONFIRMATION_DELAY = Duration.ofMinutes(15);
	/**
	 * The maximum duration of the first confirmation period.
	 */
	private static final Duration CONFIRM_PERIOD_1 = Duration.ofDays(2);
	/**
	 * The period after which to send a confirmation reminder, if necessary.
	 */
	private static final Duration CONFIRM_PERIOD_2 = Duration.ofDays(2);
	/**
	 * The total period after which a confirmation period expires.
	 */
	private static final Duration CONFIRMATION_PERIOD = CONFIRM_PERIOD_1.plus(CONFIRM_PERIOD_2);
	
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

    @Inject
    private Event<BookingConfirmedEvent> bookingConfirmedEvent;

    @Inject
    private Event<ShoutOutResolvedEvent> shoutOutResolvedEvent;

//    @Inject @Removed
//    private Event<Trip> tripCancelledEvent;
//
//    @Inject
//    private Event<TripScheduledEvent> tripScheduledEvent;
//
    @Resource
    private TimerService timerService;

    @Inject
    private Event<TripStateUpdatedEvent> tripStateUpdatedEvent;

    @Inject
    private Event<TripConfirmedEvent> tripConfirmedEvent;

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
     * @return The ID of the trip just created.
     * @throws NotFoundException In case one of the referenced object cannot be found.
     * @throws BadRequestException In case of bad parameters.
     */
    public Long createTrip(User traveller, Trip trip) throws NotFoundException, BadRequestException {
    	trip.setTraveller(traveller);
    	if (trip.getItineraryRef() == null) {
    		throw new BadRequestException("Specify an itinerary reference");
    	}
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
    	if (plan.getPlanType() == PlanType.SHOUT_OUT) {
    		// it was a shout-out plan. It is being resolved now. 
    		shoutOutResolvedEvent.fire(new ShoutOutResolvedEvent(it));
    	}
        trip.setArrivalTimeIsPinned(plan.isUseAsArrivalTime());
    	trip.setNrSeats(plan.getNrSeats());
        trip.setFrom(plan.getFrom());
        trip.setTo(plan.getTo());
       	tripDao.save(trip);
       	tripDao.flush();
       	List<BookingRequestedEvent> bookingRequestedEvents = new ArrayList<>();
       	List<BookingConfirmedEvent> bookingConfirmedEvents = new ArrayList<>();
   		for (Leg leg : trip.getItinerary().getLegs()) {
	    	// Check for bookingID set. If so than it was a shout-out and we need to convert the PROPOSAL to a CONFIRMED booking
	    	if (leg.getBookingId() != null) {
	    		// This must be a proposed booking. Confirm it. Replace the plan reference with the trip reference
	    		BookingConfirmedEvent bce = new BookingConfirmedEvent(leg.getBookingId(), traveller, trip.getTripRef());
   				leg.setState(TripState.SCHEDULED);
   				bookingConfirmedEvents.add(bce);
	    	} else if (leg.isBookingRequired()) {
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
   				bookingRequestedEvents.add(b);
	    	} else {
   	    		// If no booking is required then no further action is required. Schedule the leg.
   				leg.setState(TripState.SCHEDULED);
	    	}
       		// So what is exactly the content of the persistence context after the firing of the event?
       		// Should we merge/refresh?
       	}
		updateTripState(trip);
       	// Update the trip state before sending the event. Just for consistency.
       	bookingRequestedEvents.stream().forEach(event -> bookingRequestedEvent.fire(event));
       	bookingConfirmedEvents.stream().forEach(event -> bookingConfirmedEvent.fire(event));
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
    		updateTripState(trip);
    	}
    }

    protected void updateTripState(Trip trip) {
    	TripState previousState = trip.getState();
		trip.updateTripState();
       	if (trip.getState() == TripState.SCHEDULED) {
        	if (! trip.isMonitored() && trip.getItinerary().getDepartureTime().minus(DEPARTING_PERIOD.plus(Duration.ofHours(2))).isAfter(Instant.now())) {
        		startMonitoring(trip);
        	}
       	} else if (trip.getState() == TripState.CANCELLED) {
       		cancelTripTimers(trip);
    		trip.setMonitored(false);
       	}
    	log.debug(String.format("updateTripState %s: %s --> %s", previousState, trip.getState()));
		tripStateUpdatedEvent.fire(new TripStateUpdatedEvent(previousState, trip));
    }

    /**
     * Cancels the booked leg on trip and updates the state. This method is called in response to a cancellation from the transport provider.
     * This call is intended to update the trip state only.
     * @param tripRef
     * @param bookingRef
     * @param reason
     * @param cancelledByDriver
     * @throws NotFoundException
     */
    public void cancelBooking(String tripRef, String bookingRef, String reason, boolean cancelledByDriver) throws NotFoundException {
    	Trip trip = tripDao.find(PlannerUrnHelper.getId(Trip.URN_PREFIX, tripRef))
    			.orElseThrow(() -> new NotFoundException("No such trip: " + tripRef));
    	trip.setCancelReason(reason);
    	Leg leg = trip.getItinerary().findLegByBookingId(bookingRef)
    			.orElseThrow(() -> new NotFoundException("No such leg with bookingId " + bookingRef));
		leg.setState(TripState.CANCELLED);
		updateTripState(trip);
    }

  
    /**
     * Retrieves a trip. Anyone can read a trip, given the id. All details are retrieved.
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
   		updateTripState(tripdb);
    }
 
    /**
     * Sets the confirmation flag on each leg in the trip.
     * @param tripId the trip to update.
     * @throws NotFoundException If the trip was not found.
     */
    public void confirmTrip(Long tripId, Boolean confirmationValue) throws NotFoundException, BadRequestException {
    	Trip tripdb = tripDao.fetchGraph(tripId, Trip.MY_LEGS_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such trip: " + tripId));
    	if (confirmationValue == null) {
    		throw new BadRequestException("An empty confirmation value is not allowed");
    	}
    	if (tripdb.getState() != TripState.COMPLETED) { 
    		// No use to set this flag when the trip has already been finished physically and administratively)
        	if (tripdb.getState() != TripState.VALIDATING) {
        		throw new BadRequestException("Unexpected state for a confirmation: " + tripId + " " + tripdb.getState());
        	}
        	for (Leg leg : tripdb.getItinerary().getLegs()) {
            	if (leg.getConfirmed() != null) {
            		throw new BadRequestException("Leg has already a confirmation value: " + leg.getId());
            	}
            	leg.setConfirmed(confirmationValue);
        	}
        	tripConfirmedEvent.fire(new TripConfirmedEvent(tripdb));
    	}
    }

    /**
     * Sets the confirmation flag on each leg in the trip.
     * @param tripId the trip to update.
     * @throws NotFoundException If the trip was not found.
     */
    public void confirmTripByTransportProvider(String tripRef, String bookingRef, Boolean confirmationValue) throws NotFoundException, BadRequestException {
    	Trip tripdb = tripDao.fetchGraph(PlannerUrnHelper.getId(Trip.URN_PREFIX, tripRef), Trip.MY_LEGS_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such trip: " + tripRef));
    	if (confirmationValue == null) {
    		throw new BadRequestException("An empty confirmation value is not allowed");
    	}
    	// A confirmation from a transport provider can arrive even the trip is still in transit.
    	if (tripdb.getState() != TripState.COMPLETED) { 
    		// No use to set this flag when the trip has already been finished physically and administratively)
        	if (tripdb.getState() != TripState.IN_TRANSIT && tripdb.getState() != TripState.ARRIVING && tripdb.getState() != TripState.VALIDATING) {
        		throw new BadRequestException("Unexpected state for a confirmation: " + tripRef + " " + tripdb.getState());
        	}
        	Leg bookedLeg = tripdb.getItinerary().findLegByBookingId(bookingRef)
        			.orElseThrow(() -> new IllegalArgumentException("No such booking on trip: " + tripRef + " " + bookingRef));
           	if (bookedLeg.getConfirmedByProvider() != null) {
           		throw new BadRequestException("Leg has already a confirmation value by provider: " + bookedLeg.getId());
           	}
           	bookedLeg.setConfirmedByProvider(confirmationValue);
    	}
    }

    public static class TripInfo implements Serializable {
		private static final long serialVersionUID = -2715209888482006490L;
		public TripMonitorEvent event;
    	public Long tripId;
    	public TripInfo(TripMonitorEvent anEvent, Long aTripId) {
    		this.event = anEvent;
    		this.tripId = aTripId;
    	}
    	
		@Override
		public String toString() {
			return String.format("TripInfo [%s %s]", event, tripId);
		}
    }
    
	@Schedule(info = "Collect due trips", hour = "*/1", minute = "0", second = "0", persistent = false /* non-critical job */)
	public void checkForDueTrips() {
		log.debug("CollectDueTrips");
		// Get all trips that are in scheduled state and have a departure time within a certain window
		List<Trip> trips = tripDao.findMonitorableTrips(Instant.now().plus(Duration.ofHours(2).plus(DEPARTING_PERIOD)));
		for (Trip trip : trips) {
			startMonitoring(trip);
		}
	}

    protected void updateTripState(Trip trip, TripState newState) {
    	TripState previousState = trip.getState();
		trip.setState(newState);
    	log.debug(String.format("updateTripState %s: %s --> %s", previousState, trip.getState()));
   		tripStateUpdatedEvent.fire(new TripStateUpdatedEvent(previousState, trip));
    }

    @Timeout
	public void onTimeout(Timer timer) {
		if (! (timer.getInfo() instanceof TripInfo)) {
			log.error("Don't know how to handle timeout: " + timer.getInfo());
			return;
		}
		TripInfo tripInfo = (TripInfo) timer.getInfo();
		if (log.isDebugEnabled()) {
			log.debug("Received trip event: " + tripInfo.toString());
		}
		Trip trip = tripDao.fetchGraph(tripInfo.tripId, Trip.DETAILED_ENTITY_GRAPH)
				.orElseThrow(() -> new IllegalArgumentException("No such trip: " + tripInfo.tripId));
		Instant now = Instant.now();
		switch (tripInfo.event) {
		case TIME_TO_PREPARE:
			updateTripState(trip, TripState.DEPARTING);
			timerService.createTimer(Date.from(trip.getItinerary().getDepartureTime()), 
					new TripInfo(TripMonitorEvent.TIME_TO_DEPART, trip.getId()));
			break;
		case TIME_TO_DEPART:
			updateTripState(trip, TripState.IN_TRANSIT);
			timerService.createTimer(Date.from(trip.getItinerary().getArrivalTime()), 
					new TripInfo(TripMonitorEvent.TIME_TO_ARRIVE, trip.getId()));
			break;
		case TIME_TO_ARRIVE:
			updateTripState(trip, TripState.ARRIVING);
			if (trip.getItinerary().isConfirmationRequested() && 
					trip.getItinerary().getArrivalTime().plus(CONFIRMATION_PERIOD).isAfter(now)) {
				timerService.createTimer(Date.from(trip.getItinerary().getArrivalTime().plus(CONFIRMATION_DELAY)), 
						new TripInfo(TripMonitorEvent.TIME_TO_VALIDATE, trip.getId()));
			} else {
				timerService.createTimer(Date.from(trip.getItinerary().getArrivalTime().plus(ARRIVING_PERIOD)), 
						new TripInfo(TripMonitorEvent.TIME_TO_COMPLETE, trip.getId()));
			}
			break;
		case TIME_TO_VALIDATE:
			updateTripState(trip, TripState.VALIDATING);
			timerService.createTimer(Date.from(trip.getItinerary().getArrivalTime().plus(CONFIRM_PERIOD_1)), 
					new TripInfo(TripMonitorEvent.TIME_TO_CONFIRM_REMINDER, trip.getId()));
			break;
		case TIME_TO_CONFIRM_REMINDER:
			updateTripState(trip, TripState.VALIDATING);
			timerService.createTimer(Date.from(trip.getItinerary().getArrivalTime().plus(CONFIRM_PERIOD_2)), 
					new TripInfo(TripMonitorEvent.TIME_TO_COMPLETE, trip.getId()));
			break;
		case TIME_TO_COMPLETE:
			updateTripState(trip, TripState.COMPLETED);
			trip.setMonitored(false);
			break;
		default:
			log.warn("Don't know how to handle event: " + tripInfo.event);
			break;
		}
	}

	protected void startMonitoring(Trip trip) {
		if (trip.getState() == TripState.CANCELLED) {
			log.warn("Cannot monitor, trip has been canceled: " + trip.getId());
			return;
		}
		if (trip.isMonitored()) {
			log.warn("Trip already monitored: " + trip.getId());
			return;
		}
		trip.setMonitored(true);
		// Should we always generate timer events and let the state machine decide what to do?
		// Tested. Result: Timer events are received in random order.
		// Workaround: Set the next timer in each event handler
		timerService.createTimer(Date.from(trip.getItinerary().getDepartureTime().minus(DEPARTING_PERIOD)), 
				new TripInfo(TripMonitorEvent.TIME_TO_PREPARE, trip.getId()));
	}

	protected void cancelTripTimers(Trip trip) {
    	// Find all timers related to this trip and cancel them
    	Collection<Timer> timers = timerService.getTimers();
		for (Timer timer : timers) {
			if ((timer.getInfo() instanceof TripInfo)) {
				TripInfo tripInfo = (TripInfo) timer.getInfo();
				if (tripInfo.tripId.equals(trip.getId())) {
					try {
						timer.cancel();
					} catch (Exception ex) {
						log.error("Unable to cancel timer: " + ex.toString());
					}
				}
			}
		}
	}
}
