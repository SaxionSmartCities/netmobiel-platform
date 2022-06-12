package eu.netmobiel.rideshare.service;

import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.DuplicateEntryException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.rideshare.model.Car;
import eu.netmobiel.rideshare.model.RideshareUser;
import eu.netmobiel.rideshare.repository.CarDao;

@Stateless
@Logging
public class CarManager {

	@Inject
    protected Logger log;

    @Inject
    private CarDao carDao;
    
    public List<Car> listCars(RideshareUser driver, Boolean deletedToo) {
    	return carDao.findByDriver(driver, deletedToo);
    }

    public Long createCar(RideshareUser driver, Car car) throws DuplicateEntryException {
    	car.setDriver(driver);
    	car.setLicensePlateRaw(Car.unformatPlate(car.getLicensePlate()));
    	Optional<Car> optcar = carDao.findByDriverandPlate(driver, car.getRegistrationCountry(), car.getLicensePlateRaw());
    	if (optcar.isEmpty()) {
    		// New car for this user
        	carDao.save(car);
    	} else {
    		Car cardb = optcar.get();
    		if (cardb.isDeleted()) {
        		// Car was soft-deleted, but is restored
        		car.setId(cardb.getId());
        		carDao.merge(car);
    		} else {
    			// Car already exists for this user
    			throw new DuplicateEntryException(String.format("Car for user %s exists: %s", driver.getEmail(), cardb.getUrn()));
    		}
    	}
    	return car.getId();
    }

    /**
     * Retrieve information about a car. 
     * @param carId the primary key of the car.
     * @return The car object.
     * @throws NotFoundException if no car is present with that key.
     */
    public Car getCar(Long carId) throws NotFoundException {
    	Car cardb = carDao.find(carId)
    			.orElseThrow(() -> new NotFoundException("No such car: " + carId));
    	return cardb;
    }

    /**
     * Updates the care record. The driver cannot be changed.
     * @param carId
     * @param car
     * @throws NotFoundException
     */
    public void updateCar(Long carId, Car car) throws NotFoundException {
    	Car cardb = getCar(carId);
    	car.setId(cardb.getId());
    	car.setDriver(cardb.getDriver());
    	carDao.merge(car);
    }

    public void removeCar(Long carId) throws NotFoundException {
    	Car cardb = getCar(carId);
    	boolean inUse = carDao.getNrRideTemplatesAttached(cardb) > 0 || carDao.getNrRidesAttached(cardb) > 0;
    	if (inUse) {
    		// The car is referenced in a template and cannot be removed. Do a soft delete.
    		cardb.setDeleted(true);
    	} else {
    		carDao.remove(cardb);
    	}
    }

}
