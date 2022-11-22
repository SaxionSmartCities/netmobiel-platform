package eu.netmobiel.rideshare.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import com.google.common.base.Objects;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.rideshare.model.RideshareUser;
import eu.netmobiel.rideshare.service.RideshareUserManager;

/**
 * Base class for the rideshare resource handling. Contains a few convenience methods.
 * 
 * @author Jaap Reitsma
 *
 */
class RideshareResource {

	@Context
	protected HttpServletRequest request;

    @Inject
    protected RideshareUserManager rideshareUserManager;

    static Instant toInstant(OffsetDateTime odt) {
		return odt == null ? null : odt.toInstant();
	}

    protected boolean isPrivileged() {
    	return request.isUserInRole("admin"); 
    }
    
    protected void allowAdminOnly() {
    	if (!isPrivileged()) {
    		throw new SecurityException("You have no access rights");
    	}
    }

    protected void allowAdminOrCaller(RideshareUser caller, RideshareUser owner) {
    	if (!isPrivileged() && (owner == null || ! Objects.equal(caller.getId(), owner.getId()))) {
    		throw new SecurityException("You have no access rights");
    	}
    }

    protected void allowAdminOrCaller(RideshareUser owner) {
    	if (!isPrivileged() && (owner == null || ! Objects.equal(getCaller(), owner.getManagedIdentity()))) {
    		throw new SecurityException("You have no access rights");
    	}
    }

    protected String getCaller() {
    	return request.getUserPrincipal().getName();
    }

    protected RideshareUser resolveUserReference(String userId) throws NotFoundException, BadRequestException {
    	RideshareUser user = null;
		if ("me".equals(userId)) {
			String caller = getCaller();
			user = rideshareUserManager.findByManagedIdentity(caller).orElseThrow(() -> new NotFoundException("No such user: " + caller));
		} else {
			user = rideshareUserManager
					.resolveUrn(userId)
					.orElseThrow(() -> new NotFoundException("No such user: " + userId));
		}
		return user;
    }
}
