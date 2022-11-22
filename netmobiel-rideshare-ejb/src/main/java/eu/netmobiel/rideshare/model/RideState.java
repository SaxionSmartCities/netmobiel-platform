package eu.netmobiel.rideshare.model;

/**
 * Trips and legs have a state when persisted.
 * 
 * @author Jaap Reitsma
 *
 */
public enum RideState {
	/**
	 * The trip is scheduled, all that needs to be prepared for has been prepared. 
	 */
	SCHEDULED("SCH"),
	/**
	 * The departure is imminent.
	 */
	DEPARTING("DPR"),
	/**
	 * The trip is in transit, i.e. the user is on the road.
	 */
	IN_TRANSIT("NTR"),
	/**
	 * The traveller should be arriving
	 */
	ARRIVING("ARR"),
	/**
	 * The trip waits for confirmation by transport provider and traveller.
	 */
	VALIDATING("VLD"),
	/**
	 * The trip is completed
	 */
	COMPLETED("CMP"),
	/**
	 * The trip has been cancelled. 
	 */
	CANCELLED("CNC");

	private String code;
	 
    private RideState(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }
    
    public boolean isPreTravelState() {
    	return this == SCHEDULED;
    }

    public boolean isPostTravelState() {
    	return this == VALIDATING || this == COMPLETED;
    }
    
    public boolean isFinalState() {
    	return this == COMPLETED || this == CANCELLED;
    }
}
