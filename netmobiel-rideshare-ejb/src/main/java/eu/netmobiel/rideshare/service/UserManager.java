package eu.netmobiel.rideshare.service;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.CreateException;
import javax.ejb.DuplicateKeyException;
import javax.ejb.EJBAccessException;
import javax.ejb.ObjectNotFoundException;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.slf4j.Logger;

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
    
    // TODO Find out how to propagate security to EJB.
    @Resource
	private SessionContext ctx;

    protected User createContextUser() {
        User user = null;
    	Principal p = ctx.getCallerPrincipal();
    	if (p != null && p instanceof KeycloakPrincipal) {
    		log.debug("Principal name: " + p.getName());
    		@SuppressWarnings("unchecked")
			KeycloakPrincipal<KeycloakSecurityContext> kp = (KeycloakPrincipal<KeycloakSecurityContext>) p;
            KeycloakSecurityContext ksc = kp.getKeycloakSecurityContext(); 
//                log.debug("Is user in role admin? " +  httpReq.isUserInRole("admin"));
            AccessToken token = ksc.getToken();
            if (token != null) {
                user = new User();
                user.setManagedIdentity(token.getSubject()); // Same as kp.getName()
                user.setEmail(token.getEmail());
                user.setFamilyName(token.getFamilyName());
                user.setGivenName(token.getGivenName());
            }
    	}
    	log.debug("createCallingUser: " + (user != null ? user.toString() : "<null>"));
    	return user;
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
    			.orElseGet(() -> userDao.save(user));
    	dbuser.setEmail(user.getEmail()); 
    	dbuser.setFamilyName(user.getFamilyName()); 
    	dbuser.setGivenName(user.getGivenName()); 
    	return dbuser;
    }

    public User registerCallingUser() {
    	User caller = findCallingUser();
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
    			.orElse(createContextUser());
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
     * @throws ObjectNotFoundException If the user does not exist.
     */
    public User getUser(Long uid) throws ObjectNotFoundException {
    	return userDao.find(uid)
    			.orElseThrow(ObjectNotFoundException::new);
    }
    
    public List<Car> listMyCars() {
    	List<Car> cars = Collections.emptyList();
    	User caller = findCallingUser();
    	if (caller != null) {
    		cars = carDao.findByDriver(caller);
    	}
    	return cars;
    }

    public Long createCar(Car car) throws CreateException {
    	User caller = registerCallingUser();
    	car.setDriver(caller);
    	if (carDao.exists(car)) {
    		throw new DuplicateKeyException("Car exists" + car.toString());
    	}
    	carDao.save(car);
    	return car.getId();
    }

    /**
     * Retrieve information about a car. Anyone can read the data.
     * @param carId the primary key of the car.
     * @return The car object.
     * @throws ObjectNotFoundException if no car is present with that key.
     */
    public Car getCar(Long carId) throws ObjectNotFoundException {
    	Car cardb = carDao.find(carId)
    			.orElseThrow(ObjectNotFoundException::new);
    	return cardb;
    }

    public void updateCar(Long carId, Car car) throws ObjectNotFoundException {
    	Car cardb = carDao.find(carId)
    			.orElseThrow(ObjectNotFoundException::new);
    	checkOwnership(cardb.getDriver(), Car.class.getSimpleName());
    	car.setId(cardb.getId());
    	car.setDriver(cardb.getDriver());
    	carDao.merge(car);
    }

    public void removeCar(Long carId) throws ObjectNotFoundException {
    	Car cardb = carDao.find(carId)
    			.orElseThrow(ObjectNotFoundException::new);
    	checkOwnership(cardb.getDriver(), Car.class.getSimpleName());
		carDao.remove(cardb);
    }
    
    public void throwRuntimeException() {
    	throw new RuntimeException("A bug in a EJB!");
    }
    public void throwAccessException() {
    	throw new EJBAccessException("Can't access this!");
    }
}
