package eu.netmobiel.planner.model;

public enum TripState {
	/**
	 * A possible itinerary is returned from the planner.
	 */
	ITINERARY("ITN"),
	/**
	 * A trip is being planned, i.e. departure and destination places are known, departure or arrival time too, but not all means of transport are known yet.
	 */
	PLANNING("PLN"),
	/**
	 * A trip is being booked, seats are reserved etc., but not all confirmations have been received yet. 
	 */
	BOOKING("BKN"),
	/**
	 * A trip is scheduled, all that needs to be prepared for has been prepared. 
	 */
	SCHEDULED("SCH"),
	/**
	 * A trip is in transit, i.e. the user is on the road.
	 */
	IN_TRANSIT("NTR"),
	/**
	 * A trip is completed
	 */
	COMPLETED("CMP"),
	/**
	 * A trip has been cancelled. 
	 */
	CANCELLED("CNC");

	private String code;
	 
    private TripState(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }
}
