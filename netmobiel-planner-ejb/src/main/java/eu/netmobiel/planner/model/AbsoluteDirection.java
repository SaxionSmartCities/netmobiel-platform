package eu.netmobiel.planner.model;

/** 
 * An absolute cardinal or intermediate direction.
 */
public enum AbsoluteDirection {
	NORTH("N"), 	
	NORTHEAST("NE"), 	
	EAST("E"), 	
	SOUTHEAST("SE"), 	
	SOUTH("S"), 	
	SOUTHWEST("SW"), 	
	WEST("W"), 	
	NORTHWEST("NW");

	private String code;
	 
    private AbsoluteDirection(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }
}
