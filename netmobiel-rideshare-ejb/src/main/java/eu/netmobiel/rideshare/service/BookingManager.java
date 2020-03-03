package eu.netmobiel.rideshare.service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.BasicUser;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.BookingState;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.User;
import eu.netmobiel.rideshare.repository.BookingDao;
import eu.netmobiel.rideshare.repository.RideDao;
import eu.netmobiel.rideshare.util.RideshareUrnHelper;

@Stateless
@Logging
public class BookingManager {

    @SuppressWarnings("unused")
	@Inject
	private Logger log;
	@Inject
	private RideDao rideDao;
	@Inject
	private BookingDao bookingDao;
    @Inject
    private UserManager userManager;

    public List<Booking> listMyBookings() {
    	User caller = userManager.findCallingUser();
    	List<Booking> bookings = Collections.emptyList();
    	if (caller != null) {
    		bookings = bookingDao.findByPassenger(caller, false, null);
    	}
    	return bookings;
    	
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
    public String createBooking(String rideRef, BasicUser traveller, 
    		GeoLocation pickupLocation, GeoLocation dropOffLocation, Integer nrSeats) throws CreateException, NotFoundException {
		User travellerUser = userManager.register(traveller);
    	Long rid = RideshareUrnHelper.getId(Ride.URN_PREFIX, rideRef);
		Ride ride = rideDao.find(rid)
    			.orElseThrow(() -> new NotFoundException("Ride not found: " + rideRef));
		Booking booking = new Booking(ride, travellerUser, pickupLocation, dropOffLocation, nrSeats);
    	booking.setState(BookingState.CONFIRMED);
    	bookingDao.save(booking);
    	return RideshareUrnHelper.createUrn(Booking.URN_PREFIX, booking.getId());
    }

    /**
     * Create a booking for the calling user and assign it to a ride. 
     * @param rideId The ride to assign the booking to
     * @param booking the booking
     * @return A booking ID
     * @throws CreateException on error.
     * @throws NotFoundException if the ride cannot be found.
     */
    public Long createBooking(Long rideId, Booking booking) throws CreateException, NotFoundException {
		User caller = userManager.registerCallingUser();
    	Ride ride = rideDao.find(rideId)
    			.orElseThrow(NotFoundException::new);
    	booking.setRide(ride);
    	booking.setPassenger(caller);
    	booking.setState(BookingState.CONFIRMED);
    	bookingDao.save(booking);
    	return booking.getId();
    }

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
    public void removeBooking(Long bookingId, final String reason) throws NotFoundException {
    	Booking bookingdb = bookingDao.find(bookingId)
    			.orElseThrow(NotFoundException::new);
		User caller = userManager.findCallingUser();
		User driver = bookingdb.getRide().getRideTemplate().getDriver();
		if (Objects.equals(caller.getId(), bookingdb.getPassenger().getId()) || Objects.equals(caller.getId(), driver.getId())) {
	    	boolean cancelledByDriver = Objects.equals(caller.getId(), driver.getId());
	   		bookingdb.markAsCancelled(reason, cancelledByDriver);
		} else {
    		throw new SecurityException(String.format("Removal of %s %d is not allowed by calling user", Booking.class.getSimpleName(), bookingdb.getId()));
		}
    }
 
}
