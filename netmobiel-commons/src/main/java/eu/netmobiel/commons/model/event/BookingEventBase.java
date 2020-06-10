package eu.netmobiel.commons.model.event;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import eu.netmobiel.commons.model.NetMobielUser;

/**
 * Netmobiel booking base for defining booking events.
 * 
 * @author Jaap Reitsma
 *
 */
public abstract class BookingEventBase implements Serializable {
	private static final long serialVersionUID = -6709947526702522195L;

	/**
	 * The reference to the trveller's booking at the transport provider (a transport provider specific urn). Only know after creation of the booking.
	 */
    private String bookingRef;

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
     * No-args constructor.
     */
    public BookingEventBase(String bookingRef, NetMobielUser traveller, String travellerTripRef) {
    	this.bookingRef = bookingRef;
    	this.traveller = traveller;
    	this.travellerTripRef = travellerTripRef;
    }

	public NetMobielUser getTraveller() {
		return traveller;
	}

	public String getTravellerTripRef() {
		return travellerTripRef;
	}

	public String getBookingRef() {
		return bookingRef;
	}

}
