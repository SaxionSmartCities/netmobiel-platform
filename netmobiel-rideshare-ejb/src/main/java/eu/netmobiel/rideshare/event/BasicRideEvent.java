package eu.netmobiel.rideshare.event;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import eu.netmobiel.rideshare.model.Ride;


/**
 * This is the base class for Ride events.  
 * 
 * @author Jaap Reitsma
 *
 */
public abstract class BasicRideEvent implements Serializable {

	private static final long serialVersionUID = 8837457274309434137L;
	/**
     * The ride.
     */
    @NotNull
    private Ride ride;
    
    public BasicRideEvent(Ride aRide) {
    	this.ride = aRide;
    }

	public Ride getRide() {
		return ride;
	}

	@Override
	public String toString() {
		return String.format("%s [%s]", this.getClass().getSimpleName(), ride.getId());
	}
}
