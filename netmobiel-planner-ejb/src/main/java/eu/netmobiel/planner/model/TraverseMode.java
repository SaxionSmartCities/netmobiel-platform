package eu.netmobiel.planner.model;

import java.util.Arrays;

public enum TraverseMode {
    AIRPLANE("AP"),
    BICYCLE("BC"), 
    BUS("BS"), 
    CABLE_CAR("CC"), 
    CAR("CR"),
    FERRY("FR"),
    FUNICULAR("FC"),
    GONDOLA("GD"), 
    LEG_SWITCH("LS"),
    RAIL("RL"), 
    RIDESHARE("RS"), 
    SUBWAY("SW"), 
    TRAM("TM"), 
    TRANSIT("TS"), 
    WALK("WK");

	private String code;
	 
    private TraverseMode(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }

    public boolean isTransit() {
        return this == TRAM || this == SUBWAY || this == RAIL || this == BUS || this == FERRY
                || this == CABLE_CAR || this == GONDOLA || this == FUNICULAR || this == TRANSIT
                || this == AIRPLANE;
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
