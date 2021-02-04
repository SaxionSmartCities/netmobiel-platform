package eu.netmobiel.commons.report;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;
/**
 * Report on the trip of a traveller.
 * 
 * @author Jaap Reitsma
 *
 */
public class TripReport extends ReportKey {
	private static final long serialVersionUID = 5251079360179443539L;

	/**
	 * RSP-1: Departure postal code
	 */
	@CsvBindByName
	private String departurePostalCode;

	/**
	 * RSP-2: Arrival postal code.  
	 */
	@CsvBindByName
	private String arrivalPostalCode;
	
	/**
	 * RSP-3: The travelDate (derived from the departure time), in local date format
	 */
	@CsvBindByName
	@CsvDate("yyyy-MM-dd")
	private LocalDateTime travelDate;

	/**
	 * RSP-4: The duration of the trip in minutes.
	 */
	@CsvBindByName
	private int tripDuration;
	
	/**
	 * RSP-5: The total time needed for walking.
	 */
	@CsvBindByName
	private int walkDuration;
	
	/**
	 * RSP-6: Is the trip completed according to the passenger/traveller.  
	 */
	@CsvBindByName
	private Boolean tripConfirmed;
	
	/**
	 * RSP-7: Is the trip confirmed by the driver (in case of rideshare)?  
	 */
	@CsvBindByName
	private Boolean driverHasConfirmed;

	/**
	 * RSP-8: Is rideshare used?
	 */
	@CsvBindByName
	private Boolean rideshareUsed;
	
	/**
	 * RSP-9: Is public transport used?
	 */
	@CsvBindByName
	private Boolean publicTransportUsed;
	
	/**
	 * RSP-10: Has the passenger given a review?
	 */
	@CsvBindByName
	private Boolean reviewByPassenger;
	
	/**
	 * RSP-11: Has the driver given a review?
	 */
	@CsvBindByName
	private Boolean reviewByDriver;
	
	public TripReport() {
		
	}
	
	public TripReport(ReportPeriodKey key) {
		super(key);
	}

	public TripReport(String managedIdentity) {
		super(managedIdentity);
	}

	public String getDeparturePostalCode() {
		return departurePostalCode;
	}

	public void setDeparturePostalCode(String departurePostalCode) {
		this.departurePostalCode = departurePostalCode;
	}

	public String getArrivalPostalCode() {
		return arrivalPostalCode;
	}

	public void setArrivalPostalCode(String arrivalPostalCode) {
		this.arrivalPostalCode = arrivalPostalCode;
	}

	public LocalDateTime getTravelDate() {
		return travelDate;
	}

	public void setTravelDate(LocalDateTime travelDate) {
		this.travelDate = travelDate;
	}

	public int getTripDuration() {
		return tripDuration;
	}

	public void setTripDuration(int tripDuration) {
		this.tripDuration = tripDuration;
	}

	public int getWalkDuration() {
		return walkDuration;
	}

	public void setWalkDuration(int walkDuration) {
		this.walkDuration = walkDuration;
	}

	public Boolean getTripConfirmed() {
		return tripConfirmed;
	}

	public void setTripConfirmed(Boolean tripConfirmed) {
		this.tripConfirmed = tripConfirmed;
	}

	public Boolean getDriverHasConfirmed() {
		return driverHasConfirmed;
	}

	public void setDriverHasConfirmed(Boolean driverHasConfirmed) {
		this.driverHasConfirmed = driverHasConfirmed;
	}

	public Boolean getRideshareUsed() {
		return rideshareUsed;
	}

	public void setRideshareUsed(Boolean rideshareUsed) {
		this.rideshareUsed = rideshareUsed;
	}

	public Boolean getPublicTransportUsed() {
		return publicTransportUsed;
	}

	public void setPublicTransportUsed(Boolean publicTransportUsed) {
		this.publicTransportUsed = publicTransportUsed;
	}

	public Boolean getReviewByPassenger() {
		return reviewByPassenger;
	}

	public void setReviewByPassenger(Boolean reviewByPassenger) {
		this.reviewByPassenger = reviewByPassenger;
	}

	public Boolean getReviewByDriver() {
		return reviewByDriver;
	}

	public void setReviewByDriver(Boolean reviewByDriver) {
		this.reviewByDriver = reviewByDriver;
	}

	@Override
	public int compareTo(ReportKey other) {
		int cmp = Objects.compare(getKey(), other.getKey(), Comparator.naturalOrder());
		if (cmp == 0) {
			cmp = getTravelDate().compareTo(((TripReport)other).getTravelDate());
		}
		return cmp;
	}


}


