package eu.netmobiel.planner.event;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import eu.netmobiel.planner.model.Trip;

/**
 * Basic trip event. 
 * 
 * @author Jaap Reitsma
 *
 */
public class TripEvent implements Serializable {
	private static final long serialVersionUID = 8845962032082359275L;

	/**
     * The trip.
     */
    @NotNull
    private Trip trip;
    
    public TripEvent(Trip aTrip) {
    	this.trip = aTrip;
    }

    public Trip getTrip() {
		return trip;
	}

	@Override
	public String toString() {
		return String.format("%s [%s]", this.getClass().getSimpleName(), trip.getId());
	}

}
