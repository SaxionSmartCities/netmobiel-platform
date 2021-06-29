package eu.netmobiel.commons.security;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.OIDCAuthenticationError;
import org.keycloak.representations.AccessToken;
import org.slf4j.Logger;

/**
 * This is a simple filter that extracts authentication information from Keycloak.
 */
public class KeycloakSecurityFilter implements Filter {

    @Inject
    private Logger log;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        if (log.isDebugEnabled()) {
	        OIDCAuthenticationError error = (OIDCAuthenticationError) httpReq.getAttribute("org.keycloak.adapters.spi.AuthenticationError");
	        if (error != null) {
	        	log.debug(String.format("Keycloak authentication error: %s - %s", error.getReason(), error.getDescription()));
	        }
			KeycloakSecurityContext ksc = getKeycloakSecurityContext(request);
			if (ksc != null) {
				AccessToken token = ksc.getToken();
				if (token != null) {
		        	log.debug(String.format("%s: %s %s %s", token.getSubject(), token.getEmail(), token.getFamilyName(), token.getGivenName()));
				}
			}        
        }
        chain.doFilter(request, response);
    }

    private static KeycloakSecurityContext getKeycloakSecurityContext(ServletRequest request) {
        return KeycloakSecurityContext.class.cast(request.getAttribute(KeycloakSecurityContext.class.getName()));
    }

}
