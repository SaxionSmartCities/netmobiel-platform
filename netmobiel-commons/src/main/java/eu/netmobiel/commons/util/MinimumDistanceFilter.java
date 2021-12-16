package eu.netmobiel.commons.util;

import java.util.function.Predicate;

import eu.netmobiel.commons.model.GeoLocation;

public class MinumumDistanceFilter implements Predicate<GeoLocation>{
	private double distanceInKm;
	private GeoLocation previous;
	
	public MinumumDistanceFilter(int minimumDistanceInMeters) {
		this.distanceInKm = minimumDistanceInMeters / 1000.0;
	}

	@Override
	public boolean test(GeoLocation location) {
		boolean valid = (previous == null) ? true : location.getDistanceFlat(previous) >= distanceInKm;
		previous = location;
		return valid;
	}

}
