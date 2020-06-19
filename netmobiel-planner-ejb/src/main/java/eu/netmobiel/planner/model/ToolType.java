package eu.netmobiel.planner.model;

/**
 * The tool types used in NetMobiel for planning.
 * 
 * @author Jaap Reitsma
 *
 */
public enum ToolType {
	/**
	 * The OpenTripPlanner (V1.x) 
	 */
	OPEN_TRIP_PLANNER("OTP"),
	/**
	 * The Rideshare in NetMobiel 
	 */
	NETMOBIEL_RIDESHARE("NRS");
;

	private String code;
	 
    private ToolType(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }
}
