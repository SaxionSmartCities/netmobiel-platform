package eu.netmobiel.rideshare.service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.util.Logging;
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
    private Event<Ride> rideUpdatedEvent;
    
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
    	if (since == null) {
    		since = Instant.now();
    	}
    	if (until != null && since != null && ! until.isAfter(since)) {
    		throw new BadRequestException("Constraint violation: The 'until' date must be greater than the 'since' date.");
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
    			results = bookingDao.fetch(bookingIds.getData(), null);
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
    public String createBooking(String rideRef, NetMobielUser traveller, 
    		GeoLocation pickupLocation, GeoLocation dropOffLocation, Integer nrSeats) throws CreateException, NotFoundException {
    	Long rid = RideshareUrnHelper.getId(Ride.URN_PREFIX, rideRef);
		Ride ride = rideDao.find(rid)
    			.orElseThrow(() -> new NotFoundException("Ride not found: " + rideRef));
    	User passenger = userDao.findByManagedIdentity(traveller.getManagedIdentity())
				.orElse(null);
    	if (passenger == null) {
    		passenger = new User(traveller);
    		userDao.save(passenger);
    	}
    	if (ride.getBookings().stream().filter(b -> !b.isDeleted()).collect(Collectors.counting()) > 0) {
    		throw new CreateException("Ride has already one booking");
    	}
		Booking booking = new Booking(ride, passenger, pickupLocation, dropOffLocation, nrSeats);
    	booking.setState(BookingState.CONFIRMED);
    	bookingDao.save(booking);
    	rideUpdatedEvent.fire(ride);
    	return RideshareUrnHelper.createUrn(Booking.URN_PREFIX, booking.getId());
    }

//    /**
//     * Create a booking for the calling user and assign it to a ride. 
//     * @param rideId The ride to assign the booking to
//     * @param booking the booking
//     * @return A booking ID
//     * @throws CreateException on error.
//     * @throws NotFoundException if the ride cannot be found.
//     */
//    public Long createBooking(Long rideId, Booking booking) throws CreateException, NotFoundException {
//		User caller = userManager.registerCallingUser();
//    	Ride ride = rideDao.find(rideId)
//    			.orElseThrow(NotFoundException::new);
//    	booking.setRide(ride);
//    	booking.setPassenger(caller);
//    	booking.setState(BookingState.CONFIRMED);
//    	bookingDao.save(booking);
//    	return booking.getId();
//    }

    /**
     * Retrieves a booking. Anyone can read a booking, given the id.
     * @param id
     * @return
     * @throws NotFoundException
     */
    public Booking getBooking(Long id) throws NotFoundException {
    	Booking bookingdb = bookingDao.find(id)
    			.orElseThrow(NotFoundException::new);
    	return bookingdb;
    }

    /**
     * Removes a booking. A booking can be cancelled by the passenger or by the driver. A booking is not really removed from
     * the database, but its state is set to cancelled. 
     * @param bookingId the booking to cancel
     * @param reason An optional reason
     * @throws NotFoundException if the booking is not found in the database.
     */
    public void removeBooking(User initiator, Long bookingId, final String reason) throws NotFoundException {
    	Booking bookingdb = bookingDao.find(bookingId)
    			.orElseThrow(NotFoundException::new);
		User initiatorDb = userDao.find(initiator.getId())
				.orElseThrow(() -> new NotFoundException("No such user: " + initiator.toString()));
		User driver = bookingdb.getRide().getDriver();
    	boolean cancelledByDriver = initiatorDb.equals(driver);
		if (initiatorDb.equals(bookingdb.getPassenger()) || cancelledByDriver) {
	   		bookingdb.markAsCancelled(reason, cancelledByDriver);
	    	rideUpdatedEvent.fire(bookingdb.getRide());
		} else {
    		throw new SecurityException(String.format("Removal of %s %d is only allowed by drive or passenger", Booking.class.getSimpleName(), bookingdb.getId()));
		}
    }
 
}
