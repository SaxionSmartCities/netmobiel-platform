package eu.netmobiel.profile.api.resource;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Objects;

import eu.netmobiel.commons.security.SecurityIdentity;

public class BasicResource {

	@Inject
	protected SecurityIdentity securityIdentity;
	
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
    protected void allowAdminOrEffectiveUser(HttpServletRequest request, String mid) {
    	boolean privileged = request.isUserInRole("admin");
    	if (!privileged && ! Objects.equal(securityIdentity.getEffectivePrincipal().getName(), mid)) {
    		throw new SecurityException("You have no access rights");
    	}
    }

//    protected void allowAdminOrEffectiveUser(HttpServletRequest request, CallingContext<PlannerUser> callingContext, PlannerUser owner) {
//    	boolean privileged = request.isUserInRole("admin");
//    	if (!privileged && (owner == null || ! Objects.equal(callingContext.getEffectiveUser().getId(), owner.getId()))) {
//    		throw new SecurityException("You have no access rights");
//    	}
//    }
}
