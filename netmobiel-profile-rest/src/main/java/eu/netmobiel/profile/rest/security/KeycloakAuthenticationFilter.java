package eu.netmobiel.profile.rest.security;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.OIDCAuthenticationError;
import org.keycloak.representations.AccessToken;
import org.slf4j.Logger;

/**
 * This is a simple filter that extracts authentication information from Keycloak.
 * The actual security handling is performed in the EJBs.
 */
@WebFilter(filterName = "KeycloakAuthenticationFilter", urlPatterns = "/*")
public class KeycloakAuthenticationFilter implements Filter {

//    @Inject
//    private SecurityContext security;

    @Inject
    private Logger log;
    /**
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    	log.info("KeycloakAuthenticationFilter initialized");
    }

    /**
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        // This is also possible:
        // (KeycloakSecurityContext) req.getAttribute(KeycloakSecurityContext.class.getName());
        if (log.isDebugEnabled()) {
	        OIDCAuthenticationError error = (OIDCAuthenticationError) httpReq.getAttribute("org.keycloak.adapters.spi.AuthenticationError");
	        if (error != null) {
	        	log.debug(String.format("Keycloak authentication error: %s - %s", error.getReason(), error.getDescription()));
	        }
        }
//        @SuppressWarnings("unchecked")
//		KeycloakPrincipal<KeycloakSecurityContext> kp = (KeycloakPrincipal<KeycloakSecurityContext>) httpReq.getUserPrincipal();
//        if (kp != null) {
//            KeycloakSecurityContext ksc = kp.getKeycloakSecurityContext(); 
//            // log.debug("Is user in role admin? " +  httpReq.isUserInRole("admin"));
//            // Fabricate a User object from information in the access token and store it in the security context.
//            // Use CDI to inject the (request) context where needed.
//            AccessToken token = ksc.getToken();
//            if (token != null) {
//                User user = new User();
//                user.setManagedIdentity(token.getSubject()); // Same as kp.getName()
//                user.setEmail(token.getEmail());
//                user.setFamilyName(token.getFamilyName());
//                user.setGivenName(token.getGivenName());
//                // Get the implementation of the security context
//                SecurityContext sc = (SecurityContext) security; 
//                sc.setUser(user);
//                sc.setToken(ksc.getTokenString());
//            }
//        	log.info("User: " + (security != null ? security.toString() : "<null>"));
//        }
		KeycloakSecurityContext ksc = getKeycloakSecurityContext(request);
		if (ksc != null) {
//			AuthorizationContext ac = ksc.getAuthorizationContext();
			AccessToken token = ksc.getToken();
			if (token != null) {
	        	log.info(String.format("%s: %s %s %s", token.getSubject(), token.getEmail(), token.getFamilyName(), token.getGivenName()));
			}
		}        
        chain.doFilter(request, response);
    }

    private static KeycloakSecurityContext getKeycloakSecurityContext(ServletRequest request) {
        return KeycloakSecurityContext.class.cast(request.getAttribute(KeycloakSecurityContext.class.getName()));
    }

}
