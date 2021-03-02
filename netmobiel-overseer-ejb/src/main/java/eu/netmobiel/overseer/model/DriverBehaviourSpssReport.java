package eu.netmobiel.overseer.model;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import com.opencsv.bean.CsvBindAndJoinByName;

import eu.netmobiel.commons.report.DriverBehaviourReport;
import eu.netmobiel.commons.report.SpssReportBase;

public class DriverBehaviourSpssReport  extends SpssReportBase<DriverBehaviourReport> {
	/**
	 * RGC-1: The number of offered rides.
	 */
	@CsvBindAndJoinByName(column = "ridesOfferedCount_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> ridesOfferedCount = new ArrayListValuedHashMap<>();

	/**
	 * RGC-2: The number of bookings cancelled by the passenger.  
	 */
	@CsvBindAndJoinByName(column = "bookingsCancelledByPassengerCount_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> bookingsCancelledByPassengerCount = new ArrayListValuedHashMap<>();
	
	/**
	 * RGC-3: The number of bookings cancelled by the driver.
	 */
	@CsvBindAndJoinByName(column = "bookingsCancelledByDriverCount_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> bookingsCancelledByDriverCount = new ArrayListValuedHashMap<>();

	/**
	 * RGC-4: The number of confirmed bookings.
	 */
	@CsvBindAndJoinByName(column = "bookingsConfirmedCount_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> bookingsConfirmedCount = new ArrayListValuedHashMap<>();
	
	/**
	 * RGC-5: The number of shout-out notifications received.
	 */
	@CsvBindAndJoinByName(column = "shoutOutNotificationCount_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> shoutOutNotificationCount = new ArrayListValuedHashMap<>();
	
	/**
	 * RGC-6: The number of shout-out notifications acknowledged (i.e. read)  
	 */
	@CsvBindAndJoinByName(column = "shoutOutNotificationAckedCount_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> shoutOutNotificationAckedCount = new ArrayListValuedHashMap<>();
	
	/**
	 * RGC-7: The number of shout-outs proposed to.
	 */
	@CsvBindAndJoinByName(column = "ridesProposedCount_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> ridesProposedCount = new ArrayListValuedHashMap<>();
	
	/**
	 * RGC-8: The number of shout-outs actually accepted (with a confirmed booking)
	 */
	@CsvBindAndJoinByName(column = "ridesProposedAndAcceptedCount_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> ridesProposedAndAcceptedCount = new ArrayListValuedHashMap<>();
	

	public DriverBehaviourSpssReport(String managedIdentity, String home) {
		super(managedIdentity, home);
	}

	@Override
	public void addReportValues(DriverBehaviourReport r) {
		// 1 - 4
		ridesOfferedCount.put(String.format("ridesOfferedCount_%d_%02d", r.getYear(), r.getMonth()), r.getRidesOfferedCount());
		bookingsCancelledByPassengerCount.put(String.format("bookingsCancelledByPassengerCount_%d_%02d", r.getYear(), r.getMonth()), r.getBookingsCancelledByPassengerCount());
		bookingsCancelledByDriverCount.put(String.format("bookingsCancelledByDriverCount_%d_%02d", r.getYear(), r.getMonth()), r.getBookingsCancelledByDriverCount());
		bookingsConfirmedCount.put(String.format("bookingsConfirmedCount_%d_%02d", r.getYear(), r.getMonth()), r.getBookingsConfirmedCount());
		
		// 5 - 8
		shoutOutNotificationCount.put(String.format("shoutOutNotificationCount_%d_%02d", r.getYear(), r.getMonth()), r.getShoutOutNotificationCount());
		shoutOutNotificationAckedCount.put(String.format("shoutOutNotificationAckedCount_%d_%02d", r.getYear(), r.getMonth()), r.getShoutOutNotificationAckedCount());
		ridesProposedCount.put(String.format("ridesProposedCount_%d_%02d", r.getYear(), r.getMonth()), r.getRidesProposedCount());
		ridesProposedAndAcceptedCount.put(String.format("ridesProposedAndAcceptedCount_%d_%02d", r.getYear(), r.getMonth()), r.getRidesProposedAndAcceptedCount());
	}

	public MultiValuedMap<String, Integer> getRidesOfferedCount() {
		return ridesOfferedCount;
	}

	public void setRidesOfferedCount(MultiValuedMap<String, Integer> ridesOfferedCount) {
		this.ridesOfferedCount = ridesOfferedCount;
	}

	public MultiValuedMap<String, Integer> getBookingsCancelledByPassengerCount() {
		return bookingsCancelledByPassengerCount;
	}

	public void setBookingsCancelledByPassengerCount(MultiValuedMap<String, Integer> bookingsCancelledByPassengerCount) {
		this.bookingsCancelledByPassengerCount = bookingsCancelledByPassengerCount;
	}

	public MultiValuedMap<String, Integer> getBookingsCancelledByDriverCount() {
		return bookingsCancelledByDriverCount;
	}

	public void setBookingsCancelledByDriverCount(MultiValuedMap<String, Integer> bookingsCancelledByDriverCount) {
		this.bookingsCancelledByDriverCount = bookingsCancelledByDriverCount;
	}

	public MultiValuedMap<String, Integer> getBookingsConfirmedCount() {
		return bookingsConfirmedCount;
	}

	public void setBookingsConfirmedCount(MultiValuedMap<String, Integer> bookingsConfirmedCount) {
		this.bookingsConfirmedCount = bookingsConfirmedCount;
	}

	public MultiValuedMap<String, Integer> getShoutOutNotificationCount() {
		return shoutOutNotificationCount;
	}

	public void setShoutOutNotificationCount(MultiValuedMap<String, Integer> shoutOutNotificationCount) {
		this.shoutOutNotificationCount = shoutOutNotificationCount;
	}

	public MultiValuedMap<String, Integer> getShoutOutNotificationAckedCount() {
		return shoutOutNotificationAckedCount;
	}

	public void setShoutOutNotificationAckedCount(MultiValuedMap<String, Integer> shoutOutNotificationAckedCount) {
		this.shoutOutNotificationAckedCount = shoutOutNotificationAckedCount;
	}

	public MultiValuedMap<String, Integer> getRidesProposedCount() {
		return ridesProposedCount;
	}

	public void setRidesProposedCount(MultiValuedMap<String, Integer> ridesProposedCount) {
		this.ridesProposedCount = ridesProposedCount;
	}

	public MultiValuedMap<String, Integer> getRidesProposedAndAcceptedCount() {
		return ridesProposedAndAcceptedCount;
	}

	public void setRidesProposedAndAcceptedCount(MultiValuedMap<String, Integer> ridesProposedAndAcceptedCount) {
		this.ridesProposedAndAcceptedCount = ridesProposedAndAcceptedCount;
	}

}


