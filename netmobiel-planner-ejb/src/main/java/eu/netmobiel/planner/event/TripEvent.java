package eu.netmobiel.planner.event;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripMonitorEvent;
import eu.netmobiel.planner.model.TripState;


/**
 * This event is issued when an ride event occurs.  
 * 
 * @author Jaap Reitsma
 *
 */
public class TripEvent extends BasicTripEvent implements Serializable {

	private static final long serialVersionUID = 8837457274309434137L;
    
    @NotNull
    private TripMonitorEvent event;

    @NotNull
    private TripState oldState;

    @NotNull
    private TripState newState;
    
    public TripEvent(Trip aTrip, TripMonitorEvent anEvent, TripState anOldState, TripState aNewState) {
    	super(aTrip);
    	this.event = anEvent;
    	this.oldState = anOldState;
    	this.newState = aNewState;
    }

	public TripMonitorEvent getEvent() {
		return event;
	}

	public TripState getOldState() {
		return oldState;
	}

	public TripState getNewState() {
		return newState;
	}

	public boolean isTransitionTo(TripState state) {
		return oldState != state && newState == state;
	}
	
	@Override
	public String toString() {
		return String.format("TripEvent [%s %s in %s --> %s]", getTrip().getId(), event, oldState, newState);
	}
}
