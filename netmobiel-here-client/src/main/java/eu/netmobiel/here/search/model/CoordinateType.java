package eu.netmobiel.here.search.model;

/**
 * The type of the position field.
 * 
 * @see https://developer.here.com/documentation/geocoding-search-api/api-reference-swagger.html
 * @author Jaap Reitsma
 *
 */
public class CoordinateType {
	public Double lat;
	public Double lng;
	
	@Override
	public String toString() {
		return String.format("CoordinateType [lat=%s, lng=%s]", lat, lng);
	}
}
