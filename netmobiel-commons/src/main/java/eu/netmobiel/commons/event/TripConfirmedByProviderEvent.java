package eu.netmobiel.commons.event;

import java.io.Serializable;

import javax.validation.constraints.NotNull;


/**
 * This event is issued when a transport provider confirms driving the trip.  
 * 
 * @author Jaap Reitsma
 *
 */
public class TripConfirmedByProviderEvent implements Serializable {

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

    private Boolean confirmationByTransportProvider;
    
    /**
     * No-args constructor.
     */
    public TripConfirmedByProviderEvent(String bookingRef, String travellerTripRef, Boolean confirmationValue) {
    	this.bookingRef = bookingRef;
    	this.travellerTripRef = travellerTripRef;
    	this.confirmationByTransportProvider = confirmationValue;
    }

	public String getTravellerTripRef() {
		return travellerTripRef;
	}

	public String getBookingRef() {
		return bookingRef;
	}

	public Boolean getConfirmationByTransportProvider() {
		return confirmationByTransportProvider;
	}

	@Override
	public String toString() {
		return String.format("TripConfirmedByProviderEvent %s %s %s]", bookingRef, travellerTripRef, confirmationByTransportProvider);
	}
}
