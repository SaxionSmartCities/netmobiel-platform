package eu.netmobiel.rideshare.api.resource;

import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.rideshare.api.UsersApi;
import eu.netmobiel.rideshare.api.mapping.UserMapper;
import eu.netmobiel.rideshare.model.RideshareUser;
import eu.netmobiel.rideshare.service.RideshareUserManager;

@RequestScoped
@Path("/users")
public class UserResource implements UsersApi {

    @Inject
    private UserMapper mapper;

    @Inject
    private RideshareUserManager userManager;

    /**
     * Lists the users. For debugging, this should be admin only.
     * @return an array of users.
     */
    @Override
	public Response getUsers() {
    	return Response.ok(userManager.listUsers().stream()
    			.map(u -> mapper.map(u))
    			.collect(Collectors.toList())).build();
    }

    @Override
	public Response getUser(String userId) {
    	RideshareUser user = null;
    	try {
        	Long uid = UrnHelper.getId(RideshareUser.URN_PREFIX, userId);
			user = userManager.getUser(uid);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return Response.ok(mapper.map(user)).build();
    }

	@Override
	public Response updateUser(String userId, eu.netmobiel.rideshare.api.model.User user) {
		throw new UnsupportedOperationException("updateUser");
	}

	@Override
	public Response deleteUser(String userId) {
		throw new UnsupportedOperationException("deleteUser");
	}

}
