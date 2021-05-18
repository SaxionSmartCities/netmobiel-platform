package eu.netmobiel.profile.api.resource;

import javax.inject.Inject;

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

}
