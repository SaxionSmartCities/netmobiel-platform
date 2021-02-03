package eu.netmobiel.planner.model;

import com.opencsv.bean.CsvBindByName;

import eu.netmobiel.commons.report.ReportKey;

public class PassengerBehaviourReport extends ReportKey {
	
	private static final long serialVersionUID = -7202730003166618230L;

	/**
	 * RGP-1 The number of trips created.
	 */
	@CsvBindByName
	private int tripsCreatedCount;
	
	/**
	 * RGP-2 The number of trips cancelled by passenger or by mobility provider.
	 */
	@CsvBindByName
	private int tripsCancelledCount;

	/**
	 * RGP-3 The number of trips cancelled by the passenger.  
	 */
	@CsvBindByName
	private int tripsCancelledByPassengerCount;
	
	/**
	 * RGP-4 The number of trips cancelled by the mobility provider.  
	 */
	@CsvBindByName
	private int tripsCancelledByProviderCount;

	/**
	 * RGP-5 Number of trips with a confirmed rideshare leg 
	 */
	@CsvBindByName
	private int tripsWithConfirmedRideshareCount;

	/**
	 * RGP-6 Number of trips with a cancelled rideshare leg
	 */
	@CsvBindByName
	private int tripsWithCancelledRidesharePaymentCount;

	/**
	 * RGP-7 Number of completed monomodal trips (ignoring Walking)
	 */
	@CsvBindByName
	private int tripsMonoModalCount;

	/**
	 * RGP-9 Number of completed multimodal trips (ignoring Walking)
	 */
	@CsvBindByName
	private int tripsMultiModalCount;

	/**
	 * RGP-11 Count the number of shout-outs issued in a period
	 */
	@CsvBindByName
	private int tripPlanShoutOutIssuedCount;

	/**
	 * RGP-12 Count the number of shout-outs with at least one offer in a period
	 */
	@CsvBindByName
	private int tripPlanShoutOutAtLeastOneOfferCount;

	/**
	 * RGP-13 Count the number of accepted shout-outs issued in a period
	 */
	@CsvBindByName
	private int tripPlanShoutOutAcceptedCount;

	public PassengerBehaviourReport() {
		
	}
	
	public PassengerBehaviourReport(ReportKey key) {
		super(key);
	}

	public PassengerBehaviourReport(String managedIdentity, int year, int month) {
		super(managedIdentity, year, month);
	}

	public int getTripsCreatedCount() {
		return tripsCreatedCount;
	}

	public void setTripsCreatedCount(int tripsCreatedCount) {
		this.tripsCreatedCount = tripsCreatedCount;
	}

	public int getTripsCancelledCount() {
		return tripsCancelledCount;
	}

	public void setTripsCancelledCount(int tripsCancelledCount) {
		this.tripsCancelledCount = tripsCancelledCount;
	}

	public int getTripsCancelledByPassengerCount() {
		return tripsCancelledByPassengerCount;
	}

	public void setTripsCancelledByPassengerCount(int tripsCancelledByPassengerCount) {
		this.tripsCancelledByPassengerCount = tripsCancelledByPassengerCount;
	}

	public int getTripsCancelledByProviderCount() {
		return tripsCancelledByProviderCount;
	}

	public void setTripsCancelledByProviderCount(int tripsCancelledByProviderCount) {
		this.tripsCancelledByProviderCount = tripsCancelledByProviderCount;
	}

	public int getTripsWithConfirmedRideshareCount() {
		return tripsWithConfirmedRideshareCount;
	}

	public void setTripsWithConfirmedRideshareCount(int tripsWithConfirmedRideshareCount) {
		this.tripsWithConfirmedRideshareCount = tripsWithConfirmedRideshareCount;
	}

	public int getTripsWithCancelledRidesharePaymentCount() {
		return tripsWithCancelledRidesharePaymentCount;
	}

	public void setTripsWithCancelledRidesharePaymentCount(int tripsWithCancelledRidesharePaymentCount) {
		this.tripsWithCancelledRidesharePaymentCount = tripsWithCancelledRidesharePaymentCount;
	}

	public int getTripsMonoModalCount() {
		return tripsMonoModalCount;
	}

	public void setTripsMonoModalCount(int tripsMonoModalCount) {
		this.tripsMonoModalCount = tripsMonoModalCount;
	}

	public int getTripsMultiModalCount() {
		return tripsMultiModalCount;
	}

	public void setTripsMultiModalCount(int tripsMultiModalCount) {
		this.tripsMultiModalCount = tripsMultiModalCount;
	}

	public int getTripPlanShoutOutIssuedCount() {
		return tripPlanShoutOutIssuedCount;
	}

	public void setTripPlanShoutOutIssuedCount(int tripPlanShoutOutIssuedCount) {
		this.tripPlanShoutOutIssuedCount = tripPlanShoutOutIssuedCount;
	}

	public int getTripPlanShoutOutAtLeastOneOfferCount() {
		return tripPlanShoutOutAtLeastOneOfferCount;
	}

	public void setTripPlanShoutOutAtLeastOneOfferCount(int tripPlanShoutOutAtLeastOneOfferCount) {
		this.tripPlanShoutOutAtLeastOneOfferCount = tripPlanShoutOutAtLeastOneOfferCount;
	}

	public int getTripPlanShoutOutAcceptedCount() {
		return tripPlanShoutOutAcceptedCount;
	}

	public void setTripPlanShoutOutAcceptedCount(int tripPlanShoutOutAcceptedCount) {
		this.tripPlanShoutOutAcceptedCount = tripPlanShoutOutAcceptedCount;
	}

}


