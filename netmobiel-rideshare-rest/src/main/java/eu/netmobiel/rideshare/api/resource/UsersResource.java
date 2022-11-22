package eu.netmobiel.rideshare.api.resource;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.rideshare.api.UsersApi;
import eu.netmobiel.rideshare.api.mapping.CarMapper;
import eu.netmobiel.rideshare.api.mapping.UserMapper;
import eu.netmobiel.rideshare.model.Car;
import eu.netmobiel.rideshare.model.RideshareUser;
import eu.netmobiel.rideshare.service.CarManager;

@RequestScoped
@Path("/users")
public class UsersResource extends RideshareResource implements UsersApi {

    @Inject
    private UserMapper userMapper;

	@Inject
    private CarMapper carMapper;

	@Inject
	private CarManager carManager;
	
    /**
     * Lists the users. For debugging, admin only.
     * @return an array of users.
     */
    @Override
	public Response getUsers() {
    	allowAdminOnly();
    	return Response.ok(rideshareUserManager.listUsers().stream()
    			.map(u -> userMapper.map(u))
    			.collect(Collectors.toList())).build();
    }

    @Override
	public Response getUser(String userId) {
    	RideshareUser user = null;
    	try {
			user = resolveUserReference(userId);
			allowAdminOrCaller(user);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return Response.ok(userMapper.map(user)).build();
    }

	/***********************  CARS  **********************/
	
    /**
     * Lists the cars driven by the calling user.
     * @return an array of cars.
     */
    @Override
	public Response getUsersCars(String userId, Boolean deletedToo) {
		try {
	    	RideshareUser user = resolveUserReference(userId);
			allowAdminOrCaller(user);
			List<eu.netmobiel.rideshare.api.model.Car> cars = carManager.listCars(user, deletedToo).stream()
    			.map(u -> carMapper.mapMyCar(u))
    			.collect(Collectors.toList());
    		return Response.ok(cars).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    }

	@Override
	public Response createUsersCar(String userId, eu.netmobiel.rideshare.api.model.Car cardt) {
    	Response rsp = null;
		try {
			Car car = carMapper.map(cardt);
	    	RideshareUser user = resolveUserReference(userId);
			allowAdminOrCaller(user);
			String newCarId = UrnHelper.createUrn(Car.URN_PREFIX, carManager.createCar(user, car));
			rsp = Response.created(URI.create(newCarId)).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
    }
    
    @Override
    public Response getUsersCar(String userId, String carId) {
    	Car car = null;
    	try {
	    	RideshareUser user = resolveUserReference(userId);
			allowAdminOrCaller(user);
        	Long cid = UrnHelper.getId(Car.URN_PREFIX, carId);
			car = carManager.getCar(cid);
			allowAdminOrCaller(car.getDriver());
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return Response.ok(carMapper.map(car)).build();
    }

	@Override
	public Response updateUsersCar(String userId, String carId, eu.netmobiel.rideshare.api.model.Car cardt) {
    	Response rsp = null;
    	try {
	    	RideshareUser user = resolveUserReference(userId);
			allowAdminOrCaller(user);
        	Long cid = UrnHelper.getId(Car.URN_PREFIX, carId);
        	Car car = carMapper.map(cardt);
			Car cardb = carManager.getCar(cid);
			allowAdminOrCaller(user, cardb.getDriver());
        	carManager.updateCar(cid, car);
			rsp = Response.noContent().build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
    }

    @SuppressWarnings("resource")
	@Override
    public Response deleteUsersCar(String userId, String carId) {
    	Response rsp = null;
    	try {
	    	RideshareUser user = resolveUserReference(userId);
			allowAdminOrCaller(user);
        	Long cid = UrnHelper.getId(Car.URN_PREFIX, carId);
			Car cardb = carManager.getCar(cid);
			allowAdminOrCaller(user, cardb.getDriver());
        	carManager.removeCar(cid);
			rsp = Response.noContent().build();
		} catch (eu.netmobiel.commons.exception.NotFoundException e) {
	    	rsp = Response.status(Status.GONE).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
    }

}
