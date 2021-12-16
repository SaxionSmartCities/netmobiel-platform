package eu.netmobiel.rideshare.event;

import java.io.Serializable;

import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.Ride;


/**
 * This event is issued when a booking is settled (the fare is paid or cancelled).  
 * 
 * @author Jaap Reitsma
 *
 */
public class BookingFareSettledEvent extends BookingFareEvent implements Serializable {

	private static final long serialVersionUID = 8837457274309434137L;

    public BookingFareSettledEvent(Ride aRide, Booking aBooking) {
    	super(aRide, aBooking);
    }

	@Override
	public String toString() {
		return String.format("BookingFareSettledEvent %s %s]", getRide().getId(), getBooking().getId());
	}
}
