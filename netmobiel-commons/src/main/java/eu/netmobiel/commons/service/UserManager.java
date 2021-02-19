package eu.netmobiel.commons.service;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.EJBAccessException;
import javax.ejb.EJBException;
import javax.ejb.SessionContext;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.slf4j.Logger;

import eu.netmobiel.commons.NetMobielModule;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.User;
import eu.netmobiel.commons.repository.UserDao;
import eu.netmobiel.commons.security.SecurityContextHelper;
import eu.netmobiel.commons.util.UrnHelper;

public abstract class UserManager<D extends UserDao<T>, T extends User> {
    @Resource
	protected SessionContext sessionContext;

    protected abstract D getUserDao();
    protected abstract Logger getLogger();

	protected T createContextUser() {
    	NetMobielUser nbuser = SecurityContextHelper.getUserContext(sessionContext.getCallerPrincipal());
    	if (getLogger().isTraceEnabled()) {
    		getLogger().trace("createCallingUser: " + (nbuser != null ? nbuser.toString() : "<null>"));
    	}
    	T user = null;
    	if (nbuser != null) {
    		try {
				user = getUserDao().getPersistentClass().getDeclaredConstructor().newInstance();
	    		user.from(nbuser);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
				throw new EJBException(e);
			}
    	}
    	return user;
    }
    
	protected T createContextUser(String managedIdentity) {
    	T user = null;
		try {
			user = getUserDao().getPersistentClass().getDeclaredConstructor().newInstance();
			user.setManagedIdentity(managedIdentity);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new EJBException(e);
		}
		return user;
	}
	
	public void checkOwnership(T owner, String objectName) {
		T caller = findCallingUser();
    	if (caller == null || ! owner.getId().equals(caller.getId())) {
    		throw new SecurityException(objectName + " is not owned by calling user");
    	}
    }

	protected T enrichUser(T user) {
		return user;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public synchronized T findCorCreate(T user) {
    	user.setId(null);
    	return getUserDao().findByManagedIdentity(user.getManagedIdentity())
	    			.orElseGet(() -> getUserDao().save(user));
    }
    
	protected abstract T findCorCreateLoopback(T user);
	
	protected Optional<T> loadUser(T user) {
		return getUserDao().find(user.getId());
	}

	/**
     * Register the user, if not yet registered. This is a potential race condition if the client issues multiple requests in parallel.
     * @param user the input record
     * @return the registered user.
     * @throws Exception
     */
	//FIXME This call should be synchronized or lock.write
    public T register(T user) {
    	T dbuser = null;
    	dbuser = getUserDao().findByManagedIdentity(user.getManagedIdentity())
    			.orElse(null);
    	if (dbuser == null) {
//    		dbuser = ctx.getBusinessObject(this.getClass()).findCorCreate(user);
    		T dbuser2 = findCorCreateLoopback(user);
    		// Get the just created in the current persistence context
    		dbuser = loadUser(dbuser2).orElseThrow(() -> new IllegalStateException("Still no user: " + dbuser2));
    	} else {
	    	dbuser.setFamilyName(user.getFamilyName()); 
	    	dbuser.setGivenName(user.getGivenName());
	    	dbuser.setEmail(user.getEmail());
    	}
    	return enrichUser(dbuser);
    }

    public T registerCallingUser() {
    	T caller = createContextUser();
    	if (caller != null) {
    		caller = register(caller);
    	} else {
        	throw new SecurityException("Unknown user");
    	}
    	return caller;
    }

    public boolean isCallingUser(String identity) {
    	return identity.equals(sessionContext.getCallerPrincipal().getName());
    }

   	public T findCallingUser() {
    	return getUserDao().findByManagedIdentity(sessionContext.getCallerPrincipal().getName())
    			.orElseGet(() -> createContextUser());
    }

   	public T find(T user) {
    	return getUserDao().findByManagedIdentity(user.getManagedIdentity())
    			.orElse(user);
    }

    /**
     * Retrieves all users.
     * @return a list of User objects.
     */
    @PermitAll
    public List<T> listUsers() {
    	return getUserDao().findAll();
    }

    /**
     * Retrieves all users.
     * @return a list of User objects.
     */
    @PermitAll
    public List<String> listManagedIdentities() {
    	return getUserDao().listManagedIdentities();
    }

    /**
     * Retrieves a specific user. 
     * @param uid The id of the user.
     * @return A user object.
     * @throws NotFoundException If the user does not exist.
     */
    public T getUser(Long uid) throws NotFoundException {
    	return getUserDao().find(uid)
    			.orElseThrow(() -> new NotFoundException("No such user: " + uid));
    }
    
    protected abstract Optional<String> resolveUrnPrefix(NetMobielModule module);

    public Optional<T> resolveUrn(String userRef) {
    	T user = null;
    	if (UrnHelper.isUrn(userRef)) {
        	NetMobielModule module = NetMobielModule.getEnum(UrnHelper.getService(userRef));
    	    if (module == NetMobielModule.KEYCLOAK) {
    		    String managedIdentity = UrnHelper.getSuffix(userRef);
    		    user = getUserDao().findByManagedIdentity(managedIdentity).orElseGet(() -> createContextUser(managedIdentity));
    	    } else {
    	    	String urnPrefix = resolveUrnPrefix(module)
    	    			.orElseThrow(() -> new IllegalArgumentException("Urn not supported: " + userRef));
    			Long did = UrnHelper.getId(urnPrefix, userRef);
        		user = getUserDao().find(did).orElse(null);
    	    }
    	} else {
			Long did = UrnHelper.getId(userRef);
    		user = getUserDao().find(did).orElse(null);
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
