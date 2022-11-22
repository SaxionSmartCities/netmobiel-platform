package eu.netmobiel.banker.service;

import java.util.Optional;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.Balance;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.repository.BalanceDao;
import eu.netmobiel.banker.repository.BankerUserDao;
import eu.netmobiel.commons.NetMobielModule;
import eu.netmobiel.commons.annotation.Created;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.service.UserManager;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.profile.service.ProfileManager;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@Lock(LockType.READ)
@Logging
public class BankerUserManager extends UserManager<BankerUserDao, BankerUser> {

	@Inject
    protected Logger log;

    @Inject
    private BankerUserDao userDao;
    
    @Inject
    private BalanceDao balanceDao;

	@Inject
    private ProfileManager profileManager;

    @Inject @Created
    private Event<BankerUser> userCreatedEvent;

	@Override
	protected BankerUserDao getUserDao() {
		return userDao;
	}
    
	@Override
	protected Logger getLogger() {
		return log;
	}

	@Override
	protected NetMobielUser findExternalUser(String managedIdentity) throws NotFoundException {
		return profileManager.getFlatProfileByManagedIdentity(managedIdentity);
	}

	@Override
	protected BankerUser enrichUser(BankerUser user) {
    	if (user.getPersonalAccount() == null) {
    		// Act like a new user is created
       		userCreatedEvent.fire(user);
    	}
		return user;
	}

    public BankerUser getUserWithBalance(Long id) throws NotFoundException {
    	BankerUser userdb = userDao.loadGraph(id, BankerUser.GRAPH_WITH_ACCOUNT)
    			.orElseThrow(() -> new NotFoundException("No such user: " + id));
    	if (userdb.getPersonalAccount() == null) {
    		throw new IllegalStateException("BankerUser has no personal account: " + id);
    	}
		// Add the balance too
		Balance balance = balanceDao.findActualBalance(userdb.getPersonalAccount());
		userdb.getPersonalAccount().setActualBalance(balance);
		if (userdb.getPremiumAccount() != null) {
			Balance premiumBalance = balanceDao.findActualBalance(userdb.getPremiumAccount());
			userdb.getPremiumAccount().setActualBalance(premiumBalance);
		}
    	return userdb;
    }
    
    public Account getPersonalAccount(Long id) throws NotFoundException {
    	BankerUser userdb = userDao.loadGraph(id, BankerUser.GRAPH_WITH_ACCOUNT)
    			.orElseThrow(() -> new NotFoundException("No such user: " + id));
    	if (userdb.getPersonalAccount() == null) {
    		throw new IllegalStateException("BankerUser has no personal account: " + id);
    	}
    	return userdb.getPersonalAccount();
    }

    @Lock(LockType.WRITE)
    public void updatePersonalUserAccount(Long userId, Account acc) throws NotFoundException {
    	BankerUser userdb = userDao.loadGraph(userId, BankerUser.GRAPH_WITH_ACCOUNT)
    			.orElseThrow(() -> new NotFoundException("No such charity: " + userId));
    	if (userdb.getPersonalAccount() == null) {
    		throw new IllegalStateException("BankerUser has no account: " + userId);
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
