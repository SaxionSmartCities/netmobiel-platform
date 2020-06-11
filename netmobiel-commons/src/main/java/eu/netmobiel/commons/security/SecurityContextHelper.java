package eu.netmobiel.commons.security;

import java.security.Principal;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;

import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.NetMobielUserImpl;

public class SecurityContextHelper {
	private SecurityContextHelper () {
		// Do not instantiate
	}
	
	public static NetMobielUser getUserContext(Principal p) {
        NetMobielUserImpl user = null;
    	if (p != null && p instanceof KeycloakPrincipal) {
    		@SuppressWarnings("unchecked")
			KeycloakPrincipal<KeycloakSecurityContext> kp = (KeycloakPrincipal<KeycloakSecurityContext>) p;
            KeycloakSecurityContext ksc = kp.getKeycloakSecurityContext(); 
//                log.debug("Is user in role admin? " +  httpReq.isUserInRole("admin"));
            AccessToken token = ksc.getToken();
            if (token != null) {
                user = new NetMobielUserImpl();
                user.setManagedIdentity(token.getSubject()); // Same as kp.getName()
                user.setFamilyName(token.getFamilyName());
                user.setGivenName(token.getGivenName());
                user.setEmail(token.getEmail());
            }
    	}
    	return user;
    }
}
