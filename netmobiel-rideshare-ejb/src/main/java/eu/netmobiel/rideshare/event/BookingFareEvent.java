package eu.netmobiel.rideshare.event;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.Ride;


/**
 * Base class for booking events involving a fare.  
 * 
 * @author Jaap Reitsma
 *
 */
public abstract class BookingFareEvent implements Serializable {

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

    public BookingFareEvent(Ride aRide, Booking aBooking) {
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
		return String.format("BookingFareEvent %s %s]", ride.getId(), booking.getId());
	}
}
