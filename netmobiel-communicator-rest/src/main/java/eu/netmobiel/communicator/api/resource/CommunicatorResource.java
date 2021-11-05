package eu.netmobiel.communicator.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Objects;

import eu.netmobiel.commons.model.CallingContext;
import eu.netmobiel.communicator.model.CommunicatorUser;

/**
 * Base class for the communicator resource handling. Contains a view convenience methods.
 * 
 * @author Jaap Reitsma
 *
 */
class CommunicatorResource {
	
    protected Instant toInstant(OffsetDateTime odt) {
		return odt == null ? null : odt.toInstant();
	}

    protected void allowAdminOrEffectiveUser(HttpServletRequest request, CallingContext<CommunicatorUser> callingContext, CommunicatorUser owner) {
    	boolean privileged = request.isUserInRole("admin");
    	if (!privileged && (owner == null || ! Objects.equal(callingContext.getEffectiveUser().getId(), owner.getId()))) {
    		throw new SecurityException("You have no access rights");
    	}
    }
    
}
