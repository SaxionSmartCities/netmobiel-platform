package eu.netmobiel.overseer.service;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.util.Logging;

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

//	@Inject
//	private BankerUserManager bankerUserManager;
//
//	@Inject
//	private CommunicatorUserManager communicatorUserManager;
//
//	@Inject
//	private PlannerUserManager plannerUserManager;
//
//	@Inject
//	private RideshareUserManager rideshareUserManager;
//
//	@Inject
//	private ProfileMaintenance profileMaintenance;

    
	@PostConstruct
	public void initialize() {
		log.info("Starting up the Overseer, checking for maintenance tasks");
//    	Map<NetMobielModule, List<String>> moduleUsersMap = new HashMap<>();
//    	moduleUsersMap.put(NetMobielModule.BANKER, bankerUserManager.listManagedIdentities());
//    	List<String> commUsers = communicatorUserManager.listManagedIdentities();
//    	commUsers.remove(PublisherService.SYSTEM_USER.getManagedIdentity());
//    	moduleUsersMap.put(NetMobielModule.COMMUNICATOR, commUsers);
//    	moduleUsersMap.put(NetMobielModule.PLANNER, plannerUserManager.listManagedIdentities());
//    	moduleUsersMap.put(NetMobielModule.RIDESHARE, rideshareUserManager.listManagedIdentities());
//    	profileMaintenance.processReportOnNetMobielUsers(moduleUsersMap);
	}

}
