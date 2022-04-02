package eu.netmobiel.commons.service;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.EJBAccessException;
import javax.ejb.EJBException;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.SessionContext;

import org.slf4j.Logger;

import eu.netmobiel.commons.NetMobielModule;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.CallingContext;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.NetMobielUserImpl;
import eu.netmobiel.commons.model.User;
import eu.netmobiel.commons.repository.UserDao;
import eu.netmobiel.commons.security.SecurityIdentity;
import eu.netmobiel.commons.util.UrnHelper;

public abstract class UserManager<D extends UserDao<T>, T extends User> {
    @Resource
	protected SessionContext sessionContext;

    protected abstract D getUserDao();
    protected abstract Logger getLogger();

	protected T createContextUser() {
    	Optional<NetMobielUser> nbuser = SecurityIdentity.getKeycloakContext(sessionContext.getCallerPrincipal());
    	return createUser(nbuser.isPresent() ? nbuser.get() : null);
    }
    
	protected T createUser(String managedIdentity) {
		NetMobielUserImpl nbuser = new NetMobielUserImpl();
		nbuser.setManagedIdentity(managedIdentity);
    	return createUser(nbuser);
	}
	
	protected T createUser(SecurityIdentity securityIdentity) {
    	return createUser(securityIdentity.getRealUser());
    }
    
