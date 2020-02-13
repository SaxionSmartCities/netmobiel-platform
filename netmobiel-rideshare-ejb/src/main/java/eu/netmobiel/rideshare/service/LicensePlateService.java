package eu.netmobiel.rideshare.service;

import java.io.IOException;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.rideshare.model.Car;
import eu.netmobiel.rideshare.repository.RDWCarLicenseDao;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class LicensePlateService {
    @Inject
    private Logger log;

    @Inject
    private RDWCarLicenseDao rdwDao;
    public Car fetchLicensePlateInformation(String country, String plate) throws IOException, NotFoundException {
		Car car = null;
    	if (country.toUpperCase().equals("NL")) {
    		car = rdwDao.fetchDutchLicensePlateInformation(plate);
    	} else {
    		log.warn("Country not supported: " + country);
    	}
    	if (car == null) {
    		throw new NotFoundException(String.format("No car found with license %s %s", country.toUpperCase(), plate.toUpperCase()));
    	} else {
    		car.setRegistrationCountry(country.toUpperCase());
    	}
        return car;
    }

}
