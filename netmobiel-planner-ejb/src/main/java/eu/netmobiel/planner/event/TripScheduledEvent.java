package eu.netmobiel.planner.event;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import eu.netmobiel.planner.model.Trip;

/**
 * This event is issued when a trip is scheduled. 
 * 
 * @author Jaap Reitsma
 *
 */
public class TripScheduledEvent implements Serializable {
	private static final long serialVersionUID = 8837457274309434137L;
	/**
     * The traveller.
     */
    @NotNull
    private Trip trip;
    
    public TripScheduledEvent(Trip aTrip) {
    	this.trip = aTrip;
    }

    public Trip getTrip() {
		return trip;
	}

}
