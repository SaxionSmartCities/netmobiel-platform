package eu.netmobiel.planner.service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.report.ModalityNumericReportValue;
import eu.netmobiel.commons.report.NumericReportValue;
import eu.netmobiel.commons.report.PassengerBehaviourReport;
import eu.netmobiel.commons.report.PassengerModalityBehaviourReport;
import eu.netmobiel.commons.report.ProfileReport;
import eu.netmobiel.commons.report.TripReport;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.TriStateLogic;
import eu.netmobiel.planner.model.PlannerUser;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.repository.PlannerUserDao;
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

    @Inject
    private PlannerUserDao userDao;
	
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
    
    
    /**
     * Creates a report on all trips made in specified period.
     * @return A report with all trips in the specified period.
     * @param since	start period.
     * @param until end period exclusive.
     * @throws BadRequestException 
     */
    public List<TripReport> reportTrips(Instant since, Instant until) throws NotFoundException, BadRequestException {
    	List<TripReport> report = new ArrayList<>();
		PagedResult<Long> prs = tripDao.listTrips(since, until, 0, 0);
        Long totalCount = prs.getTotalCount();
        final int batchSize = 100;
        for (int offset = 0; offset < totalCount; offset += batchSize) {
    		PagedResult<Long> tripIds = tripDao.listTrips(since, until, batchSize, offset);
    		List<Trip> trips = tripDao.loadGraphs(tripIds.getData(), Trip.DETAILED_ENTITY_GRAPH, Trip::getId);
    		for (Trip trip : trips) {
    			TripReport rr = new TripReport(trip.getTraveller().getManagedIdentity());
    			report.add(rr);
    			// RSP-1
    			rr.setDeparturePostalCode(trip.getDeparturePostalCode());
    			// RSP-2
    			rr.setArrivalPostalCode(trip.getArrivalPostalCode());
    			// RSP-3
    			rr.setTravelDate(trip.getItinerary().getDepartureTime().atZone(ZoneId.of(TripPlanManager.DEFAULT_TIME_ZONE)).toLocalDateTime());
    			// RSP-4
    			if (trip.getItinerary().getDuration() != null) {
    				rr.setTripDuration(trip.getItinerary().getDuration() / 60);
    			}
    			// RSP-5
    			if (trip.getItinerary().getWalkTime() != null) {
    				rr.setWalkDuration(trip.getItinerary().getWalkTime() / 60);
    			}
    			// RSP-6
    			Boolean confirmed = trip.getItinerary().getLegs().stream()
    					.map(lg -> lg.getConfirmed())
    					.collect(Collectors.reducing(null, TriStateLogic::and));
    			rr.setTripConfirmed(confirmed);
    			// RSP-7
    			Boolean driverConfirmed = trip.getItinerary().getLegs().stream()
    					.map(lg -> lg.getConfirmedByProvider())
    					.collect(Collectors.reducing(null, TriStateLogic::and));
    			rr.setDriverHasConfirmed(driverConfirmed);
    			// RSP-8
    			boolean hasRideshareLeg = trip.getItinerary().getLegs().stream()
    					.filter(lg -> lg.getTraverseMode() == TraverseMode.RIDESHARE)
    					.findFirst()
    					.isPresent();
    			rr.setRideshareUsed(hasRideshareLeg);
    			// RSP-9
    			boolean hasPublicTransportLeg = trip.getItinerary().getLegs().stream()
    					.filter(lg -> lg.getTraverseMode().isTransit())
    					.findFirst()
    					.isPresent();
    			rr.setPublicTransportUsed(hasPublicTransportLeg);
    			// RSP-10
//    			rr.setReviewedByPassenger(reviewedByPassenger);
    			// RSP-11
//    			rr.setReviewByDriver(reviewedByDriver);
			}
    		
		}
        report.sort(Comparator.naturalOrder());
        return report;
    }

    public Map<String, ProfileReport> reportUsers() throws BadRequestException {
    	Map<String, ProfileReport> reportMap = new HashMap<>();
		PagedResult<Long> prs = userDao.findAll(0, 0);
        Long totalCount = prs.getTotalCount();
        final int batchSize = 100;
        for (int offset = 0; offset < totalCount; offset += batchSize) {
    		PagedResult<Long> userIds = userDao.findAll(batchSize, offset);
    		List<PlannerUser> users = userDao.loadGraphs(userIds.getData(), null, PlannerUser::getId);
    		for (PlannerUser user : users) {
    			ProfileReport rr = new ProfileReport(user.getManagedIdentity());
    			reportMap.put(user.getManagedIdentity(), rr);
    			// We don't know who is passenger and who is not, well, we could check who has trips scheduled
    			// Wait for the profile service, they know.
    			// rr.setIsPassenger(true);
    		}
   		}
    	return reportMap;
    }
}
