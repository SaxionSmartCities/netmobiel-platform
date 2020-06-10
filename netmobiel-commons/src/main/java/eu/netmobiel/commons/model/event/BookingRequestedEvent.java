package eu.netmobiel.commons.model.event;

import java.io.Serializable;
import java.time.Instant;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.NetMobielUser;

/**
 * Standard Netmobiel transport booking request.
 * 
 * @author Jaap Reitsma
 *
 */
public class BookingRequestedEvent extends BookingEventBase implements Serializable {
	private static final long serialVersionUID = 1846290640996282945L;

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
	 * The reference to the transport provider's trip (a transport provider URN).
	 */
    @NotNull
    private String providerTripRef;

    /**
     * No-args constructor.
     */
    public BookingRequestedEvent(NetMobielUser traveller, String travellerTripRef, String providerTripRef) {
    	super(null, traveller, travellerTripRef);
    	this.providerTripRef = providerTripRef;
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

	public String getProviderTripRef() {
		return providerTripRef;
	}

}
