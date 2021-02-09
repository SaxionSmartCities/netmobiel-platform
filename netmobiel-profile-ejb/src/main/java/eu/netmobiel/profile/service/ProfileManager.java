package eu.netmobiel.profile.service;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.profile.repository.ProfileDao;

/**
 * Bean class for Publisher enterprise bean. 
 */
@Stateless
@Logging
public class ProfileManager {
	public static final Integer MAX_RESULTS = 10; 

	@Resource
    private SessionContext sessionContext;

    @Inject
    private Logger logger;

    @Inject
    private ProfileDao profileDao;
    
    public ProfileManager() {
    }


}
