package eu.netmobiel.rideshare.event;

import java.io.Serializable;

import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.Ride;


/**
 * This event is issued when a booking is reset (the fare is reserved again).  
 * 
 * @author Jaap Reitsma
 *
 */
public class BookingFareResetEvent extends BookingFareEvent implements Serializable {

	private static final long serialVersionUID = 8837457274309434137L;

    public BookingFareResetEvent(Ride aRide, Booking aBooking) {
    	super(aRide, aBooking);
    }

	@Override
	public String toString() {
		return String.format("BookingFareResetEvent %s %s]", getRide().getId(), getBooking().getId());
	}
}
