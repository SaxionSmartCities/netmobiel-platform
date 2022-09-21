package eu.netmobiel.to.rideshare.api.resource;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.tomp.api.model.Address;
import eu.netmobiel.tomp.api.model.Place;

public class PlaceHelper {

	public static GeoLocation createGeolocation(Place place) {
    	GeoLocation loc = new GeoLocation();
    	if (place.getName() != null) {
    		loc.setLabel(place.getName());
    	} else if (place.getPhysicalAddress() != null) {
    		Address addr = place.getPhysicalAddress();
    		String streetPart = addr.getStreetAddress();
    		if (streetPart == null) {
        		StringBuilder sb = new StringBuilder();
        		if (addr.getStreetAddress() != null) {
        			sb.append(addr.getStreetAddress());
        		} else if (addr.getStreet() != null) {
        			sb.append(addr.getStreet()).append(" ");
        			if (addr.getHouseNumber() != null) {
        				sb.append(addr.getHouseNumber()).append(" ");
        			}
        			if (addr.getHouseNumberAddition() != null) {
        				sb.append(addr.getHouseNumberAddition());
        			}
        		}
        		streetPart = sb.toString().trim();
    		}
    		String cityPart = addr.getAreaReference();
    		if (cityPart == null) {
        		StringBuilder sb = new StringBuilder();
        		if (addr.getPostalCode() != null) {
        			sb.append(addr.getPostalCode()).append(" ");
        		}
    			if (addr.getCity() != null) {
    				sb.append(addr.getCity());
    			}
        		cityPart = sb.toString().trim();
    		}
    		loc.setLabel(streetPart + " " + cityPart);
    	}
    	loc.setLatitude(place.getCoordinates().getLat().doubleValue());
    	loc.setLongitude(place.getCoordinates().getLng().doubleValue());
    	return loc;
    }
    			
}
