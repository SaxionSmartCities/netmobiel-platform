package eu.netmobiel.rideshare.event;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.Ride;


/**
 * This event is issued when a booking is seetled (the fare is paid or cancelled).  
 * 
 * @author Jaap Reitsma
 *
 */
public class BookingSettledEvent implements Serializable {

	private static final long serialVersionUID = 8837457274309434137L;
	/**
     * The ride.
     */
    @NotNull
    private Ride ride;
    
	/**
     * The booking.
     */
    private Booking booking;

    public BookingSettledEvent(Ride aRide, Booking aBooking) {
    	this.ride = aRide;
    	this.booking = aBooking;
    }

	public Ride getRide() {
		return ride;
	}

	public Booking getBooking() {
		return booking;
	}

	@Override
	public String toString() {
		return String.format("BookingSettledEvent %s %s]", ride.getId(), booking.getId());
	}
}
