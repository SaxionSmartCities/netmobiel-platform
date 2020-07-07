package eu.netmobiel.rideshare.service;

import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.EJBAccessException;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.commons.exception.DuplicateEntryException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.security.SecurityContextHelper;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.rideshare.model.Car;
import eu.netmobiel.rideshare.model.User;
import eu.netmobiel.rideshare.repository.CarDao;
import eu.netmobiel.rideshare.repository.UserDao;

@Stateless
@Logging
public class UserManager {

	@Inject
    private Logger log;

    @Inject
    private UserDao userDao;
    
    @Inject
    private CarDao carDao;
    
    @Resource
	private SessionContext ctx;

    protected User createContextUser() {
    	NetMobielUser nbuser = SecurityContextHelper.getUserContext(ctx.getCallerPrincipal());
    	if (log.isTraceEnabled()) {
    		log.trace("createCallingUser: " + (nbuser != null ? nbuser.toString() : "<null>"));
    	}
    	return nbuser != null ? new User(nbuser) : null;
    }
    
	public void checkOwnership(User owner, String objectName) {
		User caller = findCallingUser();
    	if (caller == null || ! owner.getId().equals(caller.getId())) {
    		throw new SecurityException(objectName + " is not owned by calling user");
    	}
    }

	/**
     * 
     * Register the user, if not yet registered.
     * @param user the input record
     * @return the registered user.
     * @throws Exception
     */
    public User register(User user) {
    	User dbuser = userDao.findByManagedIdentity(user.getManagedIdentity())
    			.orElseGet(() -> {
    				user.setId(null);
    				return userDao.save(user); 
    			});
    	dbuser.setEmail(user.getEmail()); 
    	dbuser.setFamilyName(user.getFamilyName()); 
    	dbuser.setGivenName(user.getGivenName()); 
    	return dbuser;
    }

    /**
     * 
     * Register the user, if not yet registered.
     * @param user the input record
     * @return the registered user.
     * @throws Exception
     */
    public User register(NetMobielUser user) {
    	User dbuser = userDao.findByManagedIdentity(user.getManagedIdentity())
    			.orElseGet(() -> userDao.save(new User(user)));
    	dbuser.setEmail(user.getEmail()); 
    	dbuser.setFamilyName(user.getFamilyName()); 
    	dbuser.setGivenName(user.getGivenName()); 
    	return dbuser;
    }
    
    public User registerCallingUser() {
    	User caller = createContextUser();
    	if (caller != null) {
    		caller = register(caller);
    	} else {
        	throw new SecurityException("Unknown user");
    	}
    	return caller;
    }
    
    public User find(User user) {
    	return userDao.findByManagedIdentity(user.getManagedIdentity())
    			.orElse(user);
    }

    public User findCallingUser() {
    	return userDao.findByManagedIdentity(ctx.getCallerPrincipal().getName())
    			.orElseGet(() -> createContextUser());
    }

    /**
     * Retrieves all users.
     * @return a list of User objects.
     */
    public List<User> listUsers() {
    	return userDao.findAll();
    }

    /**
     * Retrieves a specific user. 
     * @param uid The id of the user.
     * @return A user object.
     * @throws NotFoundException If the user does not exist.
     */
    public User getUser(Long uid) throws NotFoundException {
    	return userDao.find(uid)
    			.orElseThrow(NotFoundException::new);
    }
    
    public List<Car> listMyCars(Boolean deletedToo) {
    	List<Car> cars = Collections.emptyList();
    	User caller = findCallingUser();
    	if (caller != null && caller.getId() != null) {
    		cars = carDao.findByDriver(caller, deletedToo);
    	}
    	return cars;
    }

    public Long createCar(Car car) throws CreateException {
    	User caller = registerCallingUser();
    	car.setDriver(caller);
    	if (carDao.exists(car)) {
    		throw new DuplicateEntryException("Car exists" + car.toString());
    	}
    	carDao.save(car);
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
    	Long nrRefs = carDao.getNrRideTemplatesAttached(cardb);
    	if (nrRefs > 0) {
    		// The car is referenced in a template and cannot be removed. Do a soft delete.
    		cardb.setDeleted(true);
    	} else {
    		carDao.remove(cardb);
    	}
    }
    
    public void throwRuntimeException() {
    	throw new RuntimeException("A bug in a EJB!");
    }
    public void throwAccessException() {
    	throw new EJBAccessException("Can't access this!");
    }
}
