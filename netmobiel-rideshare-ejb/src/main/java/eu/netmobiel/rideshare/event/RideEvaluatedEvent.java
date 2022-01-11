package eu.netmobiel.rideshare.event;

import java.io.Serializable;

import eu.netmobiel.rideshare.model.Ride;


/**
 * This event is issued when an ride has been evaluated.  
 * 
 * @author Jaap Reitsma
 *
 */
public class RideEvaluatedEvent extends BasicRideEvent implements Serializable {

	private static final long serialVersionUID = 8837457274309434137L;

    public RideEvaluatedEvent(Ride aRide) {
    	super(aRide);
    }
}
