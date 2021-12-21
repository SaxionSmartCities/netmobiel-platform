package eu.netmobiel.rideshare.service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.annotation.Created;
import eu.netmobiel.commons.annotation.Removed;
import eu.netmobiel.commons.annotation.Updated;
import eu.netmobiel.commons.event.BookingCancelledFromProviderEvent;
import eu.netmobiel.commons.event.TripConfirmedByProviderEvent;
import eu.netmobiel.commons.event.TripUnconfirmedByProviderEvent;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.ConfirmationReasonType;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.PaymentState;
import eu.netmobiel.commons.util.EventFireWrapper;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.BookingState;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideState;
import eu.netmobiel.rideshare.model.RideshareUser;
import eu.netmobiel.rideshare.repository.BookingDao;
import eu.netmobiel.rideshare.repository.RideDao;
import eu.netmobiel.rideshare.repository.RideshareUserDao;

@Stateless
@Logging
public class BookingManager {
	public static final Integer MAX_RESULTS = 10; 
	public static final boolean AUTO_CONFIRM_BOOKING = true; 

	@Inject
	private Logger log;
	@Inject
	private RideDao rideDao;
	@Inject
	private BookingDao bookingDao;
    @Inject
    private RideshareUserDao userDao;
    
    @Inject
    private RideMonitor rideMonitor;

    @Inject
    private Event<BookingCancelledFromProviderEvent> bookingCancelledEvent;

    @Inject @Updated
    private Event<Ride> staleItineraryEvent;

    @Inject @Created
    private Event<Booking> bookingCreatedEvent;

    @Inject @Removed
    private Event<Booking> bookingRemovedEvent;

    @Inject
    private Event<TripConfirmedByProviderEvent> transportProviderConfirmedEvent;

	@Inject
    private Event<TripUnconfirmedByProviderEvent> transportProviderUnconfirmedEvent;

