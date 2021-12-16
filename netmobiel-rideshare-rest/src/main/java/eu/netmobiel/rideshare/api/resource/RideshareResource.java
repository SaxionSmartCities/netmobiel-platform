package eu.netmobiel.rideshare.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Objects;

import eu.netmobiel.rideshare.model.RideshareUser;

/**
 * Base class for the rideshare resource handling. Contains a few convenience methods.
 * 
 * @author Jaap Reitsma
 *
 */
class RideshareResource {

    static Instant toInstant(OffsetDateTime odt) {
		return odt == null ? null : odt.toInstant();
	}

    protected void allowAdminOrCaller(HttpServletRequest request, RideshareUser caller, RideshareUser owner) {
    	boolean privileged = request.isUserInRole("admin");
    	if (!privileged && (owner == null || ! Objects.equal(caller.getId(), owner.getId()))) {
    		throw new SecurityException("You have no access rights");
    	}
    }
}
