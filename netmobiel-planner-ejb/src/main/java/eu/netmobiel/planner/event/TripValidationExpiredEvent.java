package eu.netmobiel.planner.event;

import java.io.Serializable;

import eu.netmobiel.planner.model.Trip;

/**
 * This event is issued when a trip validation has expired. 
 * 
 * @author Jaap Reitsma
 *
 */
public class TripValidationExpiredEvent extends BasicTripEvent implements Serializable {
	private static final long serialVersionUID = 258921475228053609L;

	public TripValidationExpiredEvent(Trip aTrip) {
    	super(aTrip);
    }

}
