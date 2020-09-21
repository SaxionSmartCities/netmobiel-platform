package eu.netmobiel.banker.service;

import java.util.Optional;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import eu.netmobiel.banker.model.BankerUser;
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
     * Retrieves a specific user and balance details. 
     * @param uid The id of the user.
     * @return A user object.
     * @throws FoundException If the user does not exist.
     */
    public BankerUser getUserWithBalance(Long uid) throws NotFoundException {
    	return userDao.find(uid, userDao.createLoadHint(BankerUser.GRAPH_WITH_BALANCE))
    			.orElseThrow(() -> new NotFoundException("No such user: " + uid));
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
