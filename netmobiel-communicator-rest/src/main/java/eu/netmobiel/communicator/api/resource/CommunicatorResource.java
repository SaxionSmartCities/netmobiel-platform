package eu.netmobiel.communicator.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Objects;

import eu.netmobiel.commons.model.CallingContext;
import eu.netmobiel.communicator.model.CommunicatorUser;
import eu.netmobiel.communicator.model.Message;

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
    
	protected void removeOtherRecipients(String me, Message msg) {
		// If I am not the sender, then remove all the envelopes of other people
		CommunicatorUser sender = msg.getSender();
		if (sender == null || !me.equals(sender.getManagedIdentity())) {
			msg.getEnvelopes()
				.removeIf(env -> !me.equals(env.getRecipient().getManagedIdentity()));
		}
	}

}
