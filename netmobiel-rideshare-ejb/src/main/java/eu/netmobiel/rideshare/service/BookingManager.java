package eu.netmobiel.rideshare.service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
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
    private RideItineraryHelper rideItineraryHelper;
    
    @Inject @Created
    private Event<Booking> bookingCreatedEvent;
    
    @Inject @Updated
    private Event<Booking> bookingUpdatedEvent;

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
    	booking.setState(BookingState.CONFIRMED);
    	bookingDao.save(booking);
    	rideItineraryHelper.updateRideItinerary(ride);
    	bookingCreatedEvent.fire(booking);
    	return RideshareUrnHelper.createUrn(Booking.URN_PREFIX, booking.getId());
    }

    /**
     * Retrieves a booking. Anyone can read a booking, given the id.
     * @param id
     * @return
     * @throws NotFoundException
     */
    public Booking getBooking(Long id) throws NotFoundException {
    	Booking bookingdb = bookingDao.fetchGraph(id, Booking.DEEP_ENTITY_GRAPH)
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
    public void removeBooking(NetMobielUser initiator, Long bookingId, final String reason) throws NotFoundException, BadRequestException {
    	Booking bookingdb = bookingDao.fetchGraph(bookingId, Booking.SHALLOW_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such booking: " + bookingId));
		User initiatorDb = userDao.findByManagedIdentity(initiator.getManagedIdentity())
				.orElseThrow(() -> new NotFoundException("No such user: " + initiator.toString()));
		User driver = bookingdb.getRide().getDriver();
//		if (log.isDebugEnabled()) {
//			log.debug(String.format("removeBooking: Driver %s, Initiator %s, Passenger %s", 
//				driver.toString(), initiatorDb.toString(), bookingdb.getPassenger().toString()));
//		}
    	boolean cancelledByDriver = initiatorDb.equals(driver);
		if (bookingdb.getPassenger().equals(initiatorDb) || cancelledByDriver) {
	   		bookingdb.markAsCancelled(reason, cancelledByDriver);
	    	rideItineraryHelper.updateRideItinerary(bookingdb.getRide());
	    	bookingRemovedEvent.fire(bookingdb);
		} else {
    		throw new SecurityException(String.format("Removal of %s %d is only allowed by driver or passenger", Booking.class.getSimpleName(), bookingdb.getId()));
		}
    }
 
}
