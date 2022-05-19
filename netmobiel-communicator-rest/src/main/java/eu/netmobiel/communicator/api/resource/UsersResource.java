package eu.netmobiel.communicator.api.resource;

import java.time.Instant;
import java.time.ZoneOffset;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.model.CallingContext;
import eu.netmobiel.communicator.api.UsersApi;
import eu.netmobiel.communicator.api.mapping.UserMapper;
import eu.netmobiel.communicator.api.model.FirebaseToken;
import eu.netmobiel.communicator.model.CommunicatorUser;

@RequestScoped
public class UsersResource extends CommunicatorResource implements UsersApi {

	@Inject
	private UserMapper userMapper;

	@Override
	public Response getUser(String xDelegator, String userId) {
    	Response rsp = null;
		try {
			CallingContext<CommunicatorUser> callingContext = userManager.findOrRegisterCallingContext(securityIdentity);
			CommunicatorUser user = resolveUserReference(callingContext, userId);
			allowAdminOrEffectiveUser(callingContext, user);
        	user = userManager.getUserAndStatus(user.getId());
			rsp = Response.ok(userMapper.map(user)).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
	}

	// =========================   Firebase Messaging Token  =========================

	@Override
	public Response getFcmToken(String xDelegator, String userId) {
		Response rsp = null;
		try {
			CallingContext<CommunicatorUser> callingContext = userManager.findOrRegisterCallingContext(securityIdentity);
			CommunicatorUser user = resolveUserReference(callingContext, userId);
			allowAdminOrEffectiveUser(callingContext, user);
			FirebaseToken token = new FirebaseToken();
			token.setToken(user.getFcmToken());
			if (user.getFcmTokenTimestamp() != null) {
				token.setLastUpdate(user.getFcmTokenTimestamp().atOffset(ZoneOffset.UTC));
			}
			rsp = Response.ok(token).build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}

	@Override
	public Response updateFcmToken(String xDelegator, String userId, FirebaseToken firebaseToken) {
		Response rsp = null;
		try {
			// Only admin and effective owner can update the user
			CallingContext<CommunicatorUser> callingContext = userManager.findOrRegisterCallingContext(securityIdentity);
			CommunicatorUser user = resolveUserReference(callingContext, userId);
			allowAdminOrEffectiveUser(callingContext, user);
			// Always update to force update of timestamp of token
			user.setFcmToken(firebaseToken.getToken());
			user.setFcmTokenTimestamp(Instant.now());
			userManager.updateUser(user);
			rsp = Response.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e);
		} catch (BusinessException ex) {
			throw new WebApplicationException(ex);
		}
		return rsp;
	}


}
