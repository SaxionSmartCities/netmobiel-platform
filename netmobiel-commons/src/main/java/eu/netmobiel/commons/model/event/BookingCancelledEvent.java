package eu.netmobiel.commons.model.event;

import java.io.Serializable;

import javax.validation.constraints.Size;

import eu.netmobiel.commons.model.NetMobielUser;

/**
 * Standard Netmobiel transport booking cancelling event.
 * 
 * @author Jaap Reitsma
 *
 */
public class BookingCancelledEvent extends BookingEventBase implements Serializable {
	private static final long serialVersionUID = -6709947526702522195L;

    /**
     * The reason for cancelling.
     */
    @Size(max = 256)
    private String cancelReason;
    
    /**
     * If true then the booking was cancelled by the transport driver.
     */
    private boolean cancelledByDriver;

    /**
     * If true then the booking was cancelled by the transport provider service.
     */
    private boolean cancelledFromTransportProvider;

    /**
     * No-args constructor.
     */
    public BookingCancelledEvent(String bookingRef, NetMobielUser traveller, String travellerTripRef, 
    		String cancelReason, boolean cancelledByDriver, boolean cancelledFromTransportProvider) {
    	super(bookingRef, traveller, travellerTripRef);
    	this.cancelReason = cancelReason;
    	this.cancelledByDriver = cancelledByDriver;
    	this.cancelledFromTransportProvider = cancelledFromTransportProvider;
    }

	public String getCancelReason() {
		return cancelReason;
	}

	public boolean isCancelledByDriver() {
		return cancelledByDriver;
	}

	public boolean isCancelledFromTransportProvider() {
		return cancelledFromTransportProvider;
	}

}
