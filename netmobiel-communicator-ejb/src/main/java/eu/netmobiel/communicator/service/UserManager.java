package eu.netmobiel.communicator.service;

import java.security.Principal;
import java.util.List;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
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
import eu.netmobiel.communicator.model.User;
import eu.netmobiel.communicator.repository.UserDao;

@Stateless(name = "communicatorUserManager")
@Logging
public class UserManager {

	@Inject
    private Logger log;

    @Inject
    private UserDao userDao;
    
    @Resource
	private SessionContext ctx;

    protected User createContextUser() {
        User user = null;
    	Principal p = ctx.getCallerPrincipal();
    	if (p != null && p instanceof KeycloakPrincipal) {
    		@SuppressWarnings("unchecked")
			KeycloakPrincipal<KeycloakSecurityContext> kp = (KeycloakPrincipal<KeycloakSecurityContext>) p;
            KeycloakSecurityContext ksc = kp.getKeycloakSecurityContext(); 
//                log.debug("Is user in role admin? " +  httpReq.isUserInRole("admin"));
            AccessToken token = ksc.getToken();
            if (token != null) {
                user = new User();
                user.setManagedIdentity(token.getSubject()); // Same as kp.getName()
                user.setFamilyName(token.getFamilyName());
                user.setGivenName(token.getGivenName());
                userDao.save(user);
            }
    	}
    	if (log.isTraceEnabled()) {
    		log.trace("createCallingUser: " + (user != null ? user.toString() : "<null>"));
    	}
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
//    	Principal p = ctx.getCallerPrincipal();
    	User dbuser = userDao.findByManagedIdentity(user.getManagedIdentity())
    			.orElseGet(() -> userDao.save(user));
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
    			.orElse(createContextUser());
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
    
    public void throwRuntimeException() {
    	throw new RuntimeException("A bug in a EJB!");
    }
    public void throwAccessException() {
    	throw new EJBAccessException("Can't access this!");
    }
}
