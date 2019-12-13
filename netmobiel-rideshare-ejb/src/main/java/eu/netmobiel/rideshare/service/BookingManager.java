package eu.netmobiel.rideshare.service;

import java.util.Collections;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.ObjectNotFoundException;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.BookingState;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.User;
import eu.netmobiel.rideshare.repository.BookingDao;
import eu.netmobiel.rideshare.repository.RideDao;

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
     * Create a booking for the calling user and assign it to a ride. 
     * @param rideId The ride to assign the booking to
     * @param booking the booking
     * @return A booking ID
     * @throws CreateException on error.
     * @throws ObjectNotFoundException if the ride cannot be found.
     */
    public Long createBooking(Long rideId, Booking booking) throws CreateException, ObjectNotFoundException {
		User caller = userManager.registerCallingUser();
    	Ride ride = rideDao.find(rideId)
    			.orElseThrow(ObjectNotFoundException::new);
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
     * @throws ObjectNotFoundException
     */
    public Booking getBooking(Long id) throws ObjectNotFoundException {
    	Booking bookingdb = bookingDao.find(id)
    			.orElseThrow(ObjectNotFoundException::new);
    	return bookingdb;
    }

    /**
     * Removes a booking. A booking can be cancelled by the passenger or by the driver. A booking is not really removed from
     * the database, but its state is set to cancelled. 
     * @param bookingId the booking to cancel
     * @param reason An optional reason
     * @throws ObjectNotFoundException if the booking is not found in the database.
     */
    public void removeBooking(Long bookingId, final String reason) throws ObjectNotFoundException {
    	Booking bookingdb = bookingDao.find(bookingId)
    			.orElseThrow(ObjectNotFoundException::new);
    	@SuppressWarnings("unused")
		User caller = userManager.registerCallingUser();
    	//TODO Fixed cancelled by driver
    	boolean cancelledByDriver = false;
    	userManager.checkOwnership(bookingdb.getPassenger(), Booking.class.getSimpleName());
   		bookingdb.markAsCancelled(reason, cancelledByDriver);
    }
 
}
