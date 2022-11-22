package eu.netmobiel.commons.event;

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import eu.netmobiel.commons.model.NetMobielUser;

/**
 * Standard Netmobiel transport booking cancelling event.
 * 
 * @author Jaap Reitsma
 *
 */
public class BookingCancelledFromProviderEvent implements Serializable {
	private static final long serialVersionUID = -6709947526702522195L;

	/**
	 * The reference to the traveller's booking at the transport provider (a transport provider specific urn). Only known after creation of the booking.
	 */
    private String bookingRef;

	/**
     * The Netmobiel user who is travelling. 
     */
    @NotNull
    private NetMobielUser traveller;

    /**
     * The reason for cancelling.
     */
    @Size(max = 256)
    private String cancelReason;
    
    /**
     * If true then the booking was cancelled by the transport driver. This flag is added because in theory 
     * the traveller can cancel the booking through the Netmobiel client, but also through the provider's client.
     */
    private boolean cancelledByDriver;

    /**
     * No-args constructor.
     */
    public BookingCancelledFromProviderEvent(String bookingRef, NetMobielUser traveller, 
    		String cancelReason, boolean cancelledByDriver) {
    	this.bookingRef = bookingRef;
    	this.traveller = traveller;
    	this.cancelReason = cancelReason;
    	this.cancelledByDriver = cancelledByDriver;
    }

	public String getBookingRef() {
		return bookingRef;
	}

	public NetMobielUser getTraveller() {
		return traveller;
	}

	public String getCancelReason() {
		return cancelReason;
	}

	public boolean isCancelledByDriver() {
		return cancelledByDriver;
	}

}
