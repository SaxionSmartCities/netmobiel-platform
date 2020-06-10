package eu.netmobiel.commons.model.event;

import java.io.Serializable;

import eu.netmobiel.commons.model.NetMobielUser;

/**
 * Standard Netmobiel transport booking cancelling event.
 * 
 * @author Jaap Reitsma
 *
 */
public class BookingConfirmedEvent extends BookingEventBase implements Serializable {
	private static final long serialVersionUID = -6709947526702522195L;

    /**
     * No-args constructor.
     */
    public BookingConfirmedEvent(String bookingRef, NetMobielUser traveller, String travellerTripRef) {
    	super(bookingRef, traveller, travellerTripRef);
    }

}
