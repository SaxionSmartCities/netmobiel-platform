package eu.netmobiel.planner.event;

import java.io.Serializable;

import javax.validation.constraints.Size;

import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.Trip;

/**
 * This event is issued when a passenger cancels a booking for a leg.
 * 
 * @author Jaap Reitsma
 *
 */
public class BookingCancelledEvent extends BookingEvent implements Serializable {
	private static final long serialVersionUID = 329634709636391206L;
	/**
     * The reason for cancelling.
     */
    @Size(max = 256)
    private String cancelReason;

	public BookingCancelledEvent(Trip aTrip, Leg aLeg, String aReason) {
    	super(aTrip, aLeg);
    	this.cancelReason = aReason;
    }

	public String getCancelReason() {
		return cancelReason;
	}
	
}
