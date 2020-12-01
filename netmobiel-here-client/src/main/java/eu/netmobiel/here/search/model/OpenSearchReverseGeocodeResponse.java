package eu.netmobiel.here.search.model;

/**
 * The item list after a successful call to a search API method.
 * 
 * @see https://developer.here.com/documentation/geocoding-search-api/api-reference-swagger.html
 * @author Jaap Reitsma
 *
 * Note: Automatic code generation (openapi generator) is an options, but it is a lot of fuss to get it working. 
 * The generator did not recognize the newset OneOf scheme things. 
 */
public class OpenSearchReverseGeocodeResponse {
	/**
	 * An array of items;
	 */
	public GeocodeResultItem[] items;
}
