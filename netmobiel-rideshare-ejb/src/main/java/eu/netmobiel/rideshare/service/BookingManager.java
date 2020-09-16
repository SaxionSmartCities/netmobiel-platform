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
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.event.BookingCancelledFromProviderEvent;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.BookingState;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.User;
import eu.netmobiel.rideshare.repository.BookingDao;
import eu.netmobiel.rideshare.repository.RideDao;
import eu.netmobiel.rideshare.repository.UserDao;
import eu.netmobiel.rideshare.util.RideshareUrnHelper;

@Stateless
@Logging
public class BookingManager {
	public static final Integer MAX_RESULTS = 10; 
	public static final boolean AUTO_CONFIRM_BOOKING = true; 

	@SuppressWarnings("unused")
	@Inject
	private Logger log;
	@Inject
	private RideDao rideDao;
	@Inject
	private BookingDao bookingDao;
    @Inject
    private UserDao userDao;
    
    @Inject
    private Event<BookingCancelledFromProviderEvent> bookingCancelledEvent;

    @Inject @Updated
    private Event<Ride> staleItineraryEvent;

    @Inject @Created
    private Event<Booking> bookingCreatedEvent;

    @Inject @Removed
    private Event<Booking> bookingRemovedEvent;

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
    	User passenger= userDao.find(userId)
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
    			results = bookingDao.fetch(bookingIds.getData(), Booking.DEEP_ENTITY_GRAPH);
    		}
    	}
    	return new PagedResult<Booking>(results, maxResults, offset, totalCount);
    }

    /**
     * Create a booking for a user. 
     * @param rideRef The ride reference to assign the booking to
     * @param traveller the identity of the traveller
     * @param pickupLocation the location to pickup the traveller
     * @param dropOffLocation the location to drop-off the traveller
     * @param nrSeats
     * @return A booking reference
     * @throws CreateException on error.
     * @throws NotFoundException if the ride cannot be found.
     */
    public String createBooking(String rideRef, NetMobielUser traveller, Booking booking) 
    		throws CreateException, NotFoundException, BadRequestException {
    	Long rid = RideshareUrnHelper.getId(Ride.URN_PREFIX, rideRef);
		Ride ride = rideDao.find(rid)
    			.orElseThrow(() -> new NotFoundException("Ride not found: " + rideRef));
		if (traveller.getManagedIdentity() == null) {
			throw new CreateException("Traveller identity is mandatory");
		}
    	User passenger = userDao.findByManagedIdentity(traveller.getManagedIdentity())
				.orElse(null);
    	if (passenger == null) {
    		passenger = new User(traveller);
    		userDao.save(passenger);
    	}
    	if (ride.getBookings().stream().filter(b -> !b.isDeleted()).collect(Collectors.counting()) > 0) {
    		throw new CreateException("Ride has already one booking");
    	}
    	ride.addBooking(booking);
		booking.setPassenger(passenger);
		if (booking.getState() != BookingState.PROPOSED) {
			booking.setState(BookingState.REQUESTED);
		}
    	bookingDao.save(booking);
    	bookingDao.flush();
    	String bookingRef = RideshareUrnHelper.createUrn(Booking.URN_PREFIX, booking.getId());
		if (booking.getState() == BookingState.REQUESTED) {
			if (AUTO_CONFIRM_BOOKING) {
				booking.setState(BookingState.CONFIRMED);
				// Update itinerary of the driver
		    	staleItineraryEvent.fire(booking.getRide());
			} else {
				throw new IllegalStateException("Unexpected booking state transition, support auto confirm only!");
			}
		}
		// Inform driver about requested booking or confirmed booking
		bookingCreatedEvent.fire(booking);
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

    /**
     * Removes a booking. A booking can be cancelled by the passenger or by the driver. A booking is not really removed from
     * the database, but its state is set to cancelled. 
     * @param bookingId the booking to cancel
     * @param reason An optional reason
     * @throws NotFoundException if the booking is not found in the database.
     */
    public void removeBooking(String bookingRef, final String reason, Boolean cancelledByDriver, boolean cancelledFromRideshare) throws NotFoundException, BadRequestException {
    	Long bookingId = UrnHelper.getId(Booking.URN_PREFIX, bookingRef);
    	Booking bookingdb = bookingDao.loadGraph(bookingId, Booking.SHALLOW_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such booking: " + bookingId));
   		bookingdb.markAsCancelled(reason, cancelledByDriver);
   		if (cancelledFromRideshare) {
   			// The driver of passenger has cancelled the ride or the booking through the rideshare API. 
   			// The Trip Manager has to know about it.
			BookingCancelledFromProviderEvent bce = new BookingCancelledFromProviderEvent(bookingRef, 
					bookingdb.getPassenger(), bookingdb.getPassengerTripRef(), reason, cancelledByDriver);
			// For now use a synchronous removal
			bookingCancelledEvent.fire(bce);						
   		}
    	staleItineraryEvent.fire(bookingdb.getRide());
    	if (! cancelledByDriver) {
    		// Allow a notification to be sent to the driver
    		bookingRemovedEvent.fire(bookingdb);
    	}
    }

    /**
     * Observes the (soft) removal of rides and handles the cancellation of the attached bookings. 
     * Booking that are already cancelled are ignored. The initiator of the removal of the booking is assumed 
     * to be the driver, because only the driver can remove a ride. 
     * The scenario where an administrator removes a ride is not fully supported.
     * @param ride the ride being removed. The ride is already marked as (soft) deleted.
     */
    public void onRideRemoved(@Observes(during = TransactionPhase.IN_PROGRESS) @Removed Ride ride) {
    	ride.getBookings().stream()
    	.filter(b -> ! b.isDeleted())
    	.forEach(b -> {
    		b.markAsCancelled(ride.getCancelReason(), true);	
   			// The driver has cancelled the ride. 
   			// The Trip Manager has to know about it.
			BookingCancelledFromProviderEvent bce = new BookingCancelledFromProviderEvent(b.getBookingRef(), b.getPassenger(), b.getPassengerTripRef(),
					ride.getCancelReason(), true);
			// For now use a synchronous removal
			bookingCancelledEvent.fire(bce);						
    	});
    }

    /**
     * Confirms an earlier booking. 
     * @param id the booking to conform
     * @throws NotFoundException if the booking was not found.
     */
    public void confirmBooking(Long id, String passengerTripRef) throws NotFoundException {
    	Booking b = bookingDao.loadGraph(id, Booking.SHALLOW_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such booking: " + id));
    	if (b.getState() != BookingState.PROPOSED && b.getState() != BookingState.REQUESTED) {
    		log.warn(String.format("Booking %d has an unexpected booking state at confirmation: %s", id, b.getState().toString()));
    		throw new IllegalStateException("Unexpected booking state: " + b.getBookingRef() + " " + b.getState());
    	}
    	b.setState(BookingState.CONFIRMED);
    	b.setPassengerTripRef(passengerTripRef);
		// Update itinerary of the driver
    	Ride r = rideDao.loadGraph(b.getRide().getId(), Ride.DETAILS_WITH_LEGS_ENTITY_GRAPH).orElseThrow(() -> new IllegalStateException());
    	staleItineraryEvent.fire(r);
		// Inform driver about confirmed booking
		bookingCreatedEvent.fire(b);
    }

}
