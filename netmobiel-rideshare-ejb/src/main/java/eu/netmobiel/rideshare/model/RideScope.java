package eu.netmobiel.rideshare.model;

import java.util.stream.Stream;

public enum RideScope {
	THIS("this"),
	THIS_AND_FOLLOWING("this-and-following");

	private String code;
	 
    private RideScope(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }

    public static RideScope lookup(String code) {
	    return Stream.of(RideScope.values())
	            .filter(c -> c.getCode().equals(code))
	            .findFirst()
	            .orElseThrow(IllegalArgumentException::new);
    }
}
