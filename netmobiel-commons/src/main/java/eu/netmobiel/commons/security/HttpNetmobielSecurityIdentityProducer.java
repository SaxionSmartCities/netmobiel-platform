package eu.netmobiel.commons.security;

import java.security.Principal;
import java.util.List;

import javax.annotation.Resource;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.auth.BasicUserPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.slf4j.Logger;

import eu.netmobiel.commons.model.NetMobielUser;

@RequestScoped
public class HttpNetmobielSecurityIdentityProducer {
	/**
	 * Only users with this role can be a delegate for someone. 
	 */
	public static final String DELEGATE_ROLE_NAME = "delegate";
	/**
	 * The HTTP header with the delegator managed identity.
	 */
	public static final String DELEGATOR_HEADER = "X-Delegator";

    @Resource(lookup = "java:global/application/stage", description = "The development stage of the application. Use one of DEV, ACC, PROD")
    private String applicationStage;

	@Inject
    private HttpServletRequest request;

	@Inject
    private Logger logger;
 
    @Produces @Default
    public SecurityIdentity getSecurityIdentityFromHttprequest() {
    	KeycloakSecurityContext ksc = KeycloakSecurityContext.class.cast(request.getAttribute(KeycloakSecurityContext.class.getName()));
    	AccessToken token = ksc != null ? ksc.getToken() : null;
    	NetMobielUser realNetmobielUser = SecurityIdentity.createUserFromToken(token);

    	Principal realUser = request.getUserPrincipal();
    	Principal effectiveUser = realUser;
		String delegator = request.getHeader(DELEGATOR_HEADER);
		if (delegator != null) {
			if (!request.isUserInRole(DELEGATE_ROLE_NAME)) {
				throw new SecurityException(String.format("Header %s is set, but caller %s lacks role '%s'", DELEGATOR_HEADER, realNetmobielUser, DELEGATE_ROLE_NAME));
			}
			// This user wants to be a delegate. Check whether that is allowed.
			String key = SecurityIdentity.getDelegatorsClaimName(applicationStage); 
			@SuppressWarnings("unchecked")
			List<String> assignedDelegators = (List<String>) token.getOtherClaims().get(key);
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("User %s: %s = %s", realUser.getName(), key, 
						assignedDelegators != null ? String.join(", ", assignedDelegators) : ""));
			}
			if (assignedDelegators != null && assignedDelegators.contains(delegator)) {
				effectiveUser = new BasicUserPrincipal(delegator);
			} else {
				throw new SecurityException(String.format("User %s has no permission to act on behalf of %s", realNetmobielUser, delegator));
			}
    	}
		SecurityIdentity si = new NetmobielSecurityIdentity(realUser, effectiveUser, token);
		if (si.isDelegationActive()) {
			logger.info(String.format("Delegation active: %s", si.toString()));
		}
    	return si;
    }

//    if (principal instanceof KeycloakPrincipal) {
//		@SuppressWarnings("unchecked")
//		KeycloakPrincipal<KeycloakSecurityContext> kp = (KeycloakPrincipal<KeycloakSecurityContext>) principal;
//        KeycloakSecurityContext ksc = kp.getKeycloakSecurityContext(); 
//        this.token = ksc.getToken();
//    } else {
//    	throw new IllegalStateException("Expected a Keycloak Principal");
//    }

}
