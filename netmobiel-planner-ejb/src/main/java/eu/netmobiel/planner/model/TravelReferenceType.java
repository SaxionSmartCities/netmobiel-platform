package eu.netmobiel.planner.model;

public enum TravelReferenceType {
	/**
	 * The travel reference is a geodesic, a 'straight' line. 
	 */
	GEODESIC("GD"),
	/**
	 * The travel reference is a car (rideshare) using the street map.  
	 */
	CAR("CR"),
	/**
	 * The travel reference is some transit connection.
	 */
	TRANSIT("TR");

	private String code;
	 
    private TravelReferenceType(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }
}
