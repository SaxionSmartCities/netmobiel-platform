package eu.netmobiel.planner.service;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * Singleton startup bean for doing some maintenance on startup of the system
 * @author Jaap Reitsma
 *
 */
@Singleton
@Startup
public class PlannerMaintenance {

	@Inject
    private Logger log;
	@Inject
	private TripManager tripManager;
	
	@PostConstruct
	public void initialize() {
		log.info("Starting up the Planner, doing some maintenance tasks");
		tripManager.reviveTripMonitors();
	}
}
