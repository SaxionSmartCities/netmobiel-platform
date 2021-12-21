package eu.netmobiel.rideshare.event;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideMonitorEvent;


/**
 * This event is issued when an ride event occurs.  
 * 
 * @author Jaap Reitsma
 *
 */
public class RideEvent implements Serializable {

	private static final long serialVersionUID = 8837457274309434137L;
	/**
     * The ride.
     */
    @NotNull
    private Ride ride;
    
    @NotNull
    private RideMonitorEvent event;

    public RideEvent(RideMonitorEvent anEvent, Ride aRide) {
    	this.event = anEvent;
    	this.ride = aRide;
    }

	public RideMonitorEvent getEvent() {
		return event;
	}

	public Ride getRide() {
		return ride;
	}

	@Override
	public String toString() {
		return String.format("RideEvent %s %s in %s ]", ride.getUrn(), event, ride.getState());
	}
}
