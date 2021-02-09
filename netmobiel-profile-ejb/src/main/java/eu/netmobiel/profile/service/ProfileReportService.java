package eu.netmobiel.profile.service;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.util.Logging;

/**
 * Bean class for Profile Report session bean. 
 */
@Stateless
@Logging
public class ProfileReportService {

    @SuppressWarnings("unused")
	@Inject
    private Logger logger;

    public ProfileReportService() {
    }

}
