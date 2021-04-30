package eu.netmobiel.planner.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Objects;

import eu.netmobiel.commons.model.CallingContext;
import eu.netmobiel.planner.model.PlannerUser;

/**
 * Base class for the planner resource handling. Contains a view convenience methods.
 * 
 * @author Jaap Reitsma
 *
 */
class PlannerResource {
	
    protected Instant toInstant(OffsetDateTime odt) {
		return odt == null ? null : odt.toInstant();
	}

    protected void allowAdminOrEffectiveUser(HttpServletRequest request, CallingContext<PlannerUser> callingContext, PlannerUser owner) {
    	boolean privileged = request.isUserInRole("admin");
    	if (!privileged && (owner == null || ! Objects.equal(callingContext.getEffectiveUser().getId(), owner.getId()))) {
    		throw new SecurityException("You have no access rights");
    	}
    }
    
}
