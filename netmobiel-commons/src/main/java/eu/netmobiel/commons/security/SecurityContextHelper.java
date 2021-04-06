package eu.netmobiel.commons.security;

import java.security.Principal;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;

import eu.netmobiel.commons.model.NetMobielUser;

public class SecurityContextHelper {
	private SecurityContextHelper () {
		// Do not instantiate
	}
	
	public static NetMobielUser getUserContext(Principal p) {
        NetMobielUser user = null;
    	if (p != null && p instanceof KeycloakPrincipal) {
    		@SuppressWarnings("unchecked")
			KeycloakPrincipal<KeycloakSecurityContext> kp = (KeycloakPrincipal<KeycloakSecurityContext>) p;
            KeycloakSecurityContext ksc = kp.getKeycloakSecurityContext(); 
//                log.debug("Is user in role admin? " +  httpReq.isUserInRole("admin"));
            AccessToken token = ksc.getToken();
            user = SecurityIdentity.createUserFromToken(token);
    	}
    	return user;
    }
}
