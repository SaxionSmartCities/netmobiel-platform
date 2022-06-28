package eu.netmobiel.to.rideshare.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

/**
 * Base class for the transport operator resource handling. Contains a few convenience methods.
 * 
 * @author Jaap Reitsma
 *
 */
class TransportOperatorResource {

	@Context
	protected HttpServletRequest request;

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

    protected String getCaller() {
    	return request.getUserPrincipal().getName();
    }

}
