package eu.netmobiel.planner.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.event.TripValidationEvent;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.RemoveException;
import eu.netmobiel.commons.exception.UpdateException;
import eu.netmobiel.commons.model.ConfirmationReasonType;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.PaymentState;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.util.Command;
import eu.netmobiel.commons.util.EventFireWrapper;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.here.search.HereSearchClient;
import eu.netmobiel.planner.event.BookingAssignedEvent;
import eu.netmobiel.planner.event.BookingCancelledEvent;
import eu.netmobiel.planner.event.BookingConfirmedEvent;
import eu.netmobiel.planner.event.BookingRequestedEvent;
import eu.netmobiel.planner.event.ShoutOutResolvedEvent;
import eu.netmobiel.planner.event.TripConfirmedEvent;
import eu.netmobiel.planner.event.TripEvaluatedEvent;
import eu.netmobiel.planner.event.TripUnconfirmedEvent;
import eu.netmobiel.planner.model.Itinerary;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.ModalityUsage;
import eu.netmobiel.planner.model.PlanType;
import eu.netmobiel.planner.model.PlannerUser;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.model.TripState;
import eu.netmobiel.planner.repository.ItineraryDao;
import eu.netmobiel.planner.repository.LegDao;
import eu.netmobiel.planner.repository.TripDao;

/**
 * Manager of the trips. This EJB contains mostly the presentation layer facing methods for CRUD operations on Trip objects. A few methods are 
 * callbacks from other EJBs to update specific parts of a trip.
 * 
 * @author Jaap Reitsma
 *
 */
@Stateless
@Logging
public class TripManager {
	public static final Integer MAX_RESULTS = 10; 
	
    @Inject
	private HereSearchClient hereSearchClient;
	
	@Inject
    private Logger log;

    @Inject
    private TripDao tripDao;

    @Inject
    private ItineraryDao itineraryDao;
    
    @Inject
    private LegDao legDao;

    @Inject
    private TripMonitor tripMonitor;
    
    @Inject
    private Event<BookingRequestedEvent> bookingRequestedEvent;

    @Inject
    private Event<BookingConfirmedEvent> bookingConfirmedEvent;

    @Inject
    private Event<BookingCancelledEvent> bookingCancelledEvent;

    @Inject
    private Event<ShoutOutResolvedEvent> shoutOutResolvedEvent;

    @Inject
    private Event<TripValidationEvent> tripValidationEvent;
    
    @Inject
    private Event<TripConfirmedEvent> tripConfirmedEvent;

    @Inject
    private Event<TripUnconfirmedEvent> tripUnconfirmedEvent;

    @Inject
    private Event<BookingAssignedEvent> bookingAssignedEvent;

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
    	trip.setItinerary(it);
    	trip.setState(TripState.PLANNING);
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
        Trip tripdb = tripDao.save(trip);
       	tripDao.flush();
   		// Update the state before doing any booking stuff
    	tripMonitor.updateTripStateMachine(tripdb);
    	List<Command> actions = new ArrayList<>();
    	if (plan.getPlanType() == PlanType.SHOUT_OUT) {
    		// it was a shout-out plan. It is being resolved now. 
    		// Use the event to adjust the context of the message thread from trip plan to trip.
    		// The trip must have a database identity!
    		// Make it synchronous, just to be sure for the conversation 
    		EventFireWrapper.fire(shoutOutResolvedEvent, new ShoutOutResolvedEvent(tripdb));
    	}

