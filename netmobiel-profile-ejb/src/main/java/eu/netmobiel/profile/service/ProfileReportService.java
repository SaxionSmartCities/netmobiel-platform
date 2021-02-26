package eu.netmobiel.profile.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.report.ProfileReport;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.repository.ProfileDao;

/**
 * Bean class for Profile Report session bean. 
 */
@Stateless
@Logging
public class ProfileReportService {

    @SuppressWarnings("unused")
	@Inject
    private Logger logger;

    @Inject
    private ProfileDao profileDao;
    
    public ProfileReportService() {
    }

    public Map<String, ProfileReport> reportUsers() throws BadRequestException {
    	Map<String, ProfileReport> reportMap = new HashMap<>();
		PagedResult<Long> prs = profileDao.findAll(0, 0);
        Long totalCount = prs.getTotalCount();
        final int batchSize = 100;
        for (int offset = 0; offset < totalCount; offset += batchSize) {
    		PagedResult<Long> userIds = profileDao.findAll(batchSize, offset);
    		List<Profile> users = profileDao.loadGraphs(userIds.getData(), null, Profile::getId);
    		for (Profile user : users) {
    			ProfileReport rr = new ProfileReport(user.getManagedIdentity());
    			reportMap.put(user.getManagedIdentity(), rr);
    			rr.setIsPassenger(user.isPassenger());
    			rr.setIsDriver(user.isDriver());
    			if (user.getDateOfBirth() != null) {
    				rr.setYearOfBirth(user.getDateOfBirth().getYear());
    			}
    			if (user.getHomeAddress() != null) {
    				rr.setHome(user.getHomeAddress().getLocality());
    			}
    		}
   		}
    	return reportMap;
    }
}
