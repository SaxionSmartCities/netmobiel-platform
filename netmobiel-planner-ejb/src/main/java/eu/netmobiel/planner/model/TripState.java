package eu.netmobiel.planner.model;

/**
 * Trips and legs have a state when persisted.
 * 
 * @author Jaap Reitsma
 *
 */
public enum TripState {
	/**
	 * The trip is being planned, i.e. departure and destination places are known, departure or arrival time too, but not all means of transport are known yet.
	 */
	PLANNING("PLN"),
	/**
	 * The trip is being booked, seats are reserved etc., but not all confirmations have been received yet. 
	 */
	BOOKING("BKN"),
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
	 
    private TripState(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }
    
    public boolean isPreTravelState() {
    	return this == PLANNING || this == BOOKING || this == SCHEDULED;
    }

    public boolean isPostTravelState() {
    	return this == VALIDATING || this == COMPLETED;
    }
    
    public boolean isFinalState() {
    	return this == COMPLETED || this == CANCELLED;
    }
}
