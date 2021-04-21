package eu.netmobiel.profile.service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.SessionContext;
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
public class ProfileMaintenance {

	@SuppressWarnings("unused")
	@Inject
    private Logger log;

    @Resource
    private SessionContext context;
    
	@PostConstruct
	public void initialize() {
	}

}
