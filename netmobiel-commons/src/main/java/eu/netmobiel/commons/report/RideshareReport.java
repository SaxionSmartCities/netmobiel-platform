package eu.netmobiel.commons.report;

import com.opencsv.bean.CsvBindByName;

public class RideshareReport extends ReportPeriodKey {
	
	private static final long serialVersionUID = -7202730003166618230L;

	/**
	 * RGC-1: The number of offered rides.
	 */
	@CsvBindByName
	private int ridesOfferedCount;

	/**
	 * RGC-2: The number of bookings cancelled by the passenger.  
	 */
	@CsvBindByName
	private int bookingsCancelledByPassengerCount;
	
	/**
	 * RGC-3: The number of bookings cancelled by the driver.
	 */
	@CsvBindByName
	private int bookingsCancelledByDriverCount;

	/**
	 * RGC-4: The number of confirmed bookings.
	 */
	@CsvBindByName
	private int bookingsConfirmedCount;
	
	/**
	 * RGC-5: The number of shout-out notifications received.
	 */
	@CsvBindByName
	private int shoutOutNotificationCount;
	
	/**
	 * RGC-6: The number of shout-out notifications acknowledged (i.e. read)  
	 */
	@CsvBindByName
	private int shoutOutNotificationAckedCount;
	
	/**
	 * RGC-7: The number of shout-outs proposed to.
	 */
	@CsvBindByName
	private int ridesProposedCount;
	
	/**
	 * RGC-8: The number of shout-outs actually accepted (with a confirmed booking)
	 */
	@CsvBindByName
	private int ridesProposedAndAcceptedCount;
	

	
	public RideshareReport() {
		
	}
	
	public RideshareReport(ReportPeriodKey key) {
		super(key);
	}

	public RideshareReport(String managedIdentity, int year, int month) {
		super(managedIdentity, year, month);
	}


	public int getRidesOfferedCount() {
		return ridesOfferedCount;
	}

	public void setRidesOfferedCount(int ridesOfferedCount) {
		this.ridesOfferedCount = ridesOfferedCount;
	}

	public int getBookingsCancelledByPassengerCount() {
		return bookingsCancelledByPassengerCount;
	}

	public void setBookingsCancelledByPassengerCount(int bookingsCancelledByPassengerCount) {
		this.bookingsCancelledByPassengerCount = bookingsCancelledByPassengerCount;
	}

	public int getBookingsCancelledByDriverCount() {
		return bookingsCancelledByDriverCount;
	}

	public void setBookingsCancelledByDriverCount(int bookingsCancelledByDriverCount) {
		this.bookingsCancelledByDriverCount = bookingsCancelledByDriverCount;
	}

	public int getBookingsConfirmedCount() {
		return bookingsConfirmedCount;
	}

	public void setBookingsConfirmedCount(int bookingsConfirmedCount) {
		this.bookingsConfirmedCount = bookingsConfirmedCount;
	}

	public int getShoutOutNotificationCount() {
		return shoutOutNotificationCount;
	}

	public void setShoutOutNotificationCount(int shoutOutNotificationCount) {
		this.shoutOutNotificationCount = shoutOutNotificationCount;
	}

	public int getShoutOutNotificationAckedCount() {
		return shoutOutNotificationAckedCount;
	}

	public void setShoutOutNotificationAckedCount(int shoutOutNotificationAckedCount) {
		this.shoutOutNotificationAckedCount = shoutOutNotificationAckedCount;
	}

	public int getRidesProposedCount() {
		return ridesProposedCount;
	}

	public void setRidesProposedCount(int ridesProposedCount) {
		this.ridesProposedCount = ridesProposedCount;
	}

	public int getRidesProposedAndAcceptedCount() {
		return ridesProposedAndAcceptedCount;
	}

	public void setRidesProposedAndAcceptedCount(int ridesProposedAndAcceptedCount) {
		this.ridesProposedAndAcceptedCount = ridesProposedAndAcceptedCount;
	}

	@Override
	public String toString() {
		return String.format(
				"%s bcfm %d bcncp %d bcncd %d",
				getKey(), bookingsConfirmedCount, bookingsCancelledByPassengerCount, bookingsCancelledByDriverCount);
	}

	
}


