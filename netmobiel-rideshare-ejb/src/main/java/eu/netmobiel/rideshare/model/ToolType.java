package eu.netmobiel.rideshare.model;

/**
 * The tool types used in Netmobiel for planning.
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
	 * The Rideshare in Netmobiel 
	 */
	NETMOBIEL_RIDESHARE("NRS"),
	/**
	 * Manually crafted, for testing etc. 
	 */
	MANUAL("MAN");
;

	private String code;
	 
    private ToolType(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }
}