    	// Should we do any booking actions? Try to do them here in a single transaction
   		for (Leg leg : tripdb.getItinerary().getLegs()) {
   			final Trip theTrip = tripdb;
	    	// Check for bookingID set. If so than it was a shout-out and we need to convert the PROPOSAL to a CONFIRMED booking
	    	if (leg.getBookingId() != null) {
	    		// This must be a proposed booking from a shout-out. Confirm it. Add a trip reference.
		    	// Check for bookingID set. If so than it was a shout-out and we need to convert the PROPOSAL to a CONFIRMED booking
	    		actions.add(() -> EventFireWrapper.fire(bookingConfirmedEvent, new BookingConfirmedEvent(theTrip, leg)));
	    	} else if (leg.isBookingRequired()) {
   	    		// Ok, we need to take additional steps before the leg can be scheduled. Start a booking procedure.
   				// Use the trip as reference, we are not sure the leg ID is a stable, permanent identifier in case of an update of a trip.
   				// Add the reference to the trip of the provider, e.g. the ride in case of rideshare.
   				actions.add(() -> EventFireWrapper.fire(bookingRequestedEvent, new BookingRequestedEvent(theTrip, leg)));
	    	} else {
   	    		// If no booking is required then no further action is required.
	    	}
       	}
		for (Command action : actions) {
			action.execute();
		}
		// Booking might have been completed now update the state
		// Refresh the trip first; could we use tripDao.refresh?
    	Trip tripdb2 = tripDao.loadGraph(tripdb.getId(), Trip.DETAILED_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such trip: " + tripdb.getId()));
    	tripMonitor.updateTripStateMachine(tripdb2);
    	return tripdb.getId();
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
		tripMonitor.updateTripStateMachine(trip);
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

    public Trip getTrip(String tripRef) throws NotFoundException, BadRequestException {
    	return  tripDao.loadGraph(UrnHelper.getId(Trip.URN_PREFIX, tripRef), Trip.MY_LEGS_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such trip: " + tripRef));
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
       		tripMonitor.updateTripStateMachine(tripdb);
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
    	Trip tripdb = getTrip(tripId);
    	if (confirmationValue == null) {
    		throw new BadRequestException("An empty confirmation value is not allowed");
    	}
    	if (tripdb.getState() == TripState.COMPLETED) {
    		// No use to set this flag when the trip has already been finished physically and administratively)
    		log.warn("Cannot confirm, trip has already completed: " + tripId);
    	} else {
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
	            	EventFireWrapper.fire(tripConfirmedEvent, new TripConfirmedEvent(tripdb, leg));
        		}
        	}
        	// The trip state is not changed
        	// Perhaps the validation is complete now? Evaluate it (asynchronous)
        	EventFireWrapper.fire(tripValidationEvent, new TripValidationEvent(tripdb.getTripRef(), false));
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
    	Trip tripdb = getTrip(tripId);
    	for (Leg leg: tripdb.getItinerary().findLegsToConfirm()) {
        	if (leg.getPaymentState() != PaymentState.CANCELLED) {
        		throw new RemoveException("You have not cancelled payment, therefore you cannot roll back the validation: " + tripdb.getTripRef());
        	}
    	}
    	if (tripdb.getState() != TripState.COMPLETED) { 
    		// No use to set this flag when the trip has not been finished or cancelled physically and administratively)
       		throw new BadRequestException("Unexpected state for revoking a confirmation: " + tripId + " " + tripdb.getState());
    	}
    	// Did this trip require validation anyway?
    	boolean validatable = tripdb.getItinerary().isConfirmationRequested();
    	if (!validatable) {
       		throw new BadRequestException("Trip does not require validation: " + tripdb.getTripRef());
    	}

    	// Reverse the global effects of the previous confirmation (all in one transaction)
    	EventFireWrapper.fire(tripUnconfirmedEvent, new TripUnconfirmedEvent(tripdb));
	}

    /**
     * Search for a trip that have no postal code assigned to departure or arrival location.
     * @return
     */
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

    /**
     * Assigns the given postal code to all trips with the same GeoLocations.
     * This is a maintenance method, not intended for use by the presentation layer. 
     * @param location the geolocation 
     * @param postalCode the postal code to assign
     * @return the number of trips altered.
     */
	public int assignPostalCode(GeoLocation location, String postalCode) {
		int affectedRows = 0;
		// Now assign all rides with same departure location to this postal code
		affectedRows += tripDao.updateDeparturePostalCode(location, postalCode);
		// And assign all rides with same arrival location to same postal code
		affectedRows += tripDao.updateArrivalPostalCode(location, postalCode);
		return affectedRows;
	}

    /**
     * Report on the usage of the modalities of a user as a passenger. The count is the number of a completed trip in which a modality is used
     * at least once. A multi-legged trip with a single modality counts as one.
     * @param user The user to report about
     * @return A list of ModalityCount objects.
     */
	public List<ModalityUsage> reportTripModalityUseAsPassenger(PlannerUser user) throws BusinessException {
		return legDao.reportModalityUsageAsPassenger(user);
	}

    /**
     * Report on the usage of the modalities of a user as a driver. The count is the number of a completed trips in which a modality is used
     * at least once. A multi-legged trip with a single modality counts as one.
     * The user is identified as the driver by the keycloak identity in the driverId field of a leg.
     * @param user The user to report about
     * @return A list of ModalityCount objects.
     */
	public List<ModalityUsage> reportTripModalityUseAsDriver(PlannerUser user) throws BusinessException {
		return legDao.reportModalityUsageAsDriver(user);
	}

    /**
     * Update the state machine. For maintenance and development.
     * @param tripId the trip
     */
    public void updateStateMachine(Long tripId) {
    	tripMonitor.updateStateMachine(tripId);
    }
    
	/**********************************************/
	/**********   CALLBACK METHODS  ***************/
	/**********************************************/
	
    /**
     * Assign a booking reference to the leg with the specified transport provider tripId. This method
     * is called from the Overseer.  
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
    	EventFireWrapper.fire(bookingAssignedEvent, new BookingAssignedEvent(trip, leg));    
    }

    /**
     * Sets the provider confirmation flag on each leg in the trip. The method is called by the Overseer.  
     * @param tripRef the trip reference to update.
     * @param bookingRef the reference to the booking 
     * @param confirmationValue the answer of the traveller.
     * @param reason the reason for the given confirmation. This is an API 
     * @param overrideResponse If true then skip the check whether an answer was already available.
     * @throws BusinessException 
     */
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void afterConfirmTripByTransportProvider(String tripRef, String bookingRef, 
    		Boolean confirmationValue, ConfirmationReasonType reason, boolean overrideResponse) throws BusinessException {
    	Trip tripdb = getTrip(tripRef);
    	if (confirmationValue == null) {
    		throw new BadRequestException("An empty confirmation value is not allowed: " + tripRef);
    	}
    	if (bookingRef == null) {
    		throw new BadRequestException("Transport provider must pass a valid booking reference: " + tripRef);
    	}
    	// A confirmation from a transport provider can arrive even the passenger trip is still in transit. The passenger's leg should already 
    	// be in validating state.
    	if (tripdb.getState() != TripState.COMPLETED) { 
    		// No use to set this flag when the trip has already been finished physically and administratively)
        	Optional<Leg> optBookedLeg = tripdb.getItinerary().findLegByBookingId(bookingRef);
        	if (optBookedLeg.isEmpty()) {
        		log.warn("No such booking on trip: " + tripRef + " " + bookingRef);
        	} else {
        		Leg leg = optBookedLeg.get(); 
            	if (!overrideResponse && leg.getConfirmedByProvider() != null) {
            		log.warn("Leg has already a confirmation value by provider: " + leg.getId());
            	} else {
                	leg.setConfirmedByProvider(confirmationValue);
                	leg.setConfirmationReasonByProvider(reason);
                	// The trip state is not altered 
            	}
        	}
    	}
    }

    /**
     * Clears the confirmation flags on this trip. This method is called by the Overseer.
     * @param tripRef the trip reference to update.
     * @throws BusinessException 
     */
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void resetValidation(Trip tripdb) throws BusinessException {
    	if (tripdb.getState() != TripState.COMPLETED && tripdb.getState() != TripState.VALIDATING) { 
    		// No use to set this flag when the trip has not been finished or cancelled physically and administratively)
       		throw new BadRequestException("Unexpected state for revoking a confirmation: " + tripdb.getTripRef()+ " " + tripdb.getState());
    	}
    	tripdb.getItinerary().findLegsToConfirm().forEach(leg -> {
    		leg.setConfirmed(null);
    		leg.setConfirmationReason(null);
    		leg.setConfirmedByProvider(null);
    		leg.setConfirmationReasonByProvider(null);
    		leg.setState(TripState.ARRIVING);
    	});
    	tripMonitor.restartValidation(tripdb);
    }

    /**
     * Updates the payment state of the leg. This method called from the Overseer on various moments of the process.
     * @param trip
     * @param leg
     * @param newState
     * @param paymentReference
     * @throws BusinessException
     */
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
	public void updateLegPaymentState(Trip trip, Leg leg, PaymentState newState, String paymentReference) throws BusinessException {
		leg.setPaymentState(newState);
		leg.setPaymentId(paymentReference);
		// No need to update the trip state yet, it will be done by one of the confirmation call (passenger or driver)
	}

    /**
     * Listener for evaluating the trip. Only evaluate after a successful transaction, otherwise it has no use.
     * @param event
     * @throws BusinessException
     */
    public void onTripEvaluation(@Observes(during = TransactionPhase.AFTER_SUCCESS) TripEvaluatedEvent event) {
    	tripMonitor.updateStateMachine(event.getTrip().getId());
    }
    
    /**
     * Listener for completing a booking. Only executed after a successful transaction, otherwise it has no use.
     * @param event
     * @throws BusinessException
     */
    public void onBookingAssigned(@Observes(during = TransactionPhase.AFTER_SUCCESS) BookingAssignedEvent event) {
    	tripMonitor.updateStateMachine(event.getTrip().getId());
    }
}
