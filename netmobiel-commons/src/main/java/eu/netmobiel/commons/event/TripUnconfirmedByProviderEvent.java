package eu.netmobiel.commons.event;

import java.io.Serializable;

import javax.validation.constraints.NotNull;


/**
 * This event is issued when a transport provider unconfirms driving the trip.  
 * 
 * @author Jaap Reitsma
 *
 */
public class TripUnconfirmedByProviderEvent implements Serializable {

	private static final long serialVersionUID = 8837457274309434137L;
    
	/**
	 * The reference to the traveller's booking at the transport provider (a transport provider specific urn). 
	 */
    private String bookingRef;

	/**
	 * The reference to the traveller's trip as known by the trip manager.
	 */
    @NotNull
    private String travellerTripRef;

    /**
     * No-args constructor.
     */
    public TripUnconfirmedByProviderEvent(String bookingRef, String travellerTripRef) {
    	this.bookingRef = bookingRef;
    	this.travellerTripRef = travellerTripRef;
    }

    public String getTravellerTripRef() {
		return travellerTripRef;
	}

	public String getBookingRef() {
		return bookingRef;
	}

	@Override
	public String toString() {
		return String.format("TripUnconfirmedByProviderEvent %s %s]", bookingRef, travellerTripRef);
	}
}
