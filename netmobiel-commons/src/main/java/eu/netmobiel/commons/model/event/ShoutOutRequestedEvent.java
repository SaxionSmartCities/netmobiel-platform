package eu.netmobiel.commons.model.event;

import java.io.Serializable;
import java.time.Instant;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.NetMobielUser;

/**
 * Netmobiel Shout-out event. When a traveller cannot find a connection, then a shout-out will be issued in the community.
 * 
 * @author Jaap Reitsma
 *
 */
public class ShoutOutRequestedEvent implements Serializable {
	private static final long serialVersionUID = 1846290640996282945L;

	/**
     * The Netmobiel user who is travelling. 
     */
    @NotNull
    private NetMobielUser traveller;

	/**
	 * The reference to the traveller's trip as known by the trip manager.
	 */
    @NotNull
    private String travellerTripRef;

	/**
     * Number of seats occupied by this booking 
     */
    @Positive
    @Max(99)
    private int nrSeats;

    /**
     * The intended pickup time.
     */
    @NotNull
    private Instant departureTime;
    
    /**
     * The intended drop-off time
     */
    @NotNull
    private Instant arrivalTime;

    /**
     * The pickup location of the passenger.  
     */
    @NotNull
    private GeoLocation pickup;
    
    /**
     * The drop-off location of the passenger.  
     */
    @NotNull
    private GeoLocation dropOff;

    /**
     * No-args constructor.
     */
    public ShoutOutRequestedEvent(NetMobielUser traveller, String travellerTripRef) {
    	this.traveller = traveller;
    	this.travellerTripRef = travellerTripRef;
    }

	public int getNrSeats() {
		return nrSeats;
	}

	public void setNrSeats(int nrSeats) {
		this.nrSeats = nrSeats;
	}

	public Instant getDepartureTime() {
		return departureTime;
	}

	public void setDepartureTime(Instant departureTime) {
		this.departureTime = departureTime;
	}

	public Instant getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(Instant arrivalTime) {
		this.arrivalTime = arrivalTime;
	}

	public GeoLocation getPickup() {
		return pickup;
	}

	public void setPickup(GeoLocation pickup) {
		this.pickup = pickup;
	}

	public GeoLocation getDropOff() {
		return dropOff;
	}

	public void setDropOff(GeoLocation dropOff) {
		this.dropOff = dropOff;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public NetMobielUser getTraveller() {
		return traveller;
	}

	public String getTravellerTripRef() {
		return travellerTripRef;
	}

}
