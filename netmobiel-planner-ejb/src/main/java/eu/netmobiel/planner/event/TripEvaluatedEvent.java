package eu.netmobiel.planner.event;

import java.io.Serializable;

import eu.netmobiel.planner.model.Trip;

/**
 * This event is issued when a trip is evaluated and it is time to update the state. 
 * 
 * @author Jaap Reitsma
 *
 */
public class TripEvaluatedEvent extends BasicTripEvent implements Serializable {
	private static final long serialVersionUID = 2270398465038857971L;

	public TripEvaluatedEvent(Trip aTrip) {
    	super(aTrip);
    }

}
