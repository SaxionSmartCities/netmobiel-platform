package eu.netmobiel.opentripplanner.api.model;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum TransportationType {
	TRAM,		// 0 - Tram, Streetcar, Light rail. Any light rail or street level system within a metropolitan area.
	SUBWAY,		// 1 - Subway, Metro. Any underground rail system within a metropolitan area.
	RAIL,		// 2 - Rail. Used for intercity or long-distance travel.
	BUS,		// 3 - Bus. Used for short- and long-distance bus routes.
	FERRY,		// 4 - Ferry. Used for short- and long-distance boat service.
	CABLE_CAR,	// 5 - Cable car. Used for street-level cable cars where the cable runs beneath the car.
	GONDOLA,	// 6 - Gondola, Suspended cable car. Typically used for aerial cable cars where the car is suspended from the cable.
	FUNICULAR;	// 7 - Funicular. Any rail system designed for steep inclines.
	
	private TransportationType() {
	}

	/**
	 * Returns the bitmask used for the enum. Example: RAIL is ordinal 2, so the mask is 1 << 2 = 0x04.
	 * @return the bitmask.
	 */
	public int getMask() {
		return 1 << ordinal();
	}
	
	public static String listNames(int values) {
		return Arrays.stream(TransportationType.values())
				.filter(t -> (values & t.getMask()) != 0)
				.map(t -> t.name())
				.collect(Collectors.joining(", "));
	}
}
