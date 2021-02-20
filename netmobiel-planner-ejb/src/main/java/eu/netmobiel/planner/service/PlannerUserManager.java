package eu.netmobiel.planner.service;

import java.util.Optional;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.NetMobielModule;
import eu.netmobiel.commons.service.UserManager;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.planner.model.PlannerUser;
import eu.netmobiel.planner.repository.PlannerUserDao;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@Logging
public class PlannerUserManager extends UserManager<PlannerUserDao, PlannerUser> {

    @Inject
    private PlannerUserDao userDao;

	@Inject
    protected Logger log;

	@Override
	protected Logger getLogger() {
		return log;
	}

    @Override
	protected PlannerUserDao getUserDao() {
		return userDao;
	}
    
	@Override
	protected PlannerUser findCorCreateLoopback(PlannerUser user) {
		return sessionContext.getBusinessObject(this.getClass()).findCorCreate(user);
	}

	@Override
	protected Optional<String> resolveUrnPrefix(NetMobielModule module) {
    	return Optional.ofNullable(module == NetMobielModule.PLANNER ? PlannerUser.URN_PREFIX : null);
    }

}
