package eu.netmobiel.planner.event;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripMonitorEvent;


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

    public TripEvent(TripMonitorEvent anEvent, Trip aTrip) {
    	super(aTrip);
    	this.event = anEvent;
    }

	public TripMonitorEvent getEvent() {
		return event;
	}

	@Override
	public String toString() {
		return String.format("RideEvent %s %s in %s ]", getTrip().getTripRef(), event, getTrip().getState());
	}
}
