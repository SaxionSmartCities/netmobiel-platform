package eu.netmobiel.banker.model;

/**
 * Grouping options for the donation listing. 
 * 
 * @author Jaap Reitsma
 *
 */
public enum DonationGroupBy {
	/**
	 * Group by charity.  
	 */
	CHARITY,
	/**
	 * Group by user.  
	 */
	USER,
	/**
	 * Group by charity and by user.  
	 */
	CHARITY_AND_USER;
}
