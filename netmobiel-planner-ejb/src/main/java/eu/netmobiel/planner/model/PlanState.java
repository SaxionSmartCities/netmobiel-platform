package eu.netmobiel.planner.model;

/**
 * The state of the trip plan created by Netmobiel. A trip plan is also a shout-out.
 * 
 * @author Jaap Reitsma
 *
 */
public enum PlanState {
	/**
	 * The plan is open for suggestions (i.e., itineraries offered by drivers) 
	 */
	OPEN("OP"),
	/**
	 * The plan is finalized. For regular plans this is the standard state, for a shout-out it means that the passenger
	 * has made a choice.  
	 */
	FINAL("FN"),
	/**
	 * The plan (shout-out) is cancelled by the passenger.
	 */
	CANCELLED("CN");

	private String code;
	 
    private PlanState(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }
}
