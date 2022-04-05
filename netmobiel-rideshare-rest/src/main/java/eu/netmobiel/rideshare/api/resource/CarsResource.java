package eu.netmobiel.rideshare.api.resource;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.rideshare.api.CarsApi;
import eu.netmobiel.rideshare.api.mapping.CarMapper;
import eu.netmobiel.rideshare.model.Car;
import eu.netmobiel.rideshare.service.RideshareUserManager;

@RequestScoped
public class CarsResource implements CarsApi {

	@Inject
    private CarMapper mapper;

    @Inject
    private RideshareUserManager userManager;

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

    @SuppressWarnings("resource")
	@Override
	public Response createCar(eu.netmobiel.rideshare.api.model.Car cardt) {
    	Response rsp = null;
		try {
			Car car = mapper.map(cardt);
			String newCarId = UrnHelper.createUrn(Car.URN_PREFIX, userManager.createCar(car));
			rsp = Response.created(URI.create(newCarId)).build();
		} catch (CreateException e) {
			rsp = Response.status(Status.CONFLICT).build();
		}
    	return rsp;
    }
    
    @Override
    public Response getCar(@PathParam("carId") String carId) {
    	Car car = null;
    	try {
        	Long cid = UrnHelper.getId(Car.URN_PREFIX, carId);
			car = userManager.getCar(cid);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return Response.ok(mapper.map(car)).build();
    }

	@Override
	public Response updateCar(@PathParam("carId") String carId, eu.netmobiel.rideshare.api.model.Car cardt) {
    	Response rsp = null;
    	try {
        	Long cid = UrnHelper.getId(Car.URN_PREFIX, carId);
        	Car car = mapper.map(cardt);
			userManager.updateCar(cid, car);
			rsp = Response.noContent().build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
    }

    @SuppressWarnings("resource")
	@Override
    public Response deleteCar(@PathParam("carId") String carId) {
    	Response rsp = null;
    	try {
        	Long cid = UrnHelper.getId(Car.URN_PREFIX, carId);
			userManager.removeCar(cid);
			rsp = Response.noContent().build();
		} catch (eu.netmobiel.commons.exception.NotFoundException e) {
	    	rsp = Response.status(Status.GONE).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp;
    }
}
