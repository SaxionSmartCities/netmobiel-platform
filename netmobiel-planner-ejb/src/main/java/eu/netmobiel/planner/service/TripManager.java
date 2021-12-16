package eu.netmobiel.planner.service;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
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
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.util.Command;
import eu.netmobiel.commons.util.EventFireWrapper;
import eu.netmobiel.commons.util.ExceptionUtil;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.commons.util.ValidEjbTimer;
import eu.netmobiel.here.search.HereSearchClient;
import eu.netmobiel.planner.event.BookingCancelledEvent;
import eu.netmobiel.planner.event.BookingConfirmedEvent;
import eu.netmobiel.planner.event.BookingRequestedEvent;
import eu.netmobiel.planner.event.ShoutOutResolvedEvent;
import eu.netmobiel.planner.event.TripConfirmedEvent;
import eu.netmobiel.planner.event.TripStateUpdatedEvent;
import eu.netmobiel.planner.event.TripUnconfirmedEvent;
import eu.netmobiel.planner.event.TripValidationExpiredEvent;
import eu.netmobiel.planner.model.Itinerary;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.PaymentState;
import eu.netmobiel.planner.model.PlanType;
import eu.netmobiel.planner.model.PlannerUser;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripMonitorEvent;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.model.TripState;
import eu.netmobiel.planner.repository.ItineraryDao;
import eu.netmobiel.planner.repository.TripDao;

@Stateless
@Logging
public class TripManager {
	public static final Integer MAX_RESULTS = 10; 
	
	/**
	 * The delay before sending a invitation for a confirmation.
	 */
	private static final Duration VALIDATION_DELAY = Duration.ofMinutes(15);
	/**
	 * The maximum duration of the first confirmation period.
	 */
	private static final Duration VALIDATION_INTERVAL = Duration.ofDays(2);
	/**
	 * The duration of the pre-departing period in which the monitoring should start or have started.
	 */
	private static final Duration PRE_DEPARTING_PERIOD = Leg.DEPARTING_PERIOD.plus(Duration.ofHours(2));

	/**
	 * The maximum number of reminders to sent during validation.
	 */
	private static final int MAX_REMINDERS = 2;

	@Inject
	private HereSearchClient hereSearchClient;
	
	@Inject
    private Logger log;

