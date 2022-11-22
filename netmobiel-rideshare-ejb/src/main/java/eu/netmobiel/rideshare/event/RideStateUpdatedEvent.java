package eu.netmobiel.rideshare.event;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideState;


/**
 * This event is issued when an ride event occurs.  
 * 
 * @author Jaap Reitsma
 *
 */
public class RideStateUpdatedEvent extends BasicRideEvent implements Serializable {

	private static final long serialVersionUID = 8837457274309434137L;

	@NotNull
    private RideState previousState;

    public RideStateUpdatedEvent(RideState aPreviousState, Ride aRide) {
    	super(aRide);
    	this.previousState = aPreviousState;
    }

    public RideState getPreviousState() {
		return previousState;
	}

	@Override
	public String toString() {
		return String.format("RideStateUpdatedEvent [%s %s -> %s]", getRide().getId(), previousState, getRide().getState());
	}
}
