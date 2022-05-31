package eu.netmobiel.banker.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import com.google.common.base.Objects;

import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.service.BankerUserManager;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.CallingContext;
import eu.netmobiel.commons.security.SecurityIdentity;

/**
 * Base class for the banker resource handling. Contains a few convenience methods.
 * 
 * @author Jaap Reitsma
 *
 */
class BankerResource {
	
	@Context
	protected HttpServletRequest request;

    @Inject
	protected SecurityIdentity securityIdentity;

	@Inject
    protected BankerUserManager bankerUserManager;

	protected static final Predicate<HttpServletRequest> isAdmin = rq -> rq.isUserInRole("admin");
	
    protected Instant toInstant(OffsetDateTime odt) {
		return odt == null ? null : odt.toInstant();
	}

    protected void allowAdminOrEffectiveUser(CallingContext<BankerUser> callingContext, BankerUser owner) {
    	boolean privileged = request.isUserInRole("admin");
    	if (!privileged && (owner == null || ! Objects.equal(callingContext.getEffectiveUser().getId(), owner.getId()))) {
    		throw new SecurityException("You have no access rights");
    	}
    }
    
    /**
     * Checks whether the caller has enough privilege to proceed: Only the caller or the admin may proceeed.
     * @param mid the managed identity to check.
     */
    protected void allowAdminOrEffectiveUser(String mid) {
    	boolean privileged = request.isUserInRole("admin");
    	if (!privileged && ! Objects.equal(securityIdentity.getEffectivePrincipal().getName(), mid)) {
    		throw new SecurityException("You have no access rights");
    	}
    }

    protected BankerUser resolveUserReference(CallingContext<BankerUser> callingContext, String userId) throws NotFoundException, BadRequestException {
    	BankerUser user = null;
		if ("me".equals(userId)) {
			user = callingContext.getEffectiveUser();
		} else {
			user = bankerUserManager
					.resolveUrn(userId)
					.orElseThrow(() -> new NotFoundException("No such user: " + userId));
		}
		return user;
    }

}
