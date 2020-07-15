package eu.netmobiel.commons.model;

import eu.netmobiel.commons.NetMobielModule;
import eu.netmobiel.commons.util.UrnHelper;

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

	/**
	 * Returns the email address of a user
	 * @return an email address, if available
	 */
	String getEmail();
	
	default String getKeyCloakUrn() {
		return UrnHelper.createUrnPrefix(NetMobielModule.KEYCLOAK.getCode(), "user") + getManagedIdentity();
	}
}
