package eu.netmobiel.planner.service;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.Schedule;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.RemoveException;
import eu.netmobiel.commons.exception.UpdateException;
import eu.netmobiel.commons.model.ConfirmationReasonType;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.report.ModalityNumericReportValue;
import eu.netmobiel.commons.report.NumericReportValue;
import eu.netmobiel.commons.util.EventFireWrapper;
import eu.netmobiel.commons.util.ExceptionUtil;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.planner.event.BookingCancelledEvent;
import eu.netmobiel.planner.event.BookingConfirmedEvent;
import eu.netmobiel.planner.event.BookingRequestedEvent;
import eu.netmobiel.planner.event.ShoutOutResolvedEvent;
import eu.netmobiel.planner.event.TripConfirmedEvent;
import eu.netmobiel.planner.event.TripStateUpdatedEvent;
import eu.netmobiel.planner.event.TripValidationExpiredEvent;
import eu.netmobiel.planner.model.Itinerary;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.PassengerBehaviourReport;
import eu.netmobiel.planner.model.PassengerModalityBehaviourReport;
import eu.netmobiel.planner.model.PaymentState;
import eu.netmobiel.planner.model.PlanType;
import eu.netmobiel.planner.model.PlannerUser;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripMonitorEvent;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.model.TripState;
import eu.netmobiel.planner.repository.ItineraryDao;
import eu.netmobiel.planner.repository.TripDao;
import eu.netmobiel.planner.repository.TripPlanDao;
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
	private static final Duration CONFIRM_PERIOD_2 = Duration.ofDays(5);
	/**
	 * The total period after which a confirmation period expires.
	 */
	private static final Duration CONFIRMATION_PERIOD = CONFIRM_PERIOD_1.plus(CONFIRM_PERIOD_2);
	
	@Inject
    private Logger log;

    @Inject
    private TripDao tripDao;

    @Inject
    private TripPlanDao tripPlanDao;

    @Inject
    private ItineraryDao itineraryDao;
    
    @Resource
    private SessionContext context;

    @Resource
    private TimerService timerService;

    @Inject
    private Event<BookingRequestedEvent> bookingRequestedEvent;

    @Inject
    private Event<BookingCancelledEvent> bookingCancelledEvent;

    @Inject
    private Event<BookingConfirmedEvent> bookingConfirmedEvent;

    @Inject
    private Event<ShoutOutResolvedEvent> shoutOutResolvedEvent;

    @Inject
    private Event<TripStateUpdatedEvent> tripStateUpdatedEvent;

    @Inject
    private Event<TripConfirmedEvent> tripConfirmedEvent;

    @Inject
    private Event<TripValidationExpiredEvent> tripValidationExpiredEvent;
    
    /**
     * List all trips owned by the specified user. Soft deleted trips are omitted.
     * @return A list of trips owned by the specified user.
     */
    public PagedResult<Trip> listTrips(PlannerUser traveller, TripState state, Instant since, Instant until, Boolean deletedToo, 
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
    			results = tripDao.loadGraphs(tripIds.getData(), Trip.DETAILED_ENTITY_GRAPH, Trip::getId);
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
     * @throws BusinessException In case of an exception coming through the event observers.
     */
    public Long createTrip(PlannerUser traveller, Trip trip) throws NotFoundException, BadRequestException, BusinessException {
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
    		EventFireWrapper.fire(shoutOutResolvedEvent, new ShoutOutResolvedEvent(it));
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
	    		BookingConfirmedEvent bce = new BookingConfirmedEvent(trip, leg);
   				leg.setState(TripState.SCHEDULED);
   				bookingConfirmedEvents.add(bce);
	    	} else if (leg.isBookingRequired()) {
   	    		// Ok, we need to take additional steps before the leg can be scheduled. Start a booking procedure.
   	    		leg.setState(TripState.BOOKING);
   				// Use the trip as reference, we are not sure the leg ID is a stable, permanent identifier in case of an update of a trip.
   				// Add the reference to the trip of the provider, e.g. the ride in case of rideshare.
   				bookingRequestedEvents.add(new BookingRequestedEvent(trip, leg));
	    	} else {
   	    		// If no booking is required then no further action is required. Schedule the leg.
   				leg.setState(TripState.SCHEDULED);
	    	}
       		// So what is exactly the content of the persistence context after the firing of the event?
       		// Should we merge/refresh?
       	}
		updateTripState(trip);
       	// Update the trip state before sending the event. Just for consistency.
		for (BookingRequestedEvent bre : bookingRequestedEvents) {
			EventFireWrapper.fire(bookingRequestedEvent, bre);
		}
		for (BookingConfirmedEvent bce : bookingConfirmedEvents) {
			EventFireWrapper.fire(bookingConfirmedEvent, bce);
		}
    	return trip.getId();
    }

    /**
     * Assign a booking reference to the leg with the specified transport provider tripId.  
     * @param tripRef The traveller trip reference, i.e. our  trip reference.
     * @param transportProviderTripRef The transport provider's trip reference, i.e. their trip. 
     * @param bookingRef The booking reference at the transport provider.
     * @param bookingConfirmed If true the booking is already confirmed.
     * @throws BusinessException 
     */
    public void assignBookingReference(String tripRef, String transportProviderTripRef, String bookingRef, boolean bookingConfirmed) throws BusinessException {
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

    /**
     * Cancels the booked leg on trip and updates the state. This method is called in response to a cancellation from the transport provider.
     * This call is intended to update the trip state only.
     * @param tripRef
     * @param bookingRef
     * @param reason
     * @param cancelledByTransportProvider
     * @return the leg concerned
     * @throws BusinessException 
     */
    public Leg cancelBooking(String tripRef, String bookingRef, String reason, boolean cancelledByTransportProvider) throws BusinessException {
    	Trip trip = tripDao.find(PlannerUrnHelper.getId(Trip.URN_PREFIX, tripRef))
    			.orElseThrow(() -> new NotFoundException("No such trip: " + tripRef));
    	trip.setCancelledByProvider(cancelledByTransportProvider);
    	trip.setCancelReason(reason);
    	Leg leg = trip.getItinerary().findLegByBookingId(bookingRef)
    			.orElseThrow(() -> new NotFoundException("No such leg with bookingId " + bookingRef));
		if (leg.getState() == TripState.CANCELLED) {
			throw new IllegalStateException("Leg already cancelled: " + leg.getId());
		}
		leg.setCancelledByProvider(cancelledByTransportProvider);
		leg.setState(TripState.CANCELLED);
		updateTripState(trip);
		return leg;
    }

  
    /**
     * Retrieves a trip. Anyone can read a trip, given the id. All details are retrieved.
     * @param id the trip id
     * @return a trip object
     * @throws NotFoundException No matching trip found.
     */
    public Trip getTrip(Long id) throws NotFoundException {
    	Trip tripdb = tripDao.loadGraph(id, Trip.DETAILED_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such trip: " + id));
    	return tripdb;
    }
    
    /**
     * Retrieves a trip by its itinerary. All details are retrieved.
     * @param itineraryId
     * @return a Trip object.
     * @throws NotFoundException No matching trip found.
     */
    public Trip getTripByItinerary(Long itineraryId) throws NotFoundException {
    	Long tripId = tripDao.findTripIdByItineraryId(itineraryId)
    			.orElseThrow(() -> new NotFoundException("No trip with such an itinerary: " + itineraryId));
    	return getTrip(tripId);
    }

    /**
     * Retrieves a trip by one of its legs. All details are retrieved.
     * @param legId
     * @return a Trip object.
     * @throws NotFoundException No matching trip found.
     */
    public Trip getTripByLeg(Long legId) throws NotFoundException {
    	Long tripId = tripDao.findTripIdByLegId(legId)
    			.orElseThrow(() -> new NotFoundException("No trip with such a leg: " + legId));
    	return getTrip(tripId);
    }

    /**
     * Removes a trip. Trips are always soft-deleted for reasons of analysis.
     * This method is supposedly to be called by the traveller.
     * Not sure whether there is for the passenger another method to cancel a trip. The trip might already be 
     * cancelled by the transport provider. 
     * @param tripId The trip to remove.
     * @param reason The reason for cancelling the trip (optional).
     * @throws BusinessException 
     */
    public void removeTrip(Long tripId, String reason) throws BusinessException {
    	Trip tripdb = tripDao.find(tripId)
    			.orElseThrow(() -> new NotFoundException("No such trip: " + tripId));
    	if (! Boolean.TRUE.equals(tripdb.getCancelledByProvider())) {
    		// Not already cancelled by the provider, so set my own reason for cancelling. 
    		tripdb.setCancelledByProvider(false);
    		tripdb.setCancelReason(reason);
    	}
    	//FIXME State handling is not robust enough
       	if (! tripdb.getState().isFinalState() && tripdb.getItinerary().getLegs() != null) {
       		for (Leg leg : tripdb.getItinerary().getLegs()) {
				if (leg.getState().isPreTravelState()) {
			    	if (leg.getBookingId() != null) {
			    		// There is a booking being requested or already confirmed. Cancel it.
						if (log.isDebugEnabled()) {
							log.debug("Cancelling a booking. State = " + leg.getState());
						}
						BookingCancelledEvent bce = new BookingCancelledEvent(tripdb, leg, reason);
						// For now use a synchronous removal
			        	EventFireWrapper.fire(bookingCancelledEvent, bce);
			    	} else {
			    		// There is a small opening between setting Booking and the setting of a booking ID.
			    		if (leg.getState() == TripState.BOOKING) {
			    			throw new IllegalStateException("Leg is in BOOKING state, but no booking reference has been set: " + leg.getId());
			    		}
			    	}
					leg.setState(TripState.CANCELLED);
					if (! Boolean.TRUE.equals(leg.getCancelledByProvider())) {
						leg.setCancelledByProvider(false);
					}
				} else if (leg.getState().isFinalState()) {
		    		// Already cancelled or completed, no action required
		    	} else {
		    		// travelling, validating
					throw new RemoveException(String.format("Cannot cancel trip %s; leg %s state %s forbids", tripdb.getId(), leg.getId(), leg.getState()));
		    	}
			}
       	}
   		tripdb.setDeleted(true);
   		updateTripState(tripdb);
    }
 
    /**
     * Sets the confirmation flag on each leg in the trip.
     * @param tripId the trip to update.
     * @param confirmationValue the answer of the traveller.
     * @param overrideResponse If true then skip the check whether an answer was already available.
     * @throws BusinessException 
     */
    public void confirmTrip(Long tripId, Boolean confirmationValue, ConfirmationReasonType reason, boolean overrideResponse) throws BusinessException {
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
        		if (leg.isConfirmationRequested()) {
	            	if (!overrideResponse && leg.getConfirmed() != null) {
	            		throw new BadRequestException("Leg has already a confirmation value: " + leg.getId());
	            	}
	            	leg.setConfirmed(confirmationValue);
	            	leg.setConfirmationReason(reason);
        		}
        	}
        	EventFireWrapper.fire(tripConfirmedEvent, new TripConfirmedEvent(tripdb));
    	}
    }

    /**
     * Sets the confirmation flag on each leg in the trip.
     * @param tripRef the trip reference to update.
     * @param bookingRef the reference to the booking 
     * @param confirmationValue the answer of the traveller.
     * @param reason the reason for the given confirmation. This is an API 
     * @param overrideResponse If true then skip the check whether an answer was already available.
     * @throws BusinessException 
     */
    public void confirmTripByTransportProvider(String tripRef, String bookingRef, 
    		Boolean confirmationValue, ConfirmationReasonType reason, boolean overrideResponse) throws BusinessException {
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
        	if (bookingRef != null) {
            	tripdb.getItinerary().findLegByBookingId(bookingRef)
            			.orElseThrow(() -> new IllegalArgumentException("No such booking on trip: " + tripRef + " " + bookingRef));
        	}
        	for (Leg leg : tripdb.getItinerary().getLegs()) {
        		if (leg.isConfirmationByProviderRequested() && (bookingRef == null || bookingRef.equals(leg.getBookingId()))) {
	            	if (!overrideResponse && leg.getConfirmedByProvider() != null) {
	            		throw new BadRequestException("Leg has already a confirmation value by provider: " + leg.getId());
	            	}
	            	leg.setConfirmedByProvider(confirmationValue);
	            	leg.setConfirmationReasonByProvider(reason);
        		}
        	}
        	EventFireWrapper.fire(tripConfirmedEvent, new TripConfirmedEvent(tripdb));
    	}
    }

    protected void updateTripState(Trip trip) throws BusinessException {
    	TripState previousState = trip.getState();
		trip.updateTripState();
		if (log.isDebugEnabled()) {
			log.debug(String.format("updateTripState %s --> %s: %s", previousState, trip.getState(), trip.toStringCompact()));
		}
       	if (trip.getState() == TripState.SCHEDULED) {
       		Duration timeLeftToDeparture = Duration.between(Instant.now(), trip.getItinerary().getDepartureTime());
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Trip %s is scheduled, time left to departure is %s", trip.getId(), timeLeftToDeparture.toString()));
    		}
        	if (! trip.isMonitored() && timeLeftToDeparture.compareTo(DEPARTING_PERIOD.plus(Duration.ofHours(2))) < 0) {
        		if (log.isDebugEnabled()) {
        			log.debug("Start monitoring trip " + trip.getId());
        		}
        		startMonitoring(trip);
        	}
       	} else if (trip.getState().isFinalState()) {
       		cancelTripTimers(trip);
       	}
    	EventFireWrapper.fire(tripStateUpdatedEvent, new TripStateUpdatedEvent(previousState, trip));
    }

    protected void updateTripAndLegState(Trip trip, TripState newState) throws BusinessException {
    	TripState previousState = trip.getState();
    	if (newState.ordinal() >= previousState.ordinal()) {
    		trip.setState(newState);
    	} else if (newState != previousState) {
    		log.warn(String.format("Trip %s: Blocked attempt to set back state from %s --> %s", trip.getId(), previousState, newState));
    	}
		trip.forceTripStateDown();
		if (log.isDebugEnabled()) {
			log.debug(String.format("updateTripAndLegState %s --> %s: %s", previousState, trip.getState(), trip.toStringCompact()));
		}
		if (trip.getState().isFinalState()) {
       		cancelTripTimers(trip);
		}
    	EventFireWrapper.fire(tripStateUpdatedEvent, new TripStateUpdatedEvent(previousState, trip));
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
//		log.debug("CollectDueTrips");
		// Get all trips that are in scheduled state and have a departure time within a certain window
		List<Trip> trips = tripDao.findMonitorableTrips(Instant.now().plus(Duration.ofHours(2).plus(DEPARTING_PERIOD)));
		for (Trip trip : trips) {
			startMonitoring(trip);
		}
	}

	protected void handleTripEvent(TripInfo tripInfo) throws BusinessException {
		if (log.isDebugEnabled()) {
			log.debug("Received trip event: " + tripInfo.toString());
		}
		Trip trip = tripDao.fetchGraph(tripInfo.tripId, Trip.DETAILED_ENTITY_GRAPH)
				.orElseThrow(() -> new IllegalArgumentException("No such trip: " + tripInfo.tripId));
		Instant now = Instant.now();
		
		switch (tripInfo.event) {
		case TIME_TO_PREPARE:
			updateTripAndLegState(trip, TripState.DEPARTING);
			timerService.createTimer(Date.from(trip.getItinerary().getDepartureTime()), 
					new TripInfo(TripMonitorEvent.TIME_TO_DEPART, trip.getId()));
			break;
		case TIME_TO_DEPART:
			updateTripAndLegState(trip, TripState.IN_TRANSIT);
			timerService.createTimer(Date.from(trip.getItinerary().getArrivalTime()), 
					new TripInfo(TripMonitorEvent.TIME_TO_ARRIVE, trip.getId()));
			break;
		case TIME_TO_ARRIVE:
			updateTripAndLegState(trip, TripState.ARRIVING);
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
			updateTripAndLegState(trip, TripState.VALIDATING);
			timerService.createTimer(Date.from(trip.getItinerary().getArrivalTime().plus(CONFIRM_PERIOD_1)), 
					new TripInfo(TripMonitorEvent.TIME_TO_CONFIRM_REMINDER, trip.getId()));
			break;
		case TIME_TO_CONFIRM_REMINDER:
			updateTripAndLegState(trip, TripState.VALIDATING);
			timerService.createTimer(Date.from(trip.getItinerary().getArrivalTime().plus(CONFIRM_PERIOD_2)), 
					new TripInfo(TripMonitorEvent.TIME_TO_COMPLETE, trip.getId()));
			break;
		case TIME_TO_COMPLETE:
			if (trip.getState() == TripState.VALIDATING) {
				EventFireWrapper.fire(tripValidationExpiredEvent, new TripValidationExpiredEvent(trip));
			}
			updateTripAndLegState(trip, TripState.COMPLETED);
			trip.setMonitored(false);
			break;
		default:
			log.warn("Don't know how to handle event: " + tripInfo.event);
			break;
		}
		
	}
	
    @Timeout
	public void onTimeout(Timer timer) {
		try {
			if (! (timer.getInfo() instanceof TripInfo)) {
				log.error("Don't know how to handle timeout: " + timer.getInfo());
				return;
			}
			TripInfo tripInfo = (TripInfo) timer.getInfo();
			handleTripEvent(tripInfo);
		} catch (BusinessException ex) {
			log.error(String.join("\n\t", ExceptionUtil.unwindException(ex)));
			log.info("Rollback status after exception: " + context.getRollbackOnly()); 
		} catch (NoSuchObjectLocalException ex) {
			log.error(String.format("Error handling timeout for %s: ", ex.toString()));
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
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("cancelTripTimers: %d timers in total active for this EJB", timers.size()));
    	}
    	int count = 0;
		for (Timer timer : timers) {
//	    	if (log.isDebugEnabled()) {
//	    		log.debug(String.format("Timer info: %s", timer.getInfo()));
//	    	}
			if ((timer.getInfo() instanceof TripInfo)) {
				TripInfo tripInfo = (TripInfo) timer.getInfo();
				if (tripInfo.tripId.equals(trip.getId())) {
					try {
						timer.cancel();
						count++;
					} catch (Exception ex) {
						log.error("Unable to cancel timer: " + ex.toString());
					}
				}
			}
		}
		if (count > 0 && log.isDebugEnabled()) {
			log.debug(String.format("Cancel %d timer(s) for trip %s", count, trip.getId()));
		}
		trip.setMonitored(false);
	}

	public List<TripInfo> listAllTripMonitorTimers() {
    	// Find all timers related to the trip manager
    	Collection<Timer> timers = timerService.getTimers();
    	return timers.stream()
    			.filter(tm -> tm.getInfo() instanceof TripInfo)
    			.map(tm -> (TripInfo) tm.getInfo())
    			.collect(Collectors.toList());
	}
	
	/**
	 * Create a map to revive the monitor. Use the event that would cause the favourable transition.
	 * Note that only trips that are monitored are considered, e.g. SCHEDULED AND monitored = true. 
	 */
	private static Map<TripState, TripMonitorEvent> tripStateToMonitorRevivalEvent = Map.ofEntries(
			new AbstractMap.SimpleEntry<>(TripState.SCHEDULED, TripMonitorEvent.TIME_TO_PREPARE),
			new AbstractMap.SimpleEntry<>(TripState.DEPARTING, TripMonitorEvent.TIME_TO_PREPARE),
			new AbstractMap.SimpleEntry<>(TripState.IN_TRANSIT, TripMonitorEvent.TIME_TO_DEPART),
			new AbstractMap.SimpleEntry<>(TripState.ARRIVING, TripMonitorEvent.TIME_TO_ARRIVE),
			new AbstractMap.SimpleEntry<>(TripState.VALIDATING, TripMonitorEvent.TIME_TO_VALIDATE),
			new AbstractMap.SimpleEntry<>(TripState.COMPLETED, TripMonitorEvent.TIME_TO_COMPLETE)
		);
	
	/**
	 * Revive the trip monitors that have been crashed due due to some unrecoverable errors.
	 */
	public void reviveTripMonitors() {
		List<TripInfo> tripInfos = listAllTripMonitorTimers();
		if (tripInfos.isEmpty()) {
			log.info("NO active trip timers");
		} else {
			log.info("Active trip timers:\n" + String.join("\n\t", 
					tripInfos.stream()
					.map(ti -> ti.toString())
					.collect(Collectors.toList()))
			);
		}		

		Set<Long> timedTripIds = tripInfos.stream()
				.map(ti -> ti.tripId)
				.collect(Collectors.toSet());
		List<Trip> monitoredTrips = tripDao.findMonitoredTrips();
		monitoredTrips.removeIf(t -> timedTripIds.contains(t.getId()));
		if (monitoredTrips.isEmpty()) {
			log.info("All required trip monitors are in place");
		} else {
			log.warn(String.format("There are %d trips without active monitoring, fixing now...", monitoredTrips.size()));
			for (Trip trip : monitoredTrips) {
				TripMonitorEvent event = tripStateToMonitorRevivalEvent.get(trip.getState());
				if (event == null) {
					log.warn(String.format("Trip %s state is %s, no suitable revival event found", trip.getId(), trip.getState()));
					// First check what is really needed before switching off the monitor 
					// trip.setMonitored(false);
			} else {
					TripInfo ti = new TripInfo(event, trip.getId());
					tripDao.detach(trip);
					try {
						handleTripEvent(ti);
					} catch (BusinessException ex) {
						log.error(String.join("\n\t", ExceptionUtil.unwindException(ex)));
					} catch (Exception ex) {
						log.error(String.format("Error reviving trip monitor: %s", ex.toString()));
					}
				}
			}
		}
	}
	
	public void updateLegPaymentState(Trip trip, Leg leg, PaymentState newState, String paymentReference) throws BusinessException {
		Trip tripdb = trip; 
		if (!tripDao.contains(trip)) {
			tripdb = tripDao.fetchGraph(trip.getId(), Trip.DETAILED_ENTITY_GRAPH)
					.orElseThrow(() -> new IllegalArgumentException("No such trip: " + trip.getId()));

		}
		Leg legdb = tripdb.getItinerary().getLegs().stream()
				.filter(lg -> lg.getId().equals(leg.getId()))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Expected to find leg " + leg.getId() + "in trip " + trip.getId()));
		legdb.setPaymentState(newState);
		legdb.setPaymentId(paymentReference);
		Optional<Leg> dueLeg =  tripdb.getItinerary().getLegs().stream()
			.filter(lg -> lg.hasFareInCredits() && lg.isPaymentDue())
			.findFirst();
		if (!dueLeg.isPresent() && trip.getState() == TripState.VALIDATING) {
			// We're done! No more due payments 
			updateTripAndLegState(trip, TripState.COMPLETED);
		}
		
	}
}
