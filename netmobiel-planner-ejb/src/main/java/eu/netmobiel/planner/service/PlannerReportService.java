package eu.netmobiel.planner.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.report.ModalityNumericReportValue;
import eu.netmobiel.commons.report.NumericReportValue;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.planner.model.PassengerBehaviourReport;
import eu.netmobiel.planner.model.PassengerModalityBehaviourReport;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.repository.TripDao;
import eu.netmobiel.planner.repository.TripPlanDao;

@Stateless
@Logging
public class PlannerReportService {
	
	@SuppressWarnings("unused")
	@Inject
    private Logger log;

    @Inject
    private TripDao tripDao;

    @Inject
    private TripPlanDao tripPlanDao;

	
    public Map<String, PassengerBehaviourReport> reportPassengerBehaviour(Instant since, Instant until) throws BadRequestException {
    	Map<String, PassengerBehaviourReport> reportMap = new HashMap<>();
    	// The first could have been realized without lookup, but now it is all the same.
    	// RGP-1
    	for (NumericReportValue nrv : tripDao.reportCount(Trip.RGP_1_TRIPS_CREATED_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new PassengerBehaviourReport(nrv))
			.setTripsCreatedCount(nrv.getValue());
		}
    	// RGP-2
    	for (NumericReportValue nrv : tripDao.reportCount(Trip.RGP_2_TRIPS_CANCELLED_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new PassengerBehaviourReport(nrv))
    			.setTripsCancelledCount(nrv.getValue());
		}
    	// RGP-3
    	for (NumericReportValue nrv : tripDao.reportCount(Trip.RGP_3_TRIPS_CANCELLED_BY_PASSENGER_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new PassengerBehaviourReport(nrv))
    			.setTripsCancelledByPassengerCount(nrv.getValue());
		}
    	// RGP-4
    	for (NumericReportValue nrv : tripDao.reportCount(Trip.RGP_4_TRIPS_CANCELLED_BY_PROVIDER_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new PassengerBehaviourReport(nrv))
    			.setTripsCancelledByProviderCount(nrv.getValue());
		}
    	// RGP-5
    	for (NumericReportValue nrv : tripDao.reportCount(Trip.RGP_5_TRIPS_WITH_CONFIRMED_RIDESHARE_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new PassengerBehaviourReport(nrv))
    			.setTripsWithConfirmedRideshareCount(nrv.getValue());
		}
    	// RGP-6
    	for (NumericReportValue nrv : tripDao.reportCount(Trip.RGP_6_TRIPS_WITH_CANCELLED_RIDESHARE_PAYMENT_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new PassengerBehaviourReport(nrv))
    			.setTripsWithCancelledRidesharePaymentCount(nrv.getValue());
		}
    	// RGP-7
    	for (NumericReportValue nrv : tripDao.reportCount(Trip.RGP_7_MONO_MODAL_TRIPS_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new PassengerBehaviourReport(nrv))
    			.setTripsCancelledByProviderCount(nrv.getValue());
		}
    	// RGP-9
    	for (NumericReportValue nrv : tripDao.reportCount(Trip.RGP_9_MULTI_MODAL_TRIPS_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new PassengerBehaviourReport(nrv))
    			.setTripsMultiModalCount(nrv.getValue());
		}

    	// RGP-11
    	for (NumericReportValue nrv : tripPlanDao.reportCount(TripPlan.RGP_11_TRIP_PLAN_SHOUT_OUT_ISSUED_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new PassengerBehaviourReport(nrv))
    			.setTripPlanShoutOutIssuedCount(nrv.getValue());
		}
    	// RGP-12
    	for (NumericReportValue nrv : tripPlanDao.reportCount(TripPlan.RGP_12_TRIP_PLAN_SHOUT_OUT_AT_LEAST_ONE_OFFER_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new PassengerBehaviourReport(nrv))
    			.setTripPlanShoutOutAtLeastOneOfferCount(nrv.getValue());
		}
    	// RGP-13
    	for (NumericReportValue nrv : tripPlanDao.reportCount(TripPlan.RGP_13_TRIP_PLAN__SHOUT_OUT_ACCEPTED_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new PassengerBehaviourReport(nrv))
    			.setTripPlanShoutOutAcceptedCount(nrv.getValue());
		}
    	return reportMap;
    	
    }

    public Map<String, PassengerModalityBehaviourReport> reportPassengerModalityBehaviour(Instant since, Instant until) throws BadRequestException {
    	Map<String, PassengerModalityBehaviourReport> reportMap = new HashMap<>();
    	// The first could have been realized without lookup, but now it is all the same.
    	// RGP-8
    	for (ModalityNumericReportValue nrv : tripDao.reportModalityCount(Trip.RGP_8_MONO_MODAL_TRIPS_BY_MODALITY_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new PassengerModalityBehaviourReport(nrv))
    			.setTripsMonoModalCount(nrv.getValue());
		}
    	// RGP-10
    	for (ModalityNumericReportValue nrv : tripDao.reportModalityCount(Trip.RGP_10_MULTI_MODAL_TRIPS_BY_MODALITY_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new PassengerModalityBehaviourReport(nrv))
    			.setTripsMultiModalCount(nrv.getValue());
		}
    	return reportMap;
    	
    }
}
