package eu.netmobiel.banker.model;

/**
 * Sort options for the charity listing.  
 * @author Jaap Reitsma
 *
 */
public enum CharitySortBy {
	/**
	 * Sort by the distance (as the crow flies) from the provided location. 
	 * If the location is not provided a BadRequestException will be thrown. 
	 */
	DISTANCE,
	/**
	 * Sort by the name of the charity
	 */
	NAME,
	/**
	 * Sort by the campaign start date of the charity
	 */
	DATE;
}
