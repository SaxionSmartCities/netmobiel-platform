package eu.netmobiel.planner.event;

import java.io.Serializable;

import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.Trip;

/**
 * This event is issued when a booking reference is assigned to a leg.
 * 
 * @author Jaap Reitsma
 *
 */
public class BookingAssignedEvent extends BookingEvent implements Serializable {
	private static final long serialVersionUID = 8837457274309434137L;
    
    public BookingAssignedEvent(Trip aTrip, Leg aLeg) {
    	super(aTrip, aLeg);
    }

}
