package eu.netmobiel.commons.model;

/**
 * Definition of a NetMobiel User.
 * @author Jaap Reitsma
 *
 */
public interface NetMobielUser {
	/**
	 * The unique identity of the user as determined by Keycloak. 
	 * @return the managed identity. 
	 */
	String getManagedIdentity();
	/**
	 * Returns the given name of a user.
	 * @return a name.
	 */
	String getGivenName();
	/**
	 * Returns the family name of a user
	 * @return a family name
	 */
	String getFamilyName();
}
