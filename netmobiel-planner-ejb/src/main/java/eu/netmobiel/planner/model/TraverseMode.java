package eu.netmobiel.planner.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;

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
    SUBWAY("SW"), 
    TRAM("TM"), 
    TRANSIT("TS"), 
    WALK("WK");

    private static HashMap <Set<TraverseMode>, Set<TraverseMode>> setMap = 
            new HashMap <Set<TraverseMode>, Set<TraverseMode>>();

	private String code;
	 
    private TraverseMode(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }

    public static Set<TraverseMode> internSet (Set<TraverseMode> modeSet) {
        if (modeSet == null)
            return null;
        Set<TraverseMode> ret = setMap.get(modeSet);
        if (ret == null) {
            EnumSet<TraverseMode> backingSet = EnumSet.noneOf(TraverseMode.class);
            backingSet.addAll(modeSet);
            Set<TraverseMode> unmodifiableSet = Collections.unmodifiableSet(backingSet);
            setMap.put(unmodifiableSet, unmodifiableSet);
            ret = unmodifiableSet;
        }
        return ret;
    }

    public boolean isTransit() {
        return this == TRAM || this == SUBWAY || this == RAIL || this == BUS || this == FERRY
                || this == CABLE_CAR || this == GONDOLA || this == FUNICULAR || this == TRANSIT
                || this == AIRPLANE;
    }

    public boolean isOnStreetNonTransit() {
        return this == WALK || this == BICYCLE || this == CAR;
    }
    
    public boolean isDriving() {
        return this == CAR;
    }

    public static boolean containsTransit(TraverseMode[] modes) {
    	return Arrays.stream(modes)
    			.filter(m -> m.isTransit())
    			.count() > 0;
    }
}
