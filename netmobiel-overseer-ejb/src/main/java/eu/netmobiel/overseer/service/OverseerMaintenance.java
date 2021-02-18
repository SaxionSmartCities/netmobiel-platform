package eu.netmobiel.overseer.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.banker.service.BankerUserManager;
import eu.netmobiel.commons.NetMobielModule;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.communicator.service.CommunicatorUserManager;
import eu.netmobiel.communicator.service.PublisherService;
import eu.netmobiel.planner.service.PlannerUserManager;
import eu.netmobiel.profile.service.ProfileMaintenance;
import eu.netmobiel.rideshare.service.RideshareUserManager;

/**
 * Singleton startup bean for doing some maintenance on startup of the system.
 * 1. Migrate profiles to this profile service and assure all components know about all users.
 *  
 * @author Jaap Reitsma
 *
 */
@Singleton
@Startup
@Logging
public class OverseerMaintenance {
	@Inject
    private Logger log;

	@Inject
	private BankerUserManager bankerUserManager;

	@Inject
	private CommunicatorUserManager communicatorUserManager;

	@Inject
	private PlannerUserManager plannerUserManager;

	@Inject
	private RideshareUserManager rideshareUserManager;

	@Inject
	private ProfileMaintenance profileMaintenance;

    
	@PostConstruct
	public void initialize() {
		log.info("Starting up the Overseer, doing some maintenance tasks");
    	Map<NetMobielModule, List<String>> moduleUsersMap = new HashMap<>();
    	moduleUsersMap.put(NetMobielModule.BANKER, bankerUserManager.listManagedIdentities());
    	List<String> commUsers = communicatorUserManager.listManagedIdentities();
    	commUsers.remove(PublisherService.SYSTEM_USER.getManagedIdentity());
    	moduleUsersMap.put(NetMobielModule.COMMUNICATOR, commUsers);
    	moduleUsersMap.put(NetMobielModule.PLANNER, plannerUserManager.listManagedIdentities());
    	moduleUsersMap.put(NetMobielModule.RIDESHARE, rideshareUserManager.listManagedIdentities());
    	profileMaintenance.processReportOnNetMobielUsers(moduleUsersMap);
	}

}
