package eu.netmobiel.rideshare.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.inject.Inject;

import eu.netmobiel.commons.NetMobielModule;
import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.commons.exception.DuplicateEntryException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.service.UserManager;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.rideshare.model.Car;
import eu.netmobiel.rideshare.model.RideshareUser;
import eu.netmobiel.rideshare.repository.CarDao;
import eu.netmobiel.rideshare.repository.RideshareUserDao;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Logging
public class RideshareUserManager extends UserManager<RideshareUserDao, RideshareUser>{

    @Inject
    private RideshareUserDao userDao;
    
    @Inject
    private CarDao carDao;
    
	@Override
	protected RideshareUserDao getUserDao() {
		return userDao;
	}

	@Override
	protected RideshareUser findCorCreateLoopback(RideshareUser user) {
		return sessionContext.getBusinessObject(this.getClass()).findCorCreate(user);
	}

	@Override
	protected Optional<String> resolveUrnPrefix(NetMobielModule module) {
    	return Optional.ofNullable(module == NetMobielModule.RIDESHARE ? RideshareUser.URN_PREFIX : null);
	}
    
    public List<Car> listMyCars(Boolean deletedToo) {
    	List<Car> cars = Collections.emptyList();
    	RideshareUser caller = findCallingUser();
    	if (caller != null && caller.getId() != null) {
    		cars = carDao.findByDriver(caller, deletedToo);
    	}
    	return cars;
    }

    public Long createCar(Car car) throws CreateException {
    	RideshareUser caller = registerCallingUser();
    	car.setDriver(caller);
    	Car cardb = carDao.findByDriverandPlate(caller, car.getRegistrationCountry(), car.getLicensePlate())
    				.orElse(null);
    	if (cardb == null) {
    		// New car for this user
        	carDao.save(car);
    	} else if (Boolean.TRUE.equals(cardb.getDeleted())) {
    		// Car was soft-deleted, but is restored
    		car.setId(cardb.getId());
    		car.setDriver(caller);
    		carDao.merge(car);
    	} else {
    		// Car already exists for this user
    		throw new DuplicateEntryException("Car exists" + car.toString());
    	}
    	return car.getId();
    }

    /**
     * Retrieve information about a car. Anyone can read the data.
     * @param carId the primary key of the car.
     * @return The car object.
     * @throws NotFoundException if no car is present with that key.
     */
    public Car getCar(Long carId) throws NotFoundException {
    	Car cardb = carDao.find(carId)
    			.orElseThrow(NotFoundException::new);
    	return cardb;
    }

    public void updateCar(Long carId, Car car) throws NotFoundException {
    	Car cardb = carDao.find(carId)
    			.orElseThrow(NotFoundException::new);
    	checkOwnership(cardb.getDriver(), Car.class.getSimpleName());
    	car.setId(cardb.getId());
    	car.setDriver(cardb.getDriver());
    	carDao.merge(car);
    }

    public void removeCar(Long carId) throws NotFoundException {
    	Car cardb = carDao.find(carId)
    			.orElseThrow(NotFoundException::new);
    	checkOwnership(cardb.getDriver(), Car.class.getSimpleName());
    	boolean inUse = carDao.getNrRideTemplatesAttached(cardb) > 0 || carDao.getNrRideTemplatesAttached(cardb) > 0;
    	if (inUse) {
    		// The car is referenced in a template and cannot be removed. Do a soft delete.
    		cardb.setDeleted(true);
    	} else {
    		carDao.remove(cardb);
    	}
    }

}
