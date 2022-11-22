package eu.netmobiel.profile.model;

import java.util.Arrays;

public enum TraverseMode {
    BICYCLE("BC"), 
    BUS("BS"), 
    CAR("CR"),
    RAIL("RL"), 
    RIDESHARE("RS"), 
    WALK("WK");

	private String code;
	 
    private TraverseMode(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }

    public boolean isTransit() {
        return this == RAIL || this == BUS; 
    }

    public boolean isOnStreetNonTransit() {
        return this == WALK || this == BICYCLE || this == CAR || this == RIDESHARE;
    }
    
    public boolean isDriving() {
        return this == CAR || this == RIDESHARE;
    }

    public static boolean containsTransit(TraverseMode[] modes) {
    	return Arrays.stream(modes)
    			.filter(m -> m.isTransit())
    			.findAny()
    			.isPresent();
    }
}
