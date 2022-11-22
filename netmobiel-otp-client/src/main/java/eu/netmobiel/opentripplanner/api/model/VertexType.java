package eu.netmobiel.opentripplanner.api.model;


/**
 * Represent type of vertex, used in Place aka from, to in API
 * for easier client side localization
 */
public enum VertexType {
	NORMAL, 	
	BIKESHARE, 	
	BIKEPARK, 	
	TRANSIT;
}
