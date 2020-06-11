package eu.netmobiel.planner.service;

import java.util.List;
import java.util.Optional;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.EJBAccessException;
import javax.ejb.ObjectNotFoundException;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.NetMobielModule;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.security.SecurityContextHelper;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.planner.model.User;
import eu.netmobiel.planner.repository.UserDao;
import eu.netmobiel.planner.util.PlannerUrnHelper;

@Stateless(name = "plannerUserManager")
@Logging
public class UserManager {

	@Inject
    private Logger log;

    @Inject
    private UserDao userDao;
    
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
//    	Principal p = ctx.getCallerPrincipal();
    	User dbuser = userDao.findByManagedIdentity(user.getManagedIdentity())
    			.orElseGet(() -> {
    				user.setId(null);
    				return userDao.save(user);	
    			});
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
    
    public User findCallingUser() {
    	return userDao.findByManagedIdentity(ctx.getCallerPrincipal().getName())
    			.orElseGet(() -> createContextUser());
    }
    public User find(User user) {
    	return userDao.findByManagedIdentity(user.getManagedIdentity())
    			.orElse(user);
    }

    /**
     * Retrieves all users.
     * @return a list of User objects.
     */
    @PermitAll
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
    
    public Optional<User> resolveUrn(String userRef) {
    	User user = null;
    	if (UrnHelper.isUrn(userRef)) {
        	NetMobielModule module = NetMobielModule.getEnum(UrnHelper.getService(userRef));
        	if (module == NetMobielModule.PLANNER) {
    			Long did = PlannerUrnHelper.getId(User.URN_PREFIX, userRef);
        		user = userDao.find(did).orElse(null);
        	} else if (module == NetMobielModule.KEYCLOAK) {
        		String managedIdentity = UrnHelper.getSuffix(userRef);
        		user = userDao.findByManagedIdentity(managedIdentity).orElseGet(() -> new User(managedIdentity, null, null));
        	}
    	} else {
			Long did = PlannerUrnHelper.getId(User.URN_PREFIX, userRef);
    		user = userDao.find(did).orElse(null);
    	}
    	return Optional.ofNullable(user);
    }

    public void throwRuntimeException() {
    	throw new RuntimeException("A bug in a EJB!");
    }
    public void throwAccessException() {
    	throw new EJBAccessException("Can't access this!");
    }
}
