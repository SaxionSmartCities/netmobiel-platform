package eu.netmobiel.overseer.model;

import java.util.stream.Stream;

public enum IncentiveCategory {
	CARPOOL,
	SURVEY,
	UNKNOWN;

	public static IncentiveCategory lookup(String value) {
	    return Stream.of(IncentiveCategory.values())
	            .filter(c -> c.name().equals(value))
	            .findFirst()
	            .orElse(UNKNOWN);
    }
}
