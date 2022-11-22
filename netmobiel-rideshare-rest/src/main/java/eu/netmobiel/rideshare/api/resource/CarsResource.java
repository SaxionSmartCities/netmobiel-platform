package eu.netmobiel.rideshare.api.resource;

import java.io.IOException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.rideshare.api.CarsApi;
import eu.netmobiel.rideshare.api.mapping.CarMapper;
import eu.netmobiel.rideshare.model.Car;
import eu.netmobiel.rideshare.service.CarManager;
import eu.netmobiel.rideshare.service.CarRegistrarService;

@RequestScoped
public class CarsResource extends RideshareResource implements CarsApi {

	@Inject
    private CarMapper mapper;

    @Inject
    private CarManager carManager;

    @Inject
    private CarRegistrarService carRegistrarService;
    
	@Override
	public Response lookupCarRegistration(String countryCode, String licensePlate) {
    	Car car = null;
    	if (countryCode == null || countryCode.isBlank()) {
			throw new BadRequestException("Parameter 'countryCode' is mandatory");
    	}
    	if (licensePlate == null || licensePlate.isBlank()) {
			throw new BadRequestException("Parameter 'licensePlate' is mandatory");
    	}
		try {
			car = carRegistrarService.fetchLicensePlateInformation(countryCode, licensePlate);
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		} catch (IOException e) {
			throw new ServiceUnavailableException("Unable to retrieve license plate information", 0L, e);
		}
        return Response.ok(mapper.map(car)).build();
	}

    @Override
    public Response getCar(String carId) {
    	Response rsp = null;
    	try {
        	Long cid = UrnHelper.getId(Car.URN_PREFIX, carId);
			Car car = carManager.getCar(cid);
			eu.netmobiel.rideshare.api.model.Car apiCar = isPrivileged() 
					? mapper.map(car)
					: mapper.mapMyCar(car);
			rsp = Response.ok(apiCar).build();
		} catch (BusinessException e) {
			throw new WebApplicationException(e);
		}
    	return rsp; 
    }

}
