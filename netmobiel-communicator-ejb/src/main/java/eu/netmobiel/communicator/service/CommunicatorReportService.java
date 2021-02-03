package eu.netmobiel.communicator.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.report.ActivityReport;
import eu.netmobiel.commons.report.NumericReportValue;
import eu.netmobiel.commons.report.ShoutOutRecipientReport;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.communicator.model.Envelope;
import eu.netmobiel.communicator.repository.EnvelopeDao;

/**
 * Bean class for Publisher enterprise bean. 
 */
@Stateless
@Logging
public class CommunicatorReportService {

    @SuppressWarnings("unused")
	@Inject
    private Logger logger;

    @Inject
    private EnvelopeDao envelopeDao;
    
    public CommunicatorReportService() {
    }

    /**
	 * Report on indicators of the communicator service, group by identity, year, month.
	 * Indicators are: # received messages, # read messages, # received notifications, # read notifications.
 	 * @param since start of period (inclusive). Use midnight of first day of first month to report on.
	 * @param until end of period (exclusive). Use midnight of first day past last month to report on.
	 * @return A sorted list (ascending on identity, year, month) of activity reports
	 * @throws BadRequestException
	 */
    public Map<String, ActivityReport> reportActivity(Instant since, Instant until) throws BadRequestException {
    	Map<String, ActivityReport> reportMap = new HashMap<>();
    	// The first could have been realized without lookup, but now it is all the same.
    	for (NumericReportValue nrv : envelopeDao.reportCount(Envelope.ACT_1_MESSAGES_RECEIVED_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new ActivityReport(nrv))
			.setMessageCount(nrv.getValue());
		}
    	for (NumericReportValue nrv : envelopeDao.reportCount(Envelope.ACT_2_NOTIFICATIONS_RECEIVED_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new ActivityReport(nrv))
    			.setNotificationCount(nrv.getValue());
		}
    	for (NumericReportValue nrv : envelopeDao.reportCount(Envelope.ACT_3_MESSAGES_READ_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new ActivityReport(nrv))
    			.setMessageAckedCount(nrv.getValue());
		}
    	for (NumericReportValue nrv : envelopeDao.reportCount(Envelope.ACT_4_NOTIFICATIONS_READ_COUNT,since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new ActivityReport(nrv))
    			.setNotificationAckedCount(nrv.getValue());
		}
    	return reportMap;
    }

    /**
	 * Report on indicators of the communicator service regarding shout-outs, group by identity, year, month.
	 * Indicators are: # received shout-outs, # read shout-outs.
 	 * @param since start of period (inclusive). Use midnight of first day of first month to report on.
	 * @param until end of period (exclusive). Use midnight of first day past last month to report on.
	 * @return A sorted list (ascending on identity, year, month) of shout-out reports
	 * @throws BadRequestException
	 */
    public List<ShoutOutRecipientReport> reportShoutOutActivity(Instant since, Instant until) throws BadRequestException {
    	Map<String, ShoutOutRecipientReport> reportMap = new HashMap<>();
    	// The first could have been realized without lookup, but now it is all the same.
    	for (NumericReportValue nrv : envelopeDao.reportCount(Envelope.RGC_5_SHOUT_OUT_NOTIFICATIONS_RECEIVED_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new ShoutOutRecipientReport(nrv))
			.setShoutOutNotificationCount(nrv.getValue());
		}
    	for (NumericReportValue nrv : envelopeDao.reportCount(Envelope.RGC_6_SHOUT_OUT_NOTIFICATIONS_READ_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new ShoutOutRecipientReport(nrv))
    			.setShoutOutNotificationAckedCount(nrv.getValue());
		}
    	return reportMap.values().stream()
    			.sorted()
    			.collect(Collectors.toList());
    }
}
