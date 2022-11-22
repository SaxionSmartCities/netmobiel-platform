package eu.netmobiel.planner.event;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.Trip;

/**
 * This event is issued when a passenger confirms a booking proposal for a leg. Assumption: The leg is already persisted.
 * 
 * @author Jaap Reitsma
 *
 */
public class BookingEvent extends BasicTripEvent implements Serializable {
	private static final long serialVersionUID = -8453852001191953795L;
	/**
     * The leg involved in the booking.
     */
    @NotNull
    private Leg leg;
    
    public BookingEvent(Trip aTrip, Leg aLeg) {
    	super(aTrip);
    	this.leg = aLeg;
    }

    public Leg getLeg() {
		return leg;
	}

    @Override
	public String toString() {
		return String.format("%s [%s %s]", this.getClass().getSimpleName(), getTrip().getId(), getLeg().getId());
	}

}
