package eu.netmobiel.rideshare.service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.report.NumericReportValue;
import eu.netmobiel.commons.report.ProfileReport;
import eu.netmobiel.commons.report.RideReport;
import eu.netmobiel.commons.report.RideshareReport;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.Recurrence;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideshareUser;
import eu.netmobiel.rideshare.repository.BookingDao;
import eu.netmobiel.rideshare.repository.RideDao;
import eu.netmobiel.rideshare.repository.RideshareUserDao;

@Stateless
@Logging
public class RideshareReportService {

	@SuppressWarnings("unused")
	@Inject
	private Logger log;
	@Inject
	private RideDao rideDao;
	@Inject
	private BookingDao bookingDao;
	@Inject
	private RideshareUserDao userDao;
    

    public Map<String, RideshareReport> reportDriverActivity(Instant since, Instant until) throws BadRequestException {
    	Map<String, RideshareReport> reportMap = new HashMap<>();
    	// The first could have been realized without lookup, but now it is all the same.
    	for (NumericReportValue nrv : rideDao.reportCount(Ride.RGC_1_OFFERED_RIDES_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new RideshareReport(nrv))
    			.setRidesOfferedCount(nrv.getValue());
		}
    	for (NumericReportValue nrv : bookingDao.reportCount(Booking.RGC_2_BOOKINGS_CANCELLED_BY_PASSENGER_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new RideshareReport(nrv))
    			.setBookingsCancelledByPassengerCount(nrv.getValue());
		}
    	for (NumericReportValue nrv : bookingDao.reportCount(Booking.RGC_3_BOOKINGS_CANCELLED_BY_DRIVER_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new RideshareReport(nrv))
    			.setBookingsCancelledByDriverCount(nrv.getValue());
		}
    	for (NumericReportValue nrv : bookingDao.reportCount(Booking.RGC_4_BOOKINGS_CONFIRMED_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new RideshareReport(nrv))
			.setBookingsConfirmedCount(nrv.getValue());
		}
    	// RGC_5 and RGC-6 are queries from the Communicator.
    	for (NumericReportValue nrv : bookingDao.reportCount(Booking.RGC_7_RIDES_PROPOSED_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new RideshareReport(nrv))
			.setRidesProposedCount(nrv.getValue());
		}
    	for (NumericReportValue nrv : bookingDao.reportCount(Booking.RGC_8_RIDES_PROPOSED_AND_ACCEPTED_COUNT, since, until)) {
    		reportMap.computeIfAbsent(nrv.getKey(), k -> new RideshareReport(nrv))
			.setRidesProposedAndAcceptedCount(nrv.getValue());
		}
    	return reportMap;    	
    }
    
    /**
     * Creates a report on all rides ridden in specified period.
     * @return A report with all rides in the specified period.
     * @param since	start period.
     * @param until end period exclusive.
     * @throws BadRequestException 
     */
    public List<RideReport> reportRides(Instant since, Instant until) throws NotFoundException, BadRequestException {
    	List<RideReport> report = new ArrayList<>();
		PagedResult<Long> prs = rideDao.listRides(since, until, 0, 0);
        Long totalCount = prs.getTotalCount();
        final int batchSize = 100;
        for (int offset = 0; offset < totalCount; offset += batchSize) {
    		PagedResult<Long> rideIds = rideDao.listRides(since, until, batchSize, offset);
    		List<Ride> rides = rideDao.loadGraphs(rideIds.getData(), Ride.LIST_RIDES_ENTITY_GRAPH, Ride::getId);
    		for (Ride ride : rides) {
    			RideReport rr = new RideReport(ride.getDriver().getManagedIdentity());
    			report.add(rr);
    			// RSC-1
    			rr.setDeparturePostalCode(ride.getDeparturePostalCode());
    			// RSC-2
    			rr.setArrivalPostalCode(ride.getArrivalPostalCode());
    			// RSC-3
    			rr.setTravelDate(ride.getDepartureTime().atZone(ZoneId.of(Recurrence.DEFAULT_TIME_ZONE)).toLocalDateTime());
    			// RSC-4
    			rr.setRideDuration(ride.getDuration() / 60);
    			// RSC-5
    			if (ride.hasActiveBooking()) {
        			rr.setNrOfPassengers(ride.getActiveBooking().get().getNrSeats());
    			}
    			// RSC-6
    			rr.setRideConfirmedByDriver(ride.getConfirmed());
    			// RSC-7
//    			rr.setReviewedByDriver(reviewedByDriver);
    			// RSC-8
//    			rr.setReviewedByPassenger(reviewedByPassenger);
    			// RSC-9
    			rr.setRecurrentRide(ride.getRideTemplate() != null);
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
    		List<RideshareUser> users = userDao.loadGraphs(userIds.getData(), RideshareUser.LIST_USERS_WITH_CARS_ENTITY_GRAPH, RideshareUser::getId);
    		for (RideshareUser user : users) {
    			ProfileReport rr = new ProfileReport(user.getManagedIdentity());
    			reportMap.put(user.getManagedIdentity(), rr);
    			rr.setNrActiveCars(user.getCarsInUse() != null ? user.getCarsInUse().size() : 0);
    			if (user.getYearOfBirth() != null) {
    				rr.setYearOfBirth(Integer.parseInt(user.getYearOfBirth()));
    			}
    			if (rr.getNrActiveCars() > 0) {
    				rr.setIsDriver(true);
    			}
    		}
   		}
    	return reportMap;
    }
}
