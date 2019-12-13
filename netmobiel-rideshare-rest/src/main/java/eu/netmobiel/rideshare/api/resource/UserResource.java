package eu.netmobiel.rideshare.api.resource;

import java.util.stream.Collectors;

import javax.ejb.ObjectNotFoundException;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import com.github.dozermapper.core.Mapper;

import eu.netmobiel.rideshare.api.UsersApi;
import eu.netmobiel.rideshare.model.User;
import eu.netmobiel.rideshare.service.UserManager;
import eu.netmobiel.rideshare.util.RideshareUrnHelper;

@ApplicationScoped
@Path("/users")
public class UserResource implements UsersApi {

    @Inject
    private Mapper mapper;

   @Inject
    private UserManager userManager;

    /**
     * Lists the users. For debugging, this should be admin only.
     * @return an array of users.
     */
    @Override
	public Response getUsers() {
    	return Response.ok(userManager.listUsers().stream()
    			.map(u -> mapper.map(u,  eu.netmobiel.rideshare.api.model.User.class))
    			.collect(Collectors.toList())).build();
    }

    @Override
	public Response getUser(String userId) {
    	User user = null;
    	try {
        	Long uid = RideshareUrnHelper.getId(User.URN_PREFIX, userId);
			user = userManager.getUser(uid);
		} catch (ObjectNotFoundException e) {
			throw new NotFoundException();
		}
    	return Response.ok(mapper.map(user, eu.netmobiel.rideshare.api.model.User.class)).build();
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
