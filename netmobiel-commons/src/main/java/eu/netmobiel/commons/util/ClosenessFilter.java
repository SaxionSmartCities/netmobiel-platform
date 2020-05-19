package eu.netmobiel.commons.util;

import java.util.function.BiPredicate;

import eu.netmobiel.commons.model.GeoLocation;

public class ClosenessFilter implements BiPredicate<GeoLocation, GeoLocation>{
	private double distanceInKm;
	
	public ClosenessFilter(int minimumDistanceInMeters) {
		this.distanceInKm = minimumDistanceInMeters / 1000.0;
	}

	@Override
	public boolean test(GeoLocation locA, GeoLocation locB) {
		return locA.getDistanceFlat(locB) <= distanceInKm;
	}

}
