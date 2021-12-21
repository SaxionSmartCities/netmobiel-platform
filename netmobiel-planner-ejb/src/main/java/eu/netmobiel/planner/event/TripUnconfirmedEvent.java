package eu.netmobiel.planner.event;

import java.io.Serializable;

import eu.netmobiel.planner.model.Trip;

/**
 * This event is issued when a trip execution is unconfirmed by the passenger. 
 * 
 * @author Jaap Reitsma
 *
 */
public class TripUnconfirmedEvent extends BasicTripEvent implements Serializable {
	private static final long serialVersionUID = 2270398465038857971L;

	public TripUnconfirmedEvent(Trip aTrip) {
    	super(aTrip);
    }

}
