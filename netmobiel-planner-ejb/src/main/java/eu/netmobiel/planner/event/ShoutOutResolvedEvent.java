package eu.netmobiel.planner.event;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import eu.netmobiel.planner.model.Itinerary;

/**
 * This event is issued when a shout-out is resolved 
 * 
 * @author Jaap Reitsma
 *
 */
public class ShoutOutResolvedEvent implements Serializable {
	private static final long serialVersionUID = 743661264510405320L;

	/**
     * The selected itinerary
     */
    @NotNull
    private Itinerary selectedItinerary;

    public ShoutOutResolvedEvent(Itinerary aSelectedItinerary) {
    	this.selectedItinerary = aSelectedItinerary;
    }

	public Itinerary getSelectedItinerary() {
		return selectedItinerary;
	}

}
