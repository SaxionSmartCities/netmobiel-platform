package eu.netmobiel.rideshare.api.resource;

import java.util.List;
import java.util.stream.Collectors;

import javax.ejb.CreateException;
import javax.ejb.ObjectNotFoundException;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import eu.netmobiel.rideshare.api.CarsApi;
import eu.netmobiel.rideshare.api.mapping.CarMapper;
import eu.netmobiel.rideshare.model.Car;
import eu.netmobiel.rideshare.service.UserManager;
import eu.netmobiel.rideshare.util.RideshareUrnHelper;

@ApplicationScoped
public class CarsResource implements CarsApi {

	@Inject
    private CarMapper mapper;

    @Inject
    private UserManager userManager;

    /**
     * Lists the cars driven by the calling user.
     * @return an array of cars.
     */
    @Override
	public Response getCars(Boolean deletedToo) {
    	List<eu.netmobiel.rideshare.api.model.Car> cars = userManager.listMyCars(deletedToo).stream()
    			.map(u -> mapper.mapMyCar(u))
    			.collect(Collectors.toList());
    	return Response.ok(cars).build();
    }

    @Override
	public Response createCar(eu.netmobiel.rideshare.api.model.Car cardt) {
    	Response rsp = null;
		try {
			Car car = mapper.map(cardt);
			String newCarId = RideshareUrnHelper.createUrn(Car.URN_PREFIX, userManager.createCar(car));
			rsp = Response.created(UriBuilder.fromPath("{arg1}").build(newCarId)).build();
		} catch (CreateException e) {
			rsp = Response.status(Status.CONFLICT).build();
		}
    	return rsp;
    }
    
    @Override
    public Response getCar(@PathParam("carId") String carId) {
    	Car car = null;
    	try {
        	Long cid = RideshareUrnHelper.getId(Car.URN_PREFIX, carId);
			car = userManager.getCar(cid);
		} catch (ObjectNotFoundException e) {
			throw new NotFoundException();
		}
    	return Response.ok(mapper.map(car)).build();
    }

    @Override
	public Response updateCar(@PathParam("carId") String carId, eu.netmobiel.rideshare.api.model.Car cardt) {
    	Response rsp = null;
    	try {
        	Long cid = RideshareUrnHelper.getId(Car.URN_PREFIX, carId);
        	Car car = mapper.map(cardt);
			userManager.updateCar(cid, car);
			rsp = Response.noContent().build();
		} catch (ObjectNotFoundException e) {
			rsp = Response.status(Status.NOT_FOUND).build();
		}
    	return rsp;
    }

    @Override
    public Response deleteCar(@PathParam("carId") String carId) {
    	Response rsp = null;
    	try {
        	Long cid = RideshareUrnHelper.getId(Car.URN_PREFIX, carId);
			userManager.removeCar(cid);
			rsp = Response.noContent().build();
		} catch (ObjectNotFoundException e) {
	    	rsp = Response.status(Status.GONE).build();
		}
    	return rsp;
    }
}
