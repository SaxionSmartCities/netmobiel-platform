package eu.netmobiel.profile.api.resource;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import com.google.common.base.Objects;

import eu.netmobiel.commons.security.SecurityIdentity;

public class BasicResource {

	@Context
	private HttpServletRequest request;

	@Inject
	protected SecurityIdentity securityIdentity;
	
	protected boolean isAdmin() {
		return request.isUserInRole("admin"); 
	}

	protected boolean isDelegate() {
		return request.isUserInRole("delegate"); 
	}

	protected String getRemoteAddress() {
		return request.getRemoteAddr(); 
	}

	/**
	 * Determines the real user of the call in case 'me'is used.
	 * @param profileId the profile id. If 'me' is used then the real user id is taken.
	 * @return the resolved user id (a keycloak managed identity).
	 */
    protected String resolveIdentity(String profileId) {
		String mid = null;
		if ("me".equals(profileId)) {
			mid = request.getUserPrincipal().getName();
		} else {
			mid = profileId;
		}
		return mid;
    }

	/**
	 * Determines the effective user of the call.
	 * @param xDelegator The header delegator user is. Not used, because we take the effective user from the security identity object.
	 * @param profileId the profile id. If 'me' is used then the effective user id is taken.
	 * @return the resolved user id (a keycloak managed identity).
	 */
    protected String resolveIdentity(String xDelegator, String profileId) {
		String mid = null;
		if ("me".equals(profileId)) {
			mid = securityIdentity.getEffectivePrincipal().getName();
		} else {
			mid = profileId;
		}
		return mid;
    }

    /**
     * Checks whether the caller has enough privilege to proceed: Only the caller or the admin may proceeed.
     * @param request the http request.
     * @param mid the managed identity to check.
     */
    protected void allowAdminOrEffectiveUser(String mid) {
    	if (!isAdmin() && ! Objects.equal(securityIdentity.getEffectivePrincipal().getName(), mid)) {
    		throw new SecurityException("You have no access rights");
    	}
    }

}
