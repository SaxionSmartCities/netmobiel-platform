package eu.netmobiel.planner.event;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import eu.netmobiel.planner.model.Trip;

/**
 * This event is issued when a shout-out is resolved 
 * 
 * @author Jaap Reitsma
 *
 */
public class ShoutOutResolvedEvent implements Serializable {
	private static final long serialVersionUID = 743661264510405320L;

	/**
     * The new trip with the itinerary
     */
    @NotNull
    private Trip trip;

    public ShoutOutResolvedEvent(Trip aNewTrip) {
    	this.trip = aNewTrip;
    }

	public Trip getTrip() {
		return trip;
	}

}
