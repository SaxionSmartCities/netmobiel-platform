package eu.netmobiel.planner.event;

import java.io.Serializable;

import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.Trip;

/**
 * This event is issued when a passenger requests a booking for a leg. Assumption: The trip is already persisted.
 * 
 * @author Jaap Reitsma
 *
 */
public class BookingRequestedEvent extends BookingEvent implements Serializable {
	private static final long serialVersionUID = 2876634695405167386L;

	public BookingRequestedEvent(Trip aTrip, Leg aLeg) {
    	super(aTrip, aLeg);
    }

}
