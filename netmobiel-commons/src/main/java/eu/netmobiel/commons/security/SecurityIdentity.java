package eu.netmobiel.commons.security;

import java.security.Principal;

import org.keycloak.representations.AccessToken;

import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.NetMobielUserImpl;

public interface SecurityIdentity {

	/**
	 * Fetches the user performing the call.
	 * @return
	 */
	Principal getPrincipal();
	
	/**
	 * Fetches the effective user, that is the user on whose behalf the action is performed.
	 * When no delegation is active the real and effective users are the same. 
	 * @return
	 */
	Principal getEffectivePrincipal();

	/**
	 * Returns the access token of the principal.
	 * @return A Keycloak access token.
	 */
	AccessToken getToken();

	/**
	 * Returns true if delegation is active, i.e. effectiveUser and realUser differ.
	 * @return Whether delegation is active.
	 */
    boolean isDelegationActive();
    
	/**
	 * Creates a NetmobielUser record from the token.
	 * @return A netmobiel user or null if the token is not available, e.g. anonymous call.
	 */
	NetMobielUser getRealUser();
	
	/**
	 * Returns the delegators attribute name in the token claim, depending on the current project stage.
	 * User DEV, ACC or PROD as stage.
	 */
	public static String getDelegatorsClaimName(String stage) {
    	return "delegators" + (stage.equals("PROD") ? "" : ("-" + stage));
    }

	/**
	 * Creates a Netmobiel User object from a Keycloak Access Token.
	 * @param token the token
	 * @return The user object or null if anonymous.
	 */
	public static NetMobielUser createUserFromToken(AccessToken token) {
        NetMobielUserImpl user = null;
        if (token != null) {
            user = new NetMobielUserImpl();
            user.setManagedIdentity(token.getSubject()); // Same as principal.getName()
            user.setFamilyName(token.getFamilyName());
            user.setGivenName(token.getGivenName());
            user.setEmail(token.getEmail());
        }
    	return user;
    }
}