	protected T createUser(NetMobielUser nbuser) {
    	if (getLogger().isTraceEnabled()) {
    		getLogger().trace("createContextUser: " + (nbuser != null ? nbuser.toString() : "<null>"));
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

	public void checkOwnership(T owner, String objectName) {
		T caller = findCallingUser();
    	if (caller == null || ! owner.getId().equals(caller.getId())) {
    		throw new SecurityException(objectName + " is not owned by calling user");
    	}
    }

	protected T enrichUser(T user) {
		return user;
	}

	protected abstract T findCorCreateLoopback(T user);
	
	protected Optional<T> loadUser(T user) {
		return getUserDao().find(user.getId());
	}

	/**
	 * Retrieves a user from an external source. Override this method in the subclass to enable a lookup elsewhere,
	 * e.g. the profile service.
	 *  
	 * @param managedIdentity the keyclooak id to look for.
	 * @return The user
	 * @throws NotFoundException If not found.
	 */
	protected NetMobielUser findExternalUser(String managedIdentity) throws NotFoundException {
		throw new NotFoundException("No such user: " + managedIdentity);
	}

	/**
     * Register the user, if not yet registered. This is a potential race condition if the client issues multiple requests in parallel.
     * Therefore this call is write protected. Because the user might already be created by a different thread while waiting for the lock,
     * another lookup is made before actually creating the user.
     * This is the equivalent of a test-and-set operation.
     * Do not call this method from application code.   
     * @param user the input record
     * @return the registered user.
     * @throws Exception
     */
	@Lock(LockType.WRITE)
	public T findCorCreate(T user) {
    	T dbuser = null;
    	dbuser = getUserDao().findByManagedIdentity(user.getManagedIdentity())
    			.orElse(null);
    	if (dbuser == null) {
    		// Ok, really create the user
    		dbuser = getUserDao().save(user);
    		dbuser = enrichUser(dbuser);
    	}
    	return dbuser;
    }

    /**
     * Try to lookup a user. If the user does not exist then create the user in another EJB call that is write protected. 
     *  
     * @return A user that exists in the local database.
     */
    public T findOrRegisterCallingUser() {
    	T caller = findCallingUser();
    	if (caller != null) {
    		if (caller.getId() == null) {
    			// Register the user in the database
        		caller = findCorCreateLoopback(caller);
    		}
    	} else {
        	throw new SecurityException("Unknown user");
    	}
    	return caller;
    }

    public T findOrRegisterUser(NetMobielUser nbuser) {
    	T caller = findUser(nbuser);
		if (caller.getId() == null) {
			// Register the user in the database
    		caller = findCorCreateLoopback(caller);
		}
    	return caller;
    }

   	public T findUser(NetMobielUser nbuser) {
    	return getUserDao().findByManagedIdentity(nbuser.getManagedIdentity())
    			.orElseGet(() -> createUser(nbuser));
    }

   	public T findCallingUser(SecurityIdentity securityIdentity) {
    	return getUserDao().findByManagedIdentity(securityIdentity.getPrincipal().getName())
    			.orElseGet(() -> createUser(securityIdentity));
    }

   	public T findCallingUser() {
    	return getUserDao().findByManagedIdentity(sessionContext.getCallerPrincipal().getName())
    			.orElseGet(() -> createContextUser());
    }
   	public T find(T user) {
    	return getUserDao().findByManagedIdentity(user.getManagedIdentity())
    			.orElse(user);
    }

   	public Optional<T> findByManagedIdentity(String identity) {
    	return getUserDao().findByManagedIdentity(identity);
    }

    public T findOrRegisterCallingUser(SecurityIdentity securityIdentity) {
		return findByManagedIdentity(securityIdentity.getPrincipal().getName())
				.orElseGet(() -> findCorCreateLoopback(createUser(securityIdentity.getRealUser())));
    }

    public CallingContext<T> findOrRegisterCallingContext(SecurityIdentity securityIdentity) throws NotFoundException {
		T caller = findByManagedIdentity(securityIdentity.getPrincipal().getName())
				.orElseGet(() -> findCorCreateLoopback(createUser(securityIdentity.getRealUser())));
		T effectiveUser = caller;
    	if (securityIdentity.isDelegationActive()) {
        	String effUserId = securityIdentity.getEffectivePrincipal().getName();
    		effectiveUser = findByManagedIdentity(effUserId).orElse(null);
    		if (effectiveUser == null) {
    			// Retrieve this user from the profile service
    			NetMobielUser effnbuser = findExternalUser(effUserId);
    			effectiveUser = findCorCreateLoopback(createUser(effnbuser));
    		}
    	}
    	return new CallingContext<>(caller, effectiveUser);
    }   
    
    public CallingContext<T> findCallingContext(SecurityIdentity securityIdentity) throws NotFoundException {
		T caller = findByManagedIdentity(securityIdentity.getPrincipal().getName()).orElse(null);
		T effectiveUser = caller;
    	if (securityIdentity.isDelegationActive()) {
        	String effUserId = securityIdentity.getEffectivePrincipal().getName();
    		effectiveUser = findByManagedIdentity(effUserId).orElse(null);
    	}
    	return new CallingContext<>(caller, effectiveUser);
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

    /**
     * Updates a user. 
     * @param uid The id of the user.
     * @return A user object.
     * @throws NotFoundException If the user does not exist.
     */
    public void updateUser(Long uid, NetMobielUser user) throws NotFoundException {
    	T usr = getUserDao().find(uid)
    			.orElseThrow(() -> new NotFoundException("No such user: " + uid));
    	copyUserData(usr, user);
    }

    private void copyUserData(T dbusr, NetMobielUser nbuser) {
    	dbusr.setEmail(nbuser.getEmail());
    	dbusr.setFamilyName(nbuser.getFamilyName());
    	dbusr.setGivenName(nbuser.getGivenName());
    }
    
    public void registerOrUpdateUser(NetMobielUser nbuser) {
    	T usr = findOrRegisterUser(nbuser);
    	copyUserData(usr, nbuser);
    }

    public void findAndUpdateUser(NetMobielUser nbuser) {
    	Optional<T> usr = findByManagedIdentity(nbuser.getManagedIdentity());
    	if (usr.isPresent()) {
    		copyUserData(usr.get(), nbuser);
    	}
    }

    protected abstract Optional<String> resolveUrnPrefix(NetMobielModule module);

    public Optional<T> resolveUrn(String userRef) throws BadRequestException {
    	Optional<T> user = null;
    	if (UrnHelper.isUrn(userRef)) {
        	NetMobielModule module = NetMobielModule.getEnum(UrnHelper.getService(userRef));
    	    if (module == NetMobielModule.KEYCLOAK) {
    		    String managedIdentity = UrnHelper.getSuffix(userRef);
    		    user = getUserDao().findByManagedIdentity(managedIdentity);
    	    } else {
    	    	String urnPrefix = resolveUrnPrefix(module)
    	    			.orElseThrow(() -> new IllegalArgumentException("Urn not supported: " + userRef));
    			Long did = UrnHelper.getId(urnPrefix, userRef);
        		user = getUserDao().find(did);
    	    }
    	} else {
			Long did = UrnHelper.getId(userRef);
    		user = getUserDao().find(did);
    	}
    	return user;
    }

	public void throwRuntimeException() {
    	throw new RuntimeException("A bug in a EJB!");
    }
	public void throwAccessException() {
    	throw new EJBAccessException("Can't access this!");
    }
}
