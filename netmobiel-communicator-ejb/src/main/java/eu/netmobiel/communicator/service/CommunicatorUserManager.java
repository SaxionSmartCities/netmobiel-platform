package eu.netmobiel.communicator.service;

import java.util.Optional;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.inject.Inject;

import eu.netmobiel.commons.NetMobielModule;
import eu.netmobiel.commons.service.UserManager;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.communicator.model.CommunicatorUser;
import eu.netmobiel.communicator.repository.CommunicatorUserDao;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Logging
public class CommunicatorUserManager extends UserManager<CommunicatorUserDao, CommunicatorUser> {

    @Inject
    private CommunicatorUserDao userDao;

	@Override
	protected CommunicatorUserDao getUserDao() {
		return userDao;
	}

	@Override
	protected CommunicatorUser findCorCreateLoopback(CommunicatorUser user) {
		return sessionContext.getBusinessObject(this.getClass()).findCorCreate(user);
	}

	@Override
	protected Optional<String> resolveUrnPrefix(NetMobielModule module) {
    	return Optional.ofNullable(module == NetMobielModule.COMMUNICATOR ? CommunicatorUser.URN_PREFIX : null);
	}
    
}