package eu.netmobiel.planner.event;

import java.io.Serializable;

import eu.netmobiel.planner.model.Trip;

/**
 * This event is issued when a trip is scheduled. 
 * 
 * @author Jaap Reitsma
 *
 */
public class TripScheduledEvent extends BasicTripEvent implements Serializable {
	private static final long serialVersionUID = 258921475228053609L;

	public TripScheduledEvent(Trip aTrip) {
    	super(aTrip);
    }

}
