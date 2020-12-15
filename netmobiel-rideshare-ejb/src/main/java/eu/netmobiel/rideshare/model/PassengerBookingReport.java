package eu.netmobiel.rideshare.model;

import com.opencsv.bean.CsvBindByName;

import eu.netmobiel.commons.report.ReportKey;

public class PassengerBookingReport extends ReportKey {
	
	private static final long serialVersionUID = -7202730003166618230L;

	/**
	 * The number of messages received.
	 */
	@CsvBindByName
	private int bookingsConfirmedCount;
	
	/**
	 * The number of messages acknowledged (i.e. read)  
	 */
	@CsvBindByName
	private int bookingsCancelledByPassengerCount;
	
	/**
	 * The number of notifications received.
	 */
	@CsvBindByName
	private int bookingsCancelledByDriverCount;
	
	public PassengerBookingReport() {
		
	}
	
	public PassengerBookingReport(ReportKey key) {
		super(key);
	}

	public PassengerBookingReport(String managedIdentity, int year, int month) {
		super(managedIdentity, year, month);
	}

	public int getBookingsConfirmedCount() {
		return bookingsConfirmedCount;
	}

	public void setBookingsConfirmedCount(int bookingsConfirmedCount) {
		this.bookingsConfirmedCount = bookingsConfirmedCount;
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

	@Override
	public String toString() {
		return String.format(
				"%s bcfm %d bcncp %d bcncd %d",
				getKey(), bookingsConfirmedCount, bookingsCancelledByPassengerCount, bookingsCancelledByDriverCount);
	}

	
}


