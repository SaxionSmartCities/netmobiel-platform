package eu.netmobiel.banker.model;

/**
 * Sort options for the donation listing.
 *   
 * @author Jaap Reitsma
 *
 */
public enum DonationSortBy {
	/**
	 * Sort by the donation date.
	 */
	DATE,
	/**
	 * Sort by the amount donated.  
	 */
	AMOUNT,
	/**
	 * Sort by the number of distinct donors (report charity popularity only).  
	 */
	DONORS;
}
