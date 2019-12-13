package eu.netmobiel.rideshare.api.resource;

import java.io.IOException;

import javax.ejb.ObjectNotFoundException;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import com.github.dozermapper.core.Mapper;

import eu.netmobiel.rideshare.api.CarLicensesApi;
import eu.netmobiel.rideshare.model.Car;
import eu.netmobiel.rideshare.service.LicensePlateService;

/**
 * License plate decoder API.
 */
@ApplicationScoped
public class CarLicenseResource implements CarLicensesApi {

    @SuppressWarnings("unused")
	@Inject
    private Logger log;

    @Inject
    private Mapper mapper;

    @Inject
    private LicensePlateService licensePlateService;
    
    @Override
	public Response getCarLicense(String country, String plate) {
    	Car car = null;
    	if (country == null || country.isEmpty()) {
			throw new BadRequestException("Parameter 'country' is mandatory");
    	}
    	if (plate == null || plate.isEmpty()) {
			throw new BadRequestException("Parameter 'plate' is mandatory");
    	}
		try {
			car = licensePlateService.fetchLicensePlateInformation(country, plate);
		} catch (ObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (IOException e) {
			throw new ServiceUnavailableException("Unable to retrieve license plate information", 0L, e);
		}
        return Response.ok(mapper.map(car,  eu.netmobiel.rideshare.api.model.Car.class)).build();
    }

}
