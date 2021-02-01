package eu.netmobiel.rideshare.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.report.NumericReportValue;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideshareReport;
import eu.netmobiel.rideshare.repository.BookingDao;
import eu.netmobiel.rideshare.repository.RideDao;

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
    

    public Map<String, RideshareReport> reportActivity(Instant since, Instant until) throws BadRequestException {
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
}
