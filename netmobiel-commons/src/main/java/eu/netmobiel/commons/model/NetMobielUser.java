package eu.netmobiel.commons.model;

import java.io.Serializable;

import eu.netmobiel.commons.NetMobielModule;
import eu.netmobiel.commons.util.UrnHelper;

/**
 * Definition of a Netmobiel User.
 * @author Jaap Reitsma
 *
 */
public interface NetMobielUser extends Serializable {
	public static final String KEYCLOAK_URN_PREFIX = UrnHelper.createUrnPrefix(NetMobielModule.KEYCLOAK.getCode(), "user");
	
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
	
	/**
	 * Compares the attributes of this user with those of the other user.
	 * @param other the other user
	 * @return true if all attributes are exactly the same, false otherwise.
	 */
	boolean isSame(NetMobielUser other);

	default String getKeyCloakUrn() {
		return UrnHelper.createUrn(KEYCLOAK_URN_PREFIX, getManagedIdentity());
	}
	
	default String getName() {
		StringBuilder sb = new StringBuilder();
		if (getGivenName() != null) {
			sb.append(getGivenName()).append(" ");
		}
		if (getFamilyName() != null) {
			sb.append(getFamilyName());
		}
		String name = sb.toString().trim();
		return name.length() > 0 ? name : null;
	}

	default String getNameAndEmail() {
		StringBuilder sb = new StringBuilder();
		if (getGivenName() != null) {
			sb.append(getGivenName()).append(" ");
		}
		if (getFamilyName() != null) {
			sb.append(getFamilyName());
		}
		if (getEmail() != null) {
			sb.append(" (").append(getEmail()).append(")");
		}
		return sb.toString().trim();
	}
	
}
