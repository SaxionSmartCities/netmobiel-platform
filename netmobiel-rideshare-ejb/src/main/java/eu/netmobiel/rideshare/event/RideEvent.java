package eu.netmobiel.rideshare.event;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideMonitorEvent;
import eu.netmobiel.rideshare.model.RideState;


/**
 * This event is issued when an ride event occurs.  
 * 
 * @author Jaap Reitsma
 *
 */
public class RideEvent extends BasicRideEvent implements Serializable {

	private static final long serialVersionUID = 8837457274309434137L;

	@NotNull
    private RideMonitorEvent event;

    @NotNull
    private RideState oldState;

    @NotNull
    private RideState newState;
    
    public RideEvent(Ride aRide, RideMonitorEvent anEvent, RideState anOldState, RideState aNewState) {
    	super(aRide);
    	this.event = anEvent;
    	this.oldState = anOldState;
    	this.newState = aNewState;
    }

	public RideMonitorEvent getEvent() {
		return event;
	}

	public RideState getOldState() {
		return oldState;
	}

	public RideState getNewState() {
		return newState;
	}

	public boolean isTransitionTo(RideState state) {
		return oldState != state && newState == state;
	}
	
	@Override
	public String toString() {
		return String.format("RideEvent [%s %s in %s --> %s]", getRide().getId(), event, oldState, newState);
	}
}
