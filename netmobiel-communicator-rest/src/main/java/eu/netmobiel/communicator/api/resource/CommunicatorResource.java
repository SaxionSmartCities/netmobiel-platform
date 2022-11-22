package eu.netmobiel.communicator.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import com.google.common.base.Objects;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.CallingContext;
import eu.netmobiel.commons.security.SecurityIdentity;
import eu.netmobiel.communicator.model.CommunicatorUser;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.service.CommunicatorUserManager;

/**
 * Base class for the communicator resource handling. Contains a few convenience methods.
 * 
 * @author Jaap Reitsma
 *
 */
class CommunicatorResource {
	
	@Context
	protected HttpServletRequest request;

    @Inject
	protected SecurityIdentity securityIdentity;

	@Inject
    protected CommunicatorUserManager communicatorUserManager;

    protected Instant toInstant(OffsetDateTime odt) {
		return odt == null ? null : odt.toInstant();
	}

    protected void allowAdminOrEffectiveUser(CallingContext<CommunicatorUser> callingContext, CommunicatorUser owner) {
    	boolean privileged = request.isUserInRole("admin");
    	if (!privileged && (owner == null || ! Objects.equal(callingContext.getEffectiveUser().getId(), owner.getId()))) {
    		throw new SecurityException("You have no access rights");
    	}
    }
    
    protected void allowAdminOrCallingUser(CallingContext<CommunicatorUser> callingContext, CommunicatorUser owner) {
    	boolean privileged = request.isUserInRole("admin");
    	if (!privileged && (owner == null || ! Objects.equal(callingContext.getCallingUser().getId(), owner.getId()))) {
    		throw new SecurityException("You have no access rights");
    	}
    }

    /**
     * Checks whether the caller has enough privilege to proceed: Only the caller or the admin may proceeed.
     * @param request the http request.
     * @param mid the managed identity to check.
     */
    protected void allowAdminOrEffectiveUser(String mid) {
    	boolean privileged = request.isUserInRole("admin");
    	if (!privileged && ! Objects.equal(securityIdentity.getEffectivePrincipal().getName(), mid)) {
    		throw new SecurityException("You have no access rights");
    	}
    }

    protected void removeOtherRecipients(CommunicatorUser me, Message msg) {
		// If I am not the sender, then remove all the envelopes of other people
		CommunicatorUser sender = msg.getSender();
		if (sender == null || !me.equals(sender)) {
			msg.getEnvelopes()
				.removeIf(env -> !me.equals(env.getRecipient()));
		}
	}

    protected CommunicatorUser resolveUserReference(CallingContext<CommunicatorUser> callingContext, String userId) throws NotFoundException, BadRequestException {
    	CommunicatorUser user = null;
		if (userId != null) {
			if ("me".equals(userId)) {
				user = callingContext.getEffectiveUser();
			} else {
				user = communicatorUserManager
						.resolveUrn(userId)
						.orElseThrow(() -> new NotFoundException("No such user: " + userId));
			}
		}
		return user;
    }

    protected CommunicatorUser resolveCallingUserReference(CallingContext<CommunicatorUser> callingContext, String userId) throws NotFoundException, BadRequestException {
    	CommunicatorUser user = null;
		if (userId != null) {
			if ("me".equals(userId)) {
				user = callingContext.getCallingUser();
			} else {
				user = communicatorUserManager
						.resolveUrn(userId)
						.orElseThrow(() -> new NotFoundException("No such user: " + userId));
			}
		}
		return user;
    }
}
