package eu.netmobiel.commons.security;

import java.security.Principal;
import java.util.Optional;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;

import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.NetMobielUserImpl;

public interface SecurityIdentity {

	/**
	 * Fetches the user performing the call.
	 * @return the calling principal.
	 */
	Principal getPrincipal();
	
	/**
	 * Fetches the effective user, that is the user on whose behalf the action is performed.
	 * When no delegation is active the real and effective users are the same. 
	 * @return the principal on whose behalf the call is made.
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
	 * Fetches the NetmobielUser record associated with calling (i.e., real) principal (and token). 
	 * If the object does not exist yet it is created from the token and cached.
	 * The object is not yet related to the database user object. The managed identity is
	 * the key between this identity and the identity in the database.
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

	/**
	 * Creates a NetMobielUser object from a Keycloak principal, if any. 
	 * @param p the principal from the http request or the EJB session context. 
	 * @return An Optional with the user, if any.
	 */
	public static Optional<NetMobielUser> getKeycloakContext(Principal p) {
        NetMobielUser user = null;
        // In an EJB the non-authenticated user is represented by a AnonymousPrincipal with name 'anonymous'.
    	if (p != null && p instanceof KeycloakPrincipal) {
    		@SuppressWarnings("unchecked")
			KeycloakPrincipal<KeycloakSecurityContext> kp = (KeycloakPrincipal<KeycloakSecurityContext>) p;
            KeycloakSecurityContext ksc = kp.getKeycloakSecurityContext(); 
            AccessToken token = ksc.getToken();
            user = createUserFromToken(token);
    	}
    	return Optional.ofNullable(user);
    }

	/**
	 * Retrieve the session ID from the access token. 
	 * @param p the principal
	 * @return the session id, if any.
	 */
	public static Optional<String> getKeycloakSessionId(Principal p) {
        String sessionState = null;
        // In an EJB the non-authenticated user is represented by a AnonymousPrincipal with name 'anonymous'.
    	if (p != null && p instanceof KeycloakPrincipal) {
    		@SuppressWarnings("unchecked")
			KeycloakPrincipal<KeycloakSecurityContext> kp = (KeycloakPrincipal<KeycloakSecurityContext>) p;
            KeycloakSecurityContext ksc = kp.getKeycloakSecurityContext(); 
            AccessToken token = ksc.getToken();
            sessionState = token.getSessionState();
    	}
    	return Optional.ofNullable(sessionState);
    }
}
