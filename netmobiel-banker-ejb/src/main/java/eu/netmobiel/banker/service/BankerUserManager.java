package eu.netmobiel.banker.service;

import java.util.Optional;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJBAccessException;
import javax.ejb.Singleton;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.Balance;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.repository.BalanceDao;
import eu.netmobiel.banker.repository.BankerUserDao;
import eu.netmobiel.commons.NetMobielModule;
import eu.netmobiel.commons.annotation.Created;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.service.UserManager;
import eu.netmobiel.commons.util.Logging;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Logging
public class BankerUserManager extends UserManager<BankerUserDao, BankerUser> {

    @Inject
    private BankerUserDao userDao;
    
    @Inject
    private BalanceDao balanceDao;

    @Inject @Created
    private Event<BankerUser> userCreatedEvent;

	@Override
	protected BankerUserDao getUserDao() {
		return userDao;
	}
    
    @Override
	protected BankerUser enrichUser(BankerUser user) {
    	if (user.getPersonalAccount() == null) {
    		// Act like a new user is created
       		userCreatedEvent.fire(user);
    	}
		return user;
	}

    /**
     * Retrieves a user. Anyone can read a user record, but the account of the user is only accessible to
     * the user and the administrators.
     * @param id the user id
     * @return a banker user object
     * @throws NotFoundException No matching user found.
     */
    public BankerUser getUser(Long id) throws NotFoundException {
    	BankerUser userdb = userDao.loadGraph(id, BankerUser.GRAPH_WITH_ACCOUNT)
    			.orElseThrow(() -> new NotFoundException("No such user: " + id));
    	if (userdb.getPersonalAccount() == null) {
    		throw new IllegalStateException("BankerUser has no account: " + id);
    	}
    	// Assure changes are not propagated to the database.
    	userDao.detach(userdb);
    	String caller = sessionContext.getCallerPrincipal().getName();
		boolean admin = sessionContext.isCallerInRole("admin");
		if (!admin && !userdb.getManagedIdentity().equals(caller)) {
			// Roles and Account are privileged
			userdb.setPersonalAccount(null);
		} else {
			// Add the balance too
			Balance balance = balanceDao.findActualBalance(userdb.getPersonalAccount());
			userdb.getPersonalAccount().setActualBalance(balance);
		}
    	return userdb;
    }

    public BankerUser getUserWithBalance(Long id) throws NotFoundException {
    	BankerUser user = getUser(id);
    	if (user.getPersonalAccount() == null) {
    		throw new EJBAccessException("Read access to user account not allowed");
    	}
    	return user;
    }
    
    public Account getPersonalAccount(Long id) throws NotFoundException {
    	BankerUser userdb = getUser(id);
    	if (userdb.getPersonalAccount() == null) {
    		throw new EJBAccessException("Read access to user account not allowed");
    	}
    	return userdb.getPersonalAccount();
    }

    public void updatePersonalUserAccount(Long userId, Account acc) throws NotFoundException {
    	BankerUser userdb = userDao.loadGraph(userId, BankerUser.GRAPH_WITH_ACCOUNT)
    			.orElseThrow(() -> new NotFoundException("No such charity: " + userId));
    	if (userdb.getPersonalAccount() == null) {
    		throw new IllegalStateException("BankerUser has no account: " + userId);
    	}
    	String caller = sessionContext.getCallerPrincipal().getName();
		boolean admin = sessionContext.isCallerInRole("admin");
		if (!admin && !userdb.getManagedIdentity().equals(caller)) {
    		throw new EJBAccessException("Write access to personal user account not allowed");
		}
    	Account accdb = userdb.getPersonalAccount();
    	// Set only specific attributes
    	accdb.setIban(acc.getIban());
    	accdb.setIbanHolder(acc.getIbanHolder());
    	accdb.setName(acc.getName());
    }

    @Override
	protected BankerUser findCorCreateLoopback(BankerUser user) {
		return sessionContext.getBusinessObject(this.getClass()).findCorCreate(user);
	}

	@Override
	protected Optional<String> resolveUrnPrefix(NetMobielModule module) {
    	return Optional.ofNullable(module == NetMobielModule.BANKER ? BankerUser.URN_PREFIX : null);
    }
}
