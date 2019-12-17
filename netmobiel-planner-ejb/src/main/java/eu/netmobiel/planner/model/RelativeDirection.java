package eu.netmobiel.planner.model;

/**
 * Represents a turn direction, relative to the current heading.
 * 
 * CIRCLE_CLOCKWISE and CIRCLE_CLOCKWISE are used to represent traffic circles. 
 * 
 */
public enum RelativeDirection {
	DEPART("DP"), 	
	HARD_LEFT("HLF"),
	LEFT("LF"), 	
	SLIGHTLY_LEFT("SLF"), 	
	CONTINUE("CNT"), 	
	SLIGHTLY_RIGHT("SRG"), 	
	RIGHT("RG"), 	
	HARD_RIGHT("HRG"), 	
	CIRCLE_CLOCKWISE("CW"), 	
	CIRCLE_COUNTERCLOCKWISE("CCW"), 	
	ELEVATOR("ELV"), 	
	UTURN_LEFT("ULF"), 	
	UTURN_RIGHT("URG");

	private String code;
	 
    private RelativeDirection(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }
}
