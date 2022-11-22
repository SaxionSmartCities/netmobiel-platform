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
public class TripStateUpdatedEvent extends BasicTripEvent implements Serializable {
	private static final long serialVersionUID = 3219309393169307909L;

	@NotNull
    private TripState previousState;

    public TripStateUpdatedEvent(TripState aPreviousState, Trip aTrip) {
    	super(aTrip);
    	this.previousState = aPreviousState;
    }

    public TripState getPreviousState() {
		return previousState;
	}

	@Override
	public String toString() {
		return String.format("TripStateUpdatedEvent %s %s -> %s]", getTrip().getId(), previousState, getTrip().getState());
	}
}
