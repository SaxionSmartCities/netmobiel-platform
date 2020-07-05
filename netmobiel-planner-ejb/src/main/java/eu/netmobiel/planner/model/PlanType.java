package eu.netmobiel.planner.model;

/**
 * The type of the trip plan created by NetMobiel.
 * 
 * @author Jaap Reitsma
 *
 */
public enum PlanType {
	/**
	 * The regular plan created by the multi-modal planner of NetMobiel 
	 */
	REGULAR("REG"),
	/**
	 * The Shout-Out plan, i.e., a request to the community for transport.  
	 */
	SHOUT_OUT("SHO"),
	/**
	 * A reference plan by a driver to resolve a Shout-Out, a potential solution.  
	 */
	SHOUT_OUT_SOLUTION("SOS");
;

	private String code;
	 
    private PlanType(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }
}