    /**
     * Search for bookings.
     * @param userId
     * @param since
     * @param until
     * @param maxResults
     * @param offset
     * @return
     * @throws NotFoundException
     * @throws BadRequestException
     */
    public PagedResult<Booking> listBookings(Long userId, Instant since, Instant until, Integer maxResults, Integer offset) throws NotFoundException, BadRequestException {
    	if (until != null && since != null && ! until.isAfter(since)) {
    		throw new BadRequestException("Constraint violation: The 'until' date must be greater than the 'since' date.");
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
    	RideshareUser passenger= userDao.find(userId)
				.orElseThrow(() -> new NotFoundException("No such user: " + userId));
        List<Booking> results = Collections.emptyList();
        Long totalCount = 0L;
        // Assure user exists in database
		PagedResult<Long> prs = bookingDao.findByPassenger(passenger, since, until, false, 0, 0);
		totalCount = prs.getTotalCount();
    	if (totalCount > 0 && maxResults > 0) {
    		// Get the actual data
    		PagedResult<Long> bookingIds = bookingDao.findByPassenger(passenger, since, until, false, maxResults, offset);
    		if (!bookingIds.getData().isEmpty()) {
    			results = bookingDao.loadGraphs(bookingIds.getData(), Booking.DEEP_ENTITY_GRAPH, Booking::getId);
    		}
    	}
    	return new PagedResult<>(results, maxResults, offset, totalCount);
    }

    /**
     * Create a booking for a user. 
     * @param rideRef The ride reference to assign the booking to
     * @param traveller the identity of the traveller
     * @param pickupLocation the location to pickup the traveller
     * @param dropOffLocation the location to drop-off the traveller
     * @param nrSeats
     * @return A booking reference
     * @throws BusinessException 
     */
    public String createBooking(String rideRef, NetMobielUser traveller, Booking booking) throws BusinessException {
    	Long rid = UrnHelper.getId(Ride.URN_PREFIX, rideRef);
		Ride ride = rideDao.fetchGraph(rid, Ride.LIST_RIDES_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("Ride not found: " + rideRef));
		if (traveller.getManagedIdentity() == null) {
			throw new CreateException("Traveller identity is mandatory");
		}
    	RideshareUser passenger = userDao.findByManagedIdentity(traveller.getManagedIdentity())
				.orElseGet(() -> userDao.save(new RideshareUser(traveller)));
    	if (ride.getBookings().stream().filter(b -> !b.isDeleted()).collect(Collectors.counting()) > 0) {
    		throw new CreateException(String.format("Ride %s has already a booking", ride.getId()));
    	}
    	if (ride.getDriver().equals(passenger)) {
    		throw new CreateException(String.format("Driver of Ride %s cannot be a Passenger too!", ride.getId()));
    	}
    	ride.addBooking(booking);
		booking.setPassenger(passenger);
		if (booking.getState() != BookingState.PROPOSED) {
			booking.setState(BookingState.REQUESTED);
		}
    	bookingDao.save(booking);
    	bookingDao.flush();
    	String bookingRef = UrnHelper.createUrn(Booking.URN_PREFIX, booking.getId());
		if (booking.getState() == BookingState.REQUESTED) {
			if (AUTO_CONFIRM_BOOKING) {
				booking.setState(BookingState.CONFIRMED);
			} else {
				throw new IllegalStateException("Unexpected booking state transition, support auto confirm only!");
			}
		}
		// Update itinerary of the driver
    	EventFireWrapper.fire(staleItineraryEvent, booking.getRide());
		// Inform driver about the new booking booking. The handler decides what to do, given the booking state
		EventFireWrapper.fire(bookingCreatedEvent, booking);
    	return bookingRef;
    }

    /**
     * Retrieves a booking. Anyone can read a booking, given the id.
     * @param id
     * @return
     * @throws NotFoundException
     */
    public Booking getBooking(Long id) throws NotFoundException {
    	Booking bookingdb = bookingDao.loadGraph(id, Booking.DEEP_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such booking: " + id));
    	return bookingdb;
    }

    private Booking getShallowBooking(String bookingRef)  throws NotFoundException, BadRequestException {
    	Long bookingId = UrnHelper.getId(Booking.URN_PREFIX, bookingRef);
    	return bookingDao.loadGraph(bookingId, Booking.SHALLOW_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such booking: " + bookingId));
    }

    /**
     * Removes a booking. A booking can be cancelled by the passenger or by the driver. A booking is not really removed from
     * the database, but its state is set to cancelled. 
     * @param bookingId the booking to cancel
     * @param reason An optional reason
     * @throws BusinessException 
     */
    public void removeBooking(String bookingRef, final String reason, Boolean cancelledByDriver, boolean cancelledFromRideshare) throws BusinessException {
    	Booking b = getShallowBooking(bookingRef);
   		b.markAsCancelled(reason, cancelledByDriver);
   		if (cancelledFromRideshare) {
   			// The driver of passenger has cancelled the ride or the booking through the rideshare API. 
   			// The Trip Manager has to know about it.
			BookingCancelledFromProviderEvent bce = new BookingCancelledFromProviderEvent(bookingRef, 
					b.getPassenger(), reason, cancelledByDriver);
			// For now use a synchronous removal
			EventFireWrapper.fire(bookingCancelledEvent, bce);
   		}
		EventFireWrapper.fire(staleItineraryEvent, b.getRide());
    	if (! cancelledByDriver) {
    		// Allow a notification to be sent to the driver
			EventFireWrapper.fire(bookingRemovedEvent, b);
    	}
    }

    /**
     * Observes the (soft) removal of rides and handles the cancellation of the attached bookings. 
     * Booking that are already cancelled are ignored. The initiator of the removal of the booking is assumed 
     * to be the driver, because only the driver can remove a ride. 
     * The scenario where an administrator removes a ride is not fully supported.
     * @param ride the ride being removed. The ride is already marked as (soft) deleted.
     * @throws BusinessException 
     */
    public void onRideRemoved(@Observes(during = TransactionPhase.IN_PROGRESS) @Removed Ride ride) throws BusinessException {
    	List<Booking> bookingsToCancel = ride.getBookings().stream()
    	.filter(b -> ! b.isDeleted())
    	.collect(Collectors.toList());
    	for (Booking b : bookingsToCancel) {
    		b.markAsCancelled(ride.getCancelReason(), true);	
   			// The driver has cancelled the ride. 
   			// The Trip Manager has to know about it.
			BookingCancelledFromProviderEvent bce = new BookingCancelledFromProviderEvent(b.getUrn(), 
					b.getPassenger(), ride.getCancelReason(), true);
			// For now use a synchronous removal
			EventFireWrapper.fire(bookingCancelledEvent, bce);
    	};
    }

    /**
     * Confirms an earlier booking. 
     * @param id the booking to conform
     * @param passengerTripRef The r3eference to the passenger's trip.
     * @param the fare of the passenger
     * @throws BusinessException 
     */
    public void confirmBooking(Long id, String passengerTripRef, int fareinCredits) throws BusinessException {
    	Booking b = bookingDao.loadGraph(id, Booking.SHALLOW_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such booking: " + id));
    	if (b.getState() != BookingState.PROPOSED && b.getState() != BookingState.REQUESTED) {
    		log.warn(String.format("Booking %d has an unexpected booking state at confirmation: %s", id, b.getState().toString()));
    		throw new IllegalStateException("Unexpected booking state: " + b.getUrn() + " " + b.getState());
    	}
    	b.setFareInCredits(fareinCredits);
    	b.setState(BookingState.CONFIRMED);
    	b.setPassengerTripRef(passengerTripRef);
		// Update itinerary of the driver
    	Ride r = rideDao.loadGraph(b.getRide().getId(), Ride.DETAILS_WITH_LEGS_ENTITY_GRAPH).orElseThrow(() -> new IllegalStateException());
		EventFireWrapper.fire(staleItineraryEvent, r);
		// Inform driver about confirmed booking
		EventFireWrapper.fire(bookingCreatedEvent, b);
    }

    /**
     * Sets the confirmation flag on the ride and sends a event to inform that the provider has confirmed the ride.
     * @param rideId the ride to update.
     * @throws BusinessException 
     */
    public void confirmTravelling(Long bookingId, Boolean confirmationValue, ConfirmationReasonType reason, boolean overwrite) throws BusinessException {
    	Booking b = bookingDao.loadGraph(bookingId, Booking.RIDE_AND_DRIVER_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such booking: " + bookingId));
    	if (b.getState() != BookingState.CONFIRMED) {
    		throw new BadRequestException("The booking is not confirmed at all! " + bookingId);
    	}
    	if (b.getRide().getState() != RideState.COMPLETED) { 
    		// No use to set this flag when the trip has already been finished physically and administratively)
        	if (b.getRide().getState() != RideState.VALIDATING) {
        		throw new BadRequestException("Unexpected state for a confirmation: " + b.getRide().getUrn() + " " + b.getRide().getState());
        	}
	    	if (confirmationValue == null) {
	    		throw new BadRequestException("An empty confirmation value is not allowed");
	    	}
	    	if (b.getConfirmed() != null && !overwrite) {
	    		throw new BadRequestException("Booking has already a confirmation value: " + bookingId);
	    	}
	    	b.setConfirmed(confirmationValue);
	    	b.setConfirmationReason(reason);
    		EventFireWrapper.fire(transportProviderConfirmedEvent, 
    				new TripConfirmedByProviderEvent(b.getUrn(), b.getPassengerTripRef(), confirmationValue, reason));
    	}
    }

    /**
     * Unconfirms (revokes confirmation) a ride and restores the state of a ride as if the validation has just started.
     * This method is called by the driver through the API.
     * The unconfirm is intended to roll-back a cancel, charge or dispute situation. The confirmation can still be altered when 
     * the payment decision is not made yet. The driver cannot uncancel a fare, only a charge.
     * @param rideId the ride to unconfirm.
     * @throws BusinessException
     */
    public void unconfirmTravelling(Long bookingId) throws BusinessException {
    	Booking b = bookingDao.loadGraph(bookingId, Booking.RIDE_AND_DRIVER_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such booking: " + bookingId));
    	if (b.getState() != BookingState.CONFIRMED) {
    		throw new BadRequestException("The booking is not confirmed at all! " + bookingId);
    	}
    	if (b.getRide().getState() != RideState.COMPLETED && b.getRide().getState() != RideState.VALIDATING) { 
    		// Not allowed when the ride has not been finished or cancelled physically and administratively)
       		throw new BadRequestException("Unexpected ride state for revoking a confirmation: " + b.getRide().getUrn() + " " + b.getRide().getState());
    	}
    	b.setConfirmed(null);
    	b.setConfirmationReason(null);
    	// Inform the Overseer
		EventFireWrapper.fire(transportProviderUnconfirmedEvent, new TripUnconfirmedByProviderEvent(b.getUrn(), b.getPassengerTripRef()));
		rideMonitor.restartValidation(b.getRide());
    }
    
    /**
     * Signals the settlement of the booking fare. This method is called from the Overseer.
     * @param rideRef the ride involved
     * @param bookingRef the booking involved.
     * @throws BusinessException
     */
    public void informBookingFareSettled(String rideRef, String bookingRef, PaymentState paymentState, String paymentId) throws BusinessException {
    	Booking bookingdb = getShallowBooking(bookingRef);
    	bookingdb.setPaymentState(paymentState);
    	bookingdb.setPaymentId(paymentId);
		rideMonitor.evaluateStateMachineDelayed(bookingdb.getRide());
    }

    /**
     * Signals the reset of the booking fare payment. No update of ride state necessary yet. 
     * Will be done later.
     * @param rideRef the ride involved
     * @param bookingRef the booking involved.
     * @throws BusinessException
     */
    public void informBookingFareReset(String rideRef, String bookingRef) throws BusinessException {
    	Booking bookingdb = getShallowBooking(bookingRef);
    	bookingdb.setPaymentState(null);
    	bookingdb.setPaymentId(null);
    }

    /**
     * Restart the validation, as if the validation of the booking was not yet started.
     * @param bookingRef the booking to revalidate.
     * @throws BusinessException 
     */
    public void restartValidation(String bookingRef) throws BusinessException {
    	Booking bookingdb = getShallowBooking(bookingRef);
		rideMonitor.restartValidation(bookingdb.getRide());
    }


}
