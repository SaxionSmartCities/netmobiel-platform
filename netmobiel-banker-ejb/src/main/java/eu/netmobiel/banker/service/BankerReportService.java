package eu.netmobiel.banker.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.banker.model.AccountingEntry;
import eu.netmobiel.banker.model.IncentiveModelDriverReport;
import eu.netmobiel.banker.model.IncentiveModelPassengerReport;
import eu.netmobiel.banker.repository.AccountingEntryDao;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.report.NumericReportValue;
import eu.netmobiel.commons.util.Logging;

@Stateless
@Logging
public class BankerReportService {

	@SuppressWarnings("unused")
	@Inject
	private Logger log;
	@Inject
	private AccountingEntryDao accountingEntryDao;
    

    public Map<String, IncentiveModelPassengerReport> reportIncentivesPassenger(Instant since, Instant until) throws BadRequestException {
    	Map<String, IncentiveModelPassengerReport> reportMap = new HashMap<>();
    	// The first could have been realized without lookup, but now it is all the same.
    	for (NumericReportValue nrv : accountingEntryDao.reportCount(AccountingEntry.IMP_1_EARNED_CREDITS, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new IncentiveModelPassengerReport(nrv))
    			.setEarnedCreditsTotal(nrv.getValue());
		}
    	// TODO AccountingEntry.IMP_2_EARNED_CREDITS_BY_APP_USAGE
    	for (NumericReportValue nrv : accountingEntryDao.reportCount(AccountingEntry.IMP_3_SPENT_CREDITS, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new IncentiveModelPassengerReport(nrv))
    			.setSpentCreditsTotal(nrv.getValue());
		}
    	for (NumericReportValue nrv : accountingEntryDao.reportCount(AccountingEntry.IMP_4_SPENT_CREDITS_TRAVELLING, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new IncentiveModelPassengerReport(nrv))
    			.setSpentCreditsForTravelling(nrv.getValue());
		}
    	for (NumericReportValue nrv : accountingEntryDao.reportCount(AccountingEntry.IMP_5_SPENT_CREDITS_CHARITIES, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new IncentiveModelPassengerReport(nrv))
			.setSpentCreditsForCharities(nrv.getValue());
		}
    	// TODO AccountingEntry.IMP_6_SPENT_CREDITS_REWARDS
    	for (NumericReportValue nrv : accountingEntryDao.reportCount(AccountingEntry.IMP_7_DEPOSITED_CREDITS, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new IncentiveModelPassengerReport(nrv))
			.setDepositedCredits(nrv.getValue());
		}
    	for (NumericReportValue nrv : accountingEntryDao.reportCount(AccountingEntry.IMP_8_WITHDRAWN_CREDITS, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new IncentiveModelPassengerReport(nrv))
			.setWithdrawnCredits(nrv.getValue());
		}
    	// TODO AccountingEntry.IMP_9_TRIPS_REVIEWED_COUNT
    	return reportMap;    	
    }


    public Map<String, IncentiveModelDriverReport> reportIncentivesDriver(Instant since, Instant until) throws BadRequestException {
    	Map<String, IncentiveModelDriverReport> reportMap = new HashMap<>();
    	// The first could have been realized without lookup, but now it is all the same.
    	for (NumericReportValue nrv : accountingEntryDao.reportCount(AccountingEntry.IMC_1_EARNED_CREDITS, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new IncentiveModelDriverReport(nrv))
    			.setEarnedCreditsTotal(nrv.getValue());
		}
    	for (NumericReportValue nrv : accountingEntryDao.reportCount(AccountingEntry.IMC_2_EARNED_CREDITS_BY_RIDES, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new IncentiveModelDriverReport(nrv))
    			.setEarnedCreditsRides(nrv.getValue());
		}
    	// TODO AccountingEntry.IMC_3_EARNED_CREDITS_BY_APP_USAGE
    	for (NumericReportValue nrv : accountingEntryDao.reportCount(AccountingEntry.IMC_4_SPENT_CREDITS, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new IncentiveModelDriverReport(nrv))
    			.setSpentCreditsTotal(nrv.getValue());
		}
    	for (NumericReportValue nrv : accountingEntryDao.reportCount(AccountingEntry.IMC_5_SPENT_CREDITS_TRAVELLING, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new IncentiveModelDriverReport(nrv))
    			.setSpentCreditsForTravelling(nrv.getValue());
		}
    	for (NumericReportValue nrv : accountingEntryDao.reportCount(AccountingEntry.IMC_6_SPENT_CREDITS_CHARITIES, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new IncentiveModelDriverReport(nrv))
			.setSpentCreditsForCharities(nrv.getValue());
		}
    	// TODO AccountingEntry.IMC_7_SPENT_CREDITS_REWARDS
    	for (NumericReportValue nrv : accountingEntryDao.reportCount(AccountingEntry.IMC_8_DEPOSITED_CREDITS, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new IncentiveModelDriverReport(nrv))
			.setDepositedCredits(nrv.getValue());
		}
    	for (NumericReportValue nrv : accountingEntryDao.reportCount(AccountingEntry.IMC_9_WITHDRAWN_CREDITS, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new IncentiveModelDriverReport(nrv))
			.setWithdrawnCredits(nrv.getValue());
		}
    	// TODO AccountingEntry.IMC_10_RIDES_REVIEWED_COUNT
    	return reportMap;    	
    }
}
