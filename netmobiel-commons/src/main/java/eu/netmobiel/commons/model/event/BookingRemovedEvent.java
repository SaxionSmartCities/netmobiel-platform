package eu.netmobiel.commons.model.event;

import java.io.Serializable;

import eu.netmobiel.commons.model.NetMobielUser;

/**
 * Netmobiel booking removed event.
 * 
 * @author Jaap Reitsma
 *
 */
public class BookingRemovedEvent extends BookingEventBase implements Serializable {
	private static final long serialVersionUID = -6709947526702522195L;

    /**
     * No-args constructor.
     */
    public BookingRemovedEvent(String bookingRef, NetMobielUser traveller, String travellerTripRef) {
    	super(bookingRef, traveller, travellerTripRef);
    }

}
