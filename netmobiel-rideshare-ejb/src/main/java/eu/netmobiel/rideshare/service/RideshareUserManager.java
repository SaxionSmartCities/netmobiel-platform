package eu.netmobiel.rideshare.service;

import java.util.Optional;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.NetMobielModule;
import eu.netmobiel.commons.service.UserManager;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.rideshare.model.RideshareUser;
import eu.netmobiel.rideshare.repository.RideshareUserDao;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@Logging
public class RideshareUserManager extends UserManager<RideshareUserDao, RideshareUser>{

    @Inject
    private RideshareUserDao userDao;
    
	@Inject
    protected Logger log;

	@Override
	protected Logger getLogger() {
		return log;
	}

	@Override
	protected RideshareUserDao getUserDao() {
		return userDao;
	}

	@Override
	protected RideshareUser findCorCreateLoopback(RideshareUser user) {
		return sessionContext.getBusinessObject(this.getClass()).findCorCreate(user);
	}

	@Override
	protected Optional<String> resolveUrnPrefix(NetMobielModule module) {
    	return Optional.ofNullable(module == NetMobielModule.RIDESHARE ? RideshareUser.URN_PREFIX : null);
	}

}
