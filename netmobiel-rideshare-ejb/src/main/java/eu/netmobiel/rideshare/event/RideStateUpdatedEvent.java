package eu.netmobiel.rideshare.event;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideState;


/**
 * This event is issued when a trip state is changed.  
 * 
 * @author Jaap Reitsma
 *
 */
public class RideStateUpdatedEvent implements Serializable {

	private static final long serialVersionUID = 8837457274309434137L;
	/**
     * The traveller.
     */
    @NotNull
    private Ride ride;
    
    @NotNull
    private RideState previousState;

    public RideStateUpdatedEvent(RideState aPreviousState, Ride aRide) {
    	this.previousState = aPreviousState;
    	this.ride = aRide;
    }

    public RideState getPreviousState() {
		return previousState;
	}

	public Ride getRide() {
		return ride;
	}

	@Override
	public String toString() {
		return String.format("RideStateUpdatedEvent %s %s -> %s]", ride.getId(), previousState, ride.getState());
	}
}
