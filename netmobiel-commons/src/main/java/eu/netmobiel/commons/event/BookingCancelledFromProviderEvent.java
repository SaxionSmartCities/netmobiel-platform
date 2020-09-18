package eu.netmobiel.commons.event;

import java.io.Serializable;

import javax.validation.constraints.Size;

import eu.netmobiel.commons.model.NetMobielUser;

/**
 * Standard Netmobiel transport booking cancelling event.
 * 
 * @author Jaap Reitsma
 *
 */
public class BookingCancelledFromProviderEvent extends BookingEventBase implements Serializable {
	private static final long serialVersionUID = -6709947526702522195L;

    /**
     * The reason for cancelling.
     */
    @Size(max = 256)
    private String cancelReason;
    
    /**
     * If true then the booking was cancelled by the transport driver. This flag is added because in theory 
     * the traveller can cancel the booking through the NetMobiel client, but also through the provider's client.
     */
    private boolean cancelledByDriver;

    /**
     * No-args constructor.
     */
    public BookingCancelledFromProviderEvent(String bookingRef, NetMobielUser traveller, String travellerTripRef, 
    		String cancelReason, boolean cancelledByDriver) {
    	super(bookingRef, traveller, travellerTripRef);
    	this.cancelReason = cancelReason;
    	this.cancelledByDriver = cancelledByDriver;
    }

	public String getCancelReason() {
		return cancelReason;
	}

	public boolean isCancelledByDriver() {
		return cancelledByDriver;
	}

}