    @Inject
    private TripDao tripDao;

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
    private Event<TripUnconfirmedEvent> tripUnconfirmedEvent;

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
    	if (maxResults != null && maxResults < 0) {
    		throw new BadRequestException("Constraint violation: 'maxResults' >= 0.");
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
    	return new PagedResult<>(results, maxResults, offset, totalCount);
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
    public Long createTrip(PlannerUser organizer, PlannerUser traveller, Trip trip) throws NotFoundException, BadRequestException, BusinessException {
    	trip.setOrganizer(organizer);
    	trip.setTraveller(traveller);
    	if (trip.getItineraryRef() == null) {
    		throw new BadRequestException("Specify an itinerary reference");
    	}
		// Create a trip for this itinerary
    	Itinerary it = itineraryDao
    			.loadGraph(UrnHelper.getId(Itinerary.URN_PREFIX, trip.getItineraryRef()), Itinerary.LIST_ITINERARIES_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such itinerary: " + trip.getItineraryRef()));
    	trip.setState(TripState.PLANNING);
    	trip.setItinerary(it);
    	trip.propagateTripStateDown();
    	// Load trip plan (lazy loaded, only plan itself)
    	TripPlan plan = it.getTripPlan();
    	if (plan == null) {
    		throw new NotFoundException("Itinerary has no plan attached!: " + trip.getItineraryRef());
    	}
    	
        trip.setArrivalTimeIsPinned(plan.isUseAsArrivalTime());
    	trip.setNrSeats(plan.getNrSeats());
        trip.setFrom(plan.getFrom());
        trip.setTo(plan.getTo());
        //TODO Use the local database first to lookup the coordinate. Omit for now. 
        trip.setDeparturePostalCode(hereSearchClient.getPostalCode6(trip.getFrom()));
        trip.setArrivalPostalCode(hereSearchClient.getPostalCode6(trip.getTo()));
        tripDao.save(trip);
       	tripDao.flush();
    	if (plan.getPlanType() == PlanType.SHOUT_OUT) {
    		// it was a shout-out plan. It is being resolved now. 
    		// Use the event to adjust the context of the message thread from trip plan to trip.
    		// The trip must have a database identity!
    		EventFireWrapper.fire(shoutOutResolvedEvent, new ShoutOutResolvedEvent(trip));
    	}
    	updateTripStateMachine(trip);
    	return trip.getId();
    }

    private List<Command> legStateMachineHandler(Trip trip, Leg leg) {
    	List<Command> actions = new ArrayList<>();
    	// Determine the new state(s)
    	TripState oldState = leg.getState();
    	Instant now = Instant.now();
    	// Update the legs
		leg.setState(leg.nextState(now));
		TripState newState = leg.getState();
		if (log.isDebugEnabled()) {
			log.debug(String.format("Leg SM %s --> %s: %s", oldState, newState, leg.toStringCompact()));
		}
		switch (newState) {
		case PLANNING:
			break;
		case BOOKING:
			if (oldState == TripState.PLANNING) {
				// A booking is required. Create an action to inform
   				// Use the trip as reference, we are not sure the leg ID is a stable, permanent identifier in case of an update of a trip.
   				// Add the reference to the trip of the provider, e.g. the ride in case of rideshare.
				actions.add(() -> EventFireWrapper.fire(bookingRequestedEvent, new BookingRequestedEvent(trip, leg)));
			}
			break;
		case SCHEDULED:
			if (oldState == TripState.PLANNING) {
		    	// Check for bookingID set. If so than it was a shout-out and we need to convert the PROPOSAL to a CONFIRMED booking
		    	if (leg.getBookingId() != null) {
		    		// This must be a proposed booking. Confirm it and add a trip reference.
		    		actions.add(() -> EventFireWrapper.fire(bookingConfirmedEvent, new BookingConfirmedEvent(trip, leg)));
		    	}
			}
			break;
		case DEPARTING:
			break;
		case IN_TRANSIT:
			break;
		case ARRIVING:
			break;
		case VALIDATING:
			break;
		case COMPLETED:
			break;
		case CANCELLED:
			break;
		}
		return actions;
    }

    private void updateTripStateMachine(Trip trip) throws BusinessException {
    	List<Command> actions = new ArrayList<>();
    	// Determine the new state(s)
    	TripState oldState = trip.getState();
    	Instant now = Instant.now();
    	// Update the legs and collect the action to execute
		trip.getItinerary().getLegs().forEach(lg -> actions.addAll(legStateMachineHandler(trip, lg)));
		trip.setState(trip.nextState(now));
		TripState newState = trip.getState();
		if (log.isDebugEnabled()) {
			log.debug(String.format("Trip SM %s --> %s: %s", oldState, newState, trip.toStringCompact()));
		}
    	if (newState.ordinal() < oldState.ordinal()) {
    		log.warn(String.format("Trip %s: State is set back from %s --> %s", trip.getId(), oldState, newState));
    	}
		// Execute the delayed actions.
		for (Command action : actions) {
			action.execute();
		}
		switch (newState) {
		case PLANNING:
			break;
		case BOOKING:
			break;
		case SCHEDULED:
			// Should we monitor already?
			checkToStartMonitoring(trip);
			break;
		case DEPARTING:
			break;
		case IN_TRANSIT:
			break;
		case ARRIVING:
			break;
		case VALIDATING:
       		if (!trip.isMonitored()) {
       			trip.setMonitored(true);
        		handleTripMonitorEvent(trip, TripMonitorEvent.TIME_TO_VALIDATE);
       		}
			break;
		case COMPLETED:
       		cancelTripTimers(trip);
			break;
		case CANCELLED:
       		cancelTripTimers(trip);
			break;
		}
		// Inform the observers
    	EventFireWrapper.fire(tripStateUpdatedEvent, new TripStateUpdatedEvent(oldState, trip));
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
    	Trip trip = tripDao.find(UrnHelper.getId(Trip.URN_PREFIX, tripRef))
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
    	leg.setBookingConfirmed(bookingConfirmed);
    	updateTripStateMachine(trip);
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
    	Trip trip = tripDao.find(UrnHelper.getId(Trip.URN_PREFIX, tripRef))
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
		// Cancel also the other legs
		for (Leg lg: trip.getItinerary().getLegs()) {
			removeTripLeg(trip, lg, "");
		}
		updateTripStateMachine(trip);
		return leg;
    }

    /**
     * Retrieves a trip. Anyone can read a trip, given the id. NO details are retrieved.
     * @param id the trip id
     * @return a trip object without details.
     * @throws NotFoundException No matching trip found.
     */
    public Trip getTripBasics(Long id) throws NotFoundException {
    	Trip tripdb = tripDao.find(id)
    			.orElseThrow(() -> new NotFoundException("No such trip: " + id));
    	return tripdb;
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
     * @param hard If set then set the delete flag. The trip will no longer appear in the regular listing. 
     * @throws BusinessException 
     */
    public void removeTrip(Long tripId, String reason, boolean hard) throws BusinessException {
    	Trip tripdb = tripDao.find(tripId)
    			.orElseThrow(() -> new NotFoundException("No such trip: " + tripId));
       	if (! tripdb.getState().isFinalState()) {
        	if (! Boolean.TRUE.equals(tripdb.getCancelledByProvider())) {
        		// Not already cancelled by the provider, so set my own reason for cancelling. 
        		tripdb.setCancelledByProvider(false);
        		if (reason != null && !reason.isBlank()) {
        			tripdb.setCancelReason(reason.trim());
        		}
        	}
       		if (tripdb.getItinerary().getLegs() != null) {
           		for (Leg leg : tripdb.getItinerary().getLegs()) {
           			removeTripLeg(tripdb, leg, reason);
    			}
       		}
       		updateTripStateMachine(tripdb);
       	}
       	if (hard) {
       		// Delete from the list too
       		tripdb.setDeleted(true);
       	}
    }

    private void removeTripLeg(Trip tripdb, Leg leg, String reason) throws BusinessException {
		if (leg.getState().isPreTravelState()) {
	    	if (leg.getBookingId() != null) {
	    		// There is a booking being requested or already confirmed. Cancel it.
				if (log.isDebugEnabled()) {
					log.debug("Cancelling a booking. State = " + leg.getState());
				}
				// For now use a synchronous removal
	        	EventFireWrapper.fire(bookingCancelledEvent, new BookingCancelledEvent(tripdb, leg, reason));
	    	} else {
	    		// There is a small opening between setting Booking and the setting of a booking ID.
	    		if (leg.getState() == TripState.BOOKING) {
	    			throw new IllegalStateException("Leg is in BOOKING state, but no booking reference has been set: " + leg.getId());
	    		}
	    	}
			leg.setState(TripState.CANCELLED);
    		// Not already cancelled by the provider, so set make that explicit (not null anymore)
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
     * Unconfirms (revokes confirmation) a trip and restores the state of a trip as if the validation has just started.
     * A passenger can only unconfirm if he or she was not actually charged. A RemoveException is thrown if the traveller 
     * is not allowed to undo the confirmation. 
     * This method is called by the passenger through the API.
     * The unconfirm is intended to roll-back a cancel, charge or dispute situation. The confirmation can still be altered when 
     * the payment decision is not made yet.
     * @param tripId the trip to unconfirm.
     * @throws BusinessException
     */
    public void unconfirmTrip(Long tripId) throws BusinessException {
    	Trip tripdb = tripDao.fetchGraph(tripId, Trip.MY_LEGS_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such trip: " + tripId));
    	if (tripdb.getState() != TripState.COMPLETED && tripdb.getState() != TripState.VALIDATING) { 
    		// No use to set this flag when the trip has not been finished or cancelled physically and administratively)
       		throw new BadRequestException("Unexpected state for revoking a confirmation: " + tripId + " " + tripdb.getState());
    	}
    	// Did this trip require validation anyway?
    	boolean validatable = tripdb.getItinerary().isConfirmationRequested();
    	if (!validatable) {
       		throw new BadRequestException("Trip does not require validation: " + tripdb.getTripRef());
    	}
    	// Update the leg state
    	tripdb.getItinerary().getLegs().forEach(lg -> {
        	lg.setConfirmed(null);
        	lg.setConfirmationReason(null);
    	});
    	// Reverse the global effects of the previous confirmation (all in one transaction)
    	EventFireWrapper.fire(tripUnconfirmedEvent, new TripUnconfirmedEvent(tripdb));
    	
    	// If all goes well, then restart the validation
    	restartValidation(tripdb);
	}

    /**
     * Sets the confirmation flag on each leg in the trip. The method is called by the Overseer.  
     * @param tripRef the trip reference to update.
     * @param bookingRef the reference to the booking 
     * @param confirmationValue the answer of the traveller.
     * @param reason the reason for the given confirmation. This is an API 
     * @param overrideResponse If true then skip the check whether an answer was already available.
     * @throws BusinessException 
     */
    public Trip confirmTripByTransportProvider(String tripRef, String bookingRef, 
    		Boolean confirmationValue, ConfirmationReasonType reason, boolean overrideResponse) throws BusinessException {
    	Trip tripdb = tripDao.fetchGraph(UrnHelper.getId(Trip.URN_PREFIX, tripRef), Trip.MY_LEGS_ENTITY_GRAPH)
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
        	if (bookingRef != null && tripdb.getItinerary().findLegByBookingId(bookingRef).isEmpty()) {
        		throw new IllegalArgumentException("No such booking on trip: " + tripRef + " " + bookingRef);
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
    	}
    	return tripdb;
    }

    /**
     * Clears the confirmation flag on the providers leg. This method is called by the Overseer.
     * @param tripRef the trip reference to update.
     * @param bookingRef the reference to the booking 
     * @throws BusinessException 
     */
    public Trip unconfirmTripByTransportProvider(String tripRef, String bookingRef) throws BusinessException {
    	Trip tripdb = tripDao.fetchGraph(UrnHelper.getId(Trip.URN_PREFIX, tripRef), Trip.MY_LEGS_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such trip: " + tripRef));
    	if (tripdb.getState() != TripState.COMPLETED && tripdb.getState() != TripState.VALIDATING) { 
    		// No use to set this flag when the trip has not been finished or cancelled physically and administratively)
       		throw new BadRequestException("Unexpected state for revoking a confirmation: " + tripRef + " " + tripdb.getState());
    	}
    	// Did this trip require validation anyway?
    	boolean validatable = tripdb.getItinerary().isConfirmationRequested();
    	if (!validatable) {
       		throw new BadRequestException("Trip does not require validation: " + tripdb.getTripRef());
    	}
    	// A unconfirmation from a transport provider can arrive even the trip is still in transit, but the leg should be validating of completed
    	// For now we don't care
    	Optional<Leg> providerLeg = tripdb.getItinerary().findLegByBookingId(bookingRef);
    	if (providerLeg.isEmpty()) {
    		throw new IllegalArgumentException("No such booking on trip: " + tripRef + " " + bookingRef);
    	}
    	Leg leg = providerLeg.get();
    	leg.setConfirmedByProvider(null);
    	leg.setConfirmationReasonByProvider(null);
    	return tripdb;
    }

    /**
     * Restart the validation, as if the state was ARRIVING. This call finalizes the unconfirmation process.
     * @param tripRef the trip to revalidate.
     * @throws BusinessException 
     */
    public void restartValidation(Trip trip) throws BusinessException {
    	if (trip.getState().isPostTravelState()) {
    		cancelTripTimers(trip);
    		trip.setReminderCount(0);
    		trip.setMonitored(true);
    		handleTripMonitorEvent(trip, TripMonitorEvent.TIME_TO_VALIDATE);
    	}
    }

    /**
     * Updates the payment state of the leg. This method called from the Overseer on various moments of the process.
     * @param trip
     * @param leg
     * @param newState
     * @param paymentReference
     * @throws BusinessException
     */
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
		/**
		 * X = invalid (should never happen)
		 * 
		 * Previous ---> Next ----> Action
		 * null			null		X
		 * null			Reserve		Normal operation
		 * null			Cancel		X
		 * null			Paid		X
		 * Reserve		Reserve		X
		 * Reserve		Cancel		Completed
		 * Reserve		Paid		Completed
		 * Paid			Reserve		Re-validating
		 * Paid			Cancel		X
		 * Paid			Paid		X
		 * Cancel		Reserve		Re-validating
		 * Cancel		Cancel		X
		 * Cancel		Paid		X
		 */
		legdb.setPaymentState(newState);
		legdb.setPaymentId(paymentReference);
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
		// Get all trips that are in scheduled state and have a departure time within a certain window
		try {
			List<Trip> trips = tripDao.findMonitorableTrips(Instant.now().plus(PRE_DEPARTING_PERIOD));
			for (Trip trip : trips) {
				handleTripMonitorEvent(trip, TripMonitorEvent.TIME_TO_PREPARE_MONITORING);
			}
		} catch (BusinessException ex) {
			log.error(String.join("\n\t", ExceptionUtil.unwindException(ex)));
			log.info("Rollback status after exception: " + context.getRollbackOnly()); 
		} catch (Exception ex) {
			log.error(String.format("Error handling timeout: %s", ex.toString()));
		}
	}

	protected void handleTripMonitorEvent(Trip trip, TripMonitorEvent event) throws BusinessException {
		if (trip.getState() == TripState.CANCELLED) {
			log.warn("Cannot monitor, trip has been canceled: " + trip.getId());
			return;
		}
		updateTripStateMachine(trip);
		Instant now = Instant.now();
		switch (event) {
		case TIME_TO_PREPARE_MONITORING:
			// Dummy event for easier timer startup
			checkToStartMonitoring(trip);
        	break;
		case TIME_TO_PREPARE:
			timerService.createTimer(Date.from(trip.getItinerary().getDepartureTime()), 
					new TripInfo(TripMonitorEvent.TIME_TO_DEPART, trip.getId()));
			break;
		case TIME_TO_DEPART:
			timerService.createTimer(Date.from(trip.getItinerary().getArrivalTime()), 
					new TripInfo(TripMonitorEvent.TIME_TO_ARRIVE, trip.getId()));
			break;
		case TIME_TO_ARRIVE:
			if (trip.getItinerary().isConfirmationRequested()) {
				timerService.createTimer(Date.from(trip.getItinerary().getArrivalTime().plus(VALIDATION_DELAY)), 
						new TripInfo(TripMonitorEvent.TIME_TO_VALIDATE, trip.getId()));
			} else {
				timerService.createTimer(Date.from(trip.getItinerary().getArrivalTime().plus(Leg.ARRIVING_PERIOD)), 
						new TripInfo(TripMonitorEvent.TIME_TO_COMPLETE, trip.getId()));
			}
			break;
		case TIME_TO_VALIDATE:
			timerService.createTimer(Date.from(now.plus(VALIDATION_INTERVAL)), 
					new TripInfo(nextEventAfterValidationExpiration(trip), trip.getId()));
			break;
		case TIME_TO_COMPLETE:
			if (trip.getState() == TripState.VALIDATING) {
				EventFireWrapper.fire(tripValidationExpiredEvent, new TripValidationExpiredEvent(trip));
			}
			break;
		default:
			log.warn("Don't know how to handle event: " + event);
			break;
		}
	}
	
	private static TripMonitorEvent nextEventAfterValidationExpiration(Trip ride) {
		ride.incrementReminderCount();
		return ride.getReminderCount() < MAX_REMINDERS ? 
				TripMonitorEvent.TIME_TO_VALIDATE : 
				TripMonitorEvent.TIME_TO_COMPLETE;  
	}
	
    @Timeout
	public void onTimeout(Timer timer) {
		try {
			if (! (timer.getInfo() instanceof TripInfo)) {
				log.error("Don't know how to handle timeout: " + timer.getInfo());
				return;
			}
			handleTimeout((TripInfo) timer.getInfo());
		} catch (BusinessException ex) {
			log.error(String.join("\n\t", ExceptionUtil.unwindException(ex)));
			log.info("Rollback status after exception: " + context.getRollbackOnly()); 
		} catch (NoSuchObjectLocalException ex) {
			log.error(String.format("Error handling timeout: %s", ex.toString()));
		}
	}

    protected void handleTimeout(TripInfo tripInfo) throws BusinessException {
		if (log.isDebugEnabled()) {
			log.debug("Received trip event: " + tripInfo.toString());
		}
		Trip trip = tripDao.fetchGraph(tripInfo.tripId, Trip.DETAILED_ENTITY_GRAPH)
				.orElseThrow(() -> new IllegalArgumentException("No such trip: " + tripInfo.tripId));
		handleTripMonitorEvent(trip, tripInfo.event);
    }

    private void checkToStartMonitoring(Trip trip) {
		if (trip.getState() == TripState.CANCELLED) {
			log.warn("Cannot monitor, trip has been canceled: " + trip.getId());
			return;
		}
   		Duration timeLeftToDeparture = Duration.between(Instant.now(), trip.getItinerary().getDepartureTime());
		if (log.isDebugEnabled()) {
			log.debug(String.format("Trip %s is scheduled, time left to departure is %s", trip.getId(), timeLeftToDeparture.toString()));
		}
    	if (! trip.isMonitored() && timeLeftToDeparture.compareTo(PRE_DEPARTING_PERIOD) < 0) {
    		if (log.isDebugEnabled()) {
    			log.debug("Start monitoring trip " + trip.getId());
    		}
    		trip.setMonitored(true);
    		// Should we generate multiple timer events and let the state machine decide what to do?
    		// Tested. Result: Timer events are received in random order, not in the order created.
    		// Workaround: Set the next timer successively in each event handler
    		timerService.createTimer(Date.from(trip.getItinerary().getDepartureTime().minus(Leg.DEPARTING_PERIOD)), 
    				new TripInfo(TripMonitorEvent.TIME_TO_PREPARE, trip.getId()));
    	}
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
    	ValidEjbTimer validEjbTimer = new ValidEjbTimer();
    	Collection<Timer> invalidTimers = timers.stream()
        		.filter(validEjbTimer.negate())
        		.collect(Collectors.toList());
    	invalidTimers.stream()
    		.forEach(tm -> {
    			log.info(String.format("Cancelling invalid trip timer: %s", tm.getInfo()));
    			tm.cancel();
    		});
    	timers.removeAll(invalidTimers);
    	timers.removeIf(tm -> !(tm.getInfo() instanceof TripInfo));
		if (timers.isEmpty()) {
			log.info("NO active trip timers");
		} else {
			log.info("Active trip timers:\n\t" + String.join("\n\t", 
					timers.stream()
					.map(tm -> String.format("%s %s %d %s", tm.getInfo(), tm.getNextTimeout(), tm.getTimeRemaining(), tm.isPersistent()))
					.collect(Collectors.toList()))
			);
		}		
    	return timers.stream()
    			.map(tm -> (TripInfo) tm.getInfo())
    			.collect(Collectors.toList());
	}
	
	/**
	 * Create a map to revive the monitor. Use the event that would cause the favourable transition.
	 * Note that only trips that are monitored are considered, e.g. SCHEDULED AND monitored = true. 
	 */
	private static Map<TripState, TripMonitorEvent> tripStateToMonitorRevivalEvent = Map.ofEntries(
			new AbstractMap.SimpleEntry<>(TripState.SCHEDULED, TripMonitorEvent.TIME_TO_PREPARE_MONITORING),
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
					try {
						TripInfo ti = new TripInfo(event, trip.getId());
						tripDao.detach(trip);
						handleTimeout(ti);
					} catch (BusinessException ex) {
						log.error(String.join("\n\t", ExceptionUtil.unwindException(ex)));
					} catch (Exception ex) {
						log.error(String.format("Error reviving trip monitor: %s", ex.toString()));
					}
				}
			}
		}
	}
	
	public Optional<GeoLocation> findNextMissingPostalCode() {
		Optional<Trip> t = tripDao.findFirstTripWithoutPostalCode();
		GeoLocation loc = null;
		if (t.isPresent()) {
			if (t.get().getDeparturePostalCode() == null) {
				loc = t.get().getFrom();
			} else {
				loc = t.get().getTo();
			}
		}
		return Optional.ofNullable(loc);
	}

	public int assignPostalCode(GeoLocation location, String postalCode) {
		int affectedRows = 0;
		// Now assign all rides with same departure location to this postal code
		affectedRows += tripDao.updateDeparturePostalCode(location, postalCode);
		// And assign all rides with same arrival location to same postal code
		affectedRows += tripDao.updateArrivalPostalCode(location, postalCode);
		return affectedRows;
	}

}
