package eu.netmobiel.commons.event;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

/**
 * This event is issued when a trip execution is confirmed (either affirmative or denied) by the passenger. 
 * 
 * @author Jaap Reitsma
 *
 */
public class TripValidationEvent implements Serializable {
	private static final long serialVersionUID = 2270398465038857971L;

	/**
     * The trip reference.
     */
    @NotNull
    private String tripId;

    private boolean finalOrdeal = false;
	
	public TripValidationEvent(String aTripId, boolean aFinalOrdeal) {
		this.tripId = aTripId;
    	this.finalOrdeal = aFinalOrdeal;
    }

	public String getTripId() {
		return tripId;
	}

	public boolean isFinalOrdeal() {
		return finalOrdeal;
	}
}
