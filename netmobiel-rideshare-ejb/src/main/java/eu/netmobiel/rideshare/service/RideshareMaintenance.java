package eu.netmobiel.rideshare.service;

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
public class RideshareMaintenance {

	@Inject
    private Logger log;
	@Inject
	private RideManager rideManager;
	
	@PostConstruct
	public void initialize() {
		log.info("Starting up the Rideshare, doing some maintenance tasks");
		rideManager.reviveRideMonitors();
	}
}
