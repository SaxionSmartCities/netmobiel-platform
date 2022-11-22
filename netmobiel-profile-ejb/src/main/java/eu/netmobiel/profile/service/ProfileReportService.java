package eu.netmobiel.profile.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.report.ActivityReport;
import eu.netmobiel.commons.report.IncentiveModelDriverReport;
import eu.netmobiel.commons.report.IncentiveModelPassengerReport;
import eu.netmobiel.commons.report.NumericReportValue;
import eu.netmobiel.commons.report.ProfileReport;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.profile.model.UserEvent;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.model.Review;
import eu.netmobiel.profile.repository.UserEventDao;
import eu.netmobiel.profile.repository.ProfileDao;
import eu.netmobiel.profile.repository.ReviewDao;

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

    @Inject
    private ReviewDao reviewDao;

    @Inject
    private UserEventDao pageVisitDao;

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
    
    public Map<String, IncentiveModelPassengerReport> reportIncentiveModelPassager(Instant since, Instant until) throws BadRequestException {
    	Map<String, IncentiveModelPassengerReport> reportMap = new HashMap<>();
    	// IMP-9
    	for (NumericReportValue nrv : reviewDao.reportCount(Review.IMP_9_TRIPS_REVIEWED_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new IncentiveModelPassengerReport(nrv))
			.setTripsReviewedCount(nrv.getValue());
		}
    	return reportMap;
    }

    public Map<String, IncentiveModelDriverReport> reportIncentiveModelDriver(Instant since, Instant until) throws BadRequestException {
    	Map<String, IncentiveModelDriverReport> reportMap = new HashMap<>();
    	// IMP-9
    	for (NumericReportValue nrv : reviewDao.reportCount(Review.IMC_10_RIDES_REVIEWED_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new IncentiveModelDriverReport(nrv))
			.setRidesReviewedCount(nrv.getValue());
		}
    	return reportMap;
    }

    public Map<String, ActivityReport> reportUsageActivity(Instant since, Instant until) throws BadRequestException {
    	Map<String, ActivityReport> reportMap = new HashMap<>();
    	// ACT-5
    	for (NumericReportValue nrv : pageVisitDao.reportCount(UserEvent.ACT_5_USER_VISITS_DAYS_PER_MONTH_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new ActivityReport(nrv))
			.setUsageDaysPerMonthCount(nrv.getValue());
		}
    	// ACT-6
    	for (NumericReportValue nrv : pageVisitDao.reportCount(UserEvent.ACT_6_HOME_PAGE_UPDATES_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new ActivityReport(nrv))
			.setViewHomePageCTACount(nrv.getValue());
		}
    	// ACT-7
    	for (NumericReportValue nrv : pageVisitDao.reportCount(UserEvent.ACT_7_HOME_PAGE_UPDATES_CTA_PRESSED_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new ActivityReport(nrv))
			.setSelectHomePageCTACount(nrv.getValue());
		}
    	return reportMap;
    }
}
