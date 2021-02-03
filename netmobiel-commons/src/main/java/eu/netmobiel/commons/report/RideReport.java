package eu.netmobiel.commons.report;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;

public class RideReport extends ReportKey {
	private static final long serialVersionUID = 5251079360179443539L;

	/**
	 * RSC-1: Departure postal code
	 */
	@CsvBindByName
	private String departurePostalCode;

	/**
	 * RSC-2: Arrival postal code.  
	 */
	@CsvBindByName
	private String arrivalPostalCode;
	
	/**
	 * RSC-3: The travelDate (derived from the departure time), in local date format
	 */
	@CsvBindByName
	@CsvDate("yyyy-MM-dd")
	private LocalDateTime travelDate;

	/**
	 * RSC-4: The duration of the ride in minutes.
	 */
	@CsvBindByName
	private int rideDuration;
	
	/**
	 * RSC-5: The number of passengers accompanying the driver.
	 */
	@CsvBindByName
	private int nrOfPassengers;
	
	/**
	 * RSC-6: Is the ride completed according to the driver.  
	 */
	@CsvBindByName
	private Boolean rideCompleted;
	
	/**
	 * RSC-7: Is the ride reviewed by at least one passenger?
	 */
	@CsvBindByName
	private Boolean reviewedByPassenger;
	
	/**
	 * RSC-8: Is the ride reviewed by the driver? 
	 */
	@CsvBindByName
	private Boolean reviewedByDriver;
	
	/**
	 * RSC-9: Is the ride a recurrent ride?  
	 */
	@CsvBindByName
	private boolean recurrentRide;

	
	public RideReport() {
		
	}
	
	public RideReport(ReportPeriodKey key) {
		super(key);
	}

	public RideReport(String managedIdentity) {
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

	public int getRideDuration() {
		return rideDuration;
	}

	public void setRideDuration(int rideDuration) {
		this.rideDuration = rideDuration;
	}

	public int getNrOfPassengers() {
		return nrOfPassengers;
	}

	public void setNrOfPassengers(int nrOfPassengers) {
		this.nrOfPassengers = nrOfPassengers;
	}

	public Boolean getRideCompleted() {
		return rideCompleted;
	}

	public void setRideCompleted(Boolean rideCompleted) {
		this.rideCompleted = rideCompleted;
	}

	public Boolean getReviewedByPassenger() {
		return reviewedByPassenger;
	}

	public void setReviewedByPassenger(Boolean reviewedByPassenger) {
		this.reviewedByPassenger = reviewedByPassenger;
	}

	public Boolean getReviewedByDriver() {
		return reviewedByDriver;
	}

	public void setReviewedByDriver(Boolean reviewedByDriver) {
		this.reviewedByDriver = reviewedByDriver;
	}

	public boolean isRecurrentRide() {
		return recurrentRide;
	}

	public void setRecurrentRide(boolean recurrentRide) {
		this.recurrentRide = recurrentRide;
	}

	@Override
	public int compareTo(ReportKey other) {
		int cmp = Objects.compare(getKey(), other.getKey(), Comparator.naturalOrder());
		if (cmp == 0) {
			cmp = getTravelDate().compareTo(((RideReport)other).getTravelDate());
		}
		return cmp;
	}


}


