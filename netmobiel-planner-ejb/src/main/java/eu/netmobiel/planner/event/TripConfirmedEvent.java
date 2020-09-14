package eu.netmobiel.planner.event;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import eu.netmobiel.planner.model.Trip;

/**
 * This event is issued when a trip execution is confirmed (either affirmative or denied) by the passenger. 
 * 
 * @author Jaap Reitsma
 *
 */
public class TripConfirmedEvent implements Serializable {
	private static final long serialVersionUID = 8837457274309434137L;
	/**
     * The traveller.
     */
    @NotNull
    private Trip trip;
    
    public TripConfirmedEvent(Trip aTrip) {
    	this.trip = aTrip;
    }

    public Trip getTrip() {
		return trip;
	}

	@Override
	public String toString() {
		return String.format("TripConfirmedEvent [%s]", trip.getId());
	}

}
