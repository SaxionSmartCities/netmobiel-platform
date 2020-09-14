package eu.netmobiel.planner.event;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripState;

/**
 * This event is issued when a trip state is changed.  
 * 
 * @author Jaap Reitsma
 *
 */
public class TripStateUpdatedEvent implements Serializable {
	private static final long serialVersionUID = 8837457274309434137L;
	/**
     * The traveller.
     */
    @NotNull
    private Trip trip;
    
    @NotNull
    private TripState previousState;

    public TripStateUpdatedEvent(TripState aPreviousState, Trip aTrip) {
    	this.previousState = aPreviousState;
    	this.trip = aTrip;
    }

    public TripState getPreviousState() {
		return previousState;
	}

	public Trip getTrip() {
		return trip;
	}

	@Override
	public String toString() {
		return String.format("TripStateUpdatedEvent %s %s -> %s]", trip.getId(), previousState, trip.getState());
	}
}
