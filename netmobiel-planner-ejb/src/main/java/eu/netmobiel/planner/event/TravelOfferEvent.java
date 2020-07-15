package eu.netmobiel.planner.event;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.planner.model.Itinerary;
import eu.netmobiel.planner.model.TripPlan;

/**
 * This event is issued when someone offers a ride (as in rideshare) in respond to a shout-out.
 * 
 * @author Jaap Reitsma
 *
 */
public class TravelOfferEvent implements Serializable {
	private static final long serialVersionUID = 1846290640996282945L;

    /**
     * The traveller.
     */
    @NotNull
    private TripPlan shoutOutPlan;

    /**
     * The shout-out itinerary. 
     */
    @NotNull
    private Itinerary shoutOutItinerary;

    /**
     * The proposed plan.
     */
    @NotNull
    private TripPlan proposedPlan;

    /**
     * A reference to the driver, if known and relevant.
     */
    private String driverRef;

    /**
     * A reference to the vehicle used for the transport, if known and relevant.
     */
    private String vehicleRef;
    
    public TravelOfferEvent(TripPlan aShoutOutPlan, Itinerary aPassengerItinerary, TripPlan aProposedPlan, String aDriverRef, String aVehicleRef) {
    	this.shoutOutPlan = aShoutOutPlan;
    	this.shoutOutItinerary = aPassengerItinerary;
    	this.proposedPlan = aProposedPlan;
    	this.driverRef = aDriverRef;
    	this.vehicleRef = aVehicleRef;
    }

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public TripPlan getShoutOutPlan() {
		return shoutOutPlan;
	}

	public TripPlan getProposedPlan() {
		return proposedPlan;
	}

	public Itinerary getShoutOutItinerary() {
		return shoutOutItinerary;
	}

	public String getDriverRef() {
		return driverRef;
	}

	public String getVehicleRef() {
		return vehicleRef;
	}
    
    
}
