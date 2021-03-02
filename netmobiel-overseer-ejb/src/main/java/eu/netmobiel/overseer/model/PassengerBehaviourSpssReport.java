package eu.netmobiel.overseer.model;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import com.opencsv.bean.CsvBindAndJoinByName;

import eu.netmobiel.commons.report.PassengerBehaviourReport;
import eu.netmobiel.commons.report.SpssReportBase;

public class PassengerBehaviourSpssReport  extends SpssReportBase<PassengerBehaviourReport> {
	/**
	 * RGP-1 The number of trips created.
	 */
	@CsvBindAndJoinByName(column = "tripsCreatedCount_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> tripsCreatedCount = new ArrayListValuedHashMap<>();
	
	/**
	 * RGP-2 The number of trips cancelled by passenger or by mobility provider.
	 */
	@CsvBindAndJoinByName(column = "tripsCancelledCount_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> tripsCancelledCount = new ArrayListValuedHashMap<>();

	/**
	 * RGP-3 The number of trips cancelled by the passenger.  
	 */
	@CsvBindAndJoinByName(column = "tripsCancelledByPassengerCount_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> tripsCancelledByPassengerCount = new ArrayListValuedHashMap<>();
	
	/**
	 * RGP-4 The number of trips cancelled by the mobility provider.  
	 */
	@CsvBindAndJoinByName(column = "tripsCancelledByProviderCount_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> tripsCancelledByProviderCount = new ArrayListValuedHashMap<>();

	/**
	 * RGP-5 Number of trips with a confirmed rideshare leg 
	 */
	@CsvBindAndJoinByName(column = "tripsWithConfirmedRideshareCount_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> tripsWithConfirmedRideshareCount = new ArrayListValuedHashMap<>();

	/**
	 * RGP-6 Number of trips with a cancelled rideshare leg
	 */
	@CsvBindAndJoinByName(column = "tripsWithCancelledRidesharePaymentCount_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> tripsWithCancelledRidesharePaymentCount = new ArrayListValuedHashMap<>();

	/**
	 * RGP-7 Number of completed monomodal trips (ignoring Walking)
	 */
	@CsvBindAndJoinByName(column = "tripsMonoModalCountt_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> tripsMonoModalCount = new ArrayListValuedHashMap<>();

	/**
	 * RGP-8 mono-modal count per modality (separate report)
	 */
	
	/**
	 * RGP-9 Number of completed multimodal trips (ignoring Walking)
	 */
	@CsvBindAndJoinByName(column = "tripsMultiModalCount_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> tripsMultiModalCount = new ArrayListValuedHashMap<>();

	/**
	 * RGP-10 Multi-modal count per modality (separate report)
	 */

	/**
	 * RGP-11 Count the number of shout-outs issued in a period
	 */
	@CsvBindAndJoinByName(column = "tripPlanShoutOutIssuedCount_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> tripPlanShoutOutIssuedCount = new ArrayListValuedHashMap<>();

	/**
	 * RGP-12 Count the number of shout-outs with at least one offer in a period
	 */
	@CsvBindAndJoinByName(column = "tripPlanShoutOutAtLeastOneOfferCount_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> tripPlanShoutOutAtLeastOneOfferCount = new ArrayListValuedHashMap<>();

	/**
	 * RGP-13 Count the number of accepted shout-outs issued in a period
	 */
	@CsvBindAndJoinByName(column = "tripPlanShoutOutAcceptedCount_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> tripPlanShoutOutAcceptedCount = new ArrayListValuedHashMap<>();

	public PassengerBehaviourSpssReport(String managedIdentity, String home) {
		super(managedIdentity, home);
	}

	@Override
	public void addReportValues(PassengerBehaviourReport ar) {
		// 1 - 4
		tripsCreatedCount.put(String.format("messageCount_%d_%02d", ar.getYear(), ar.getMonth()), ar.getTripsCreatedCount());
		tripsCancelledCount.put(String.format("messageAckedCount_%d_%02d", ar.getYear(), ar.getMonth()), ar.getTripsCancelledCount());
		tripsCancelledByPassengerCount.put(String.format("notificationCount_%d_%02d", ar.getYear(), ar.getMonth()), ar.getTripsCancelledByPassengerCount());
		tripsCancelledByProviderCount.put(String.format("notificationAckedCount_%d_%02d", ar.getYear(), ar.getMonth()), ar.getTripsCancelledByProviderCount());
		
		// 5 - 8
		tripsWithConfirmedRideshareCount.put(String.format("notificationAckedCount_%d_%02d", ar.getYear(), ar.getMonth()), ar.getTripsWithConfirmedRideshareCount());
		tripsWithCancelledRidesharePaymentCount.put(String.format("notificationAckedCount_%d_%02d", ar.getYear(), ar.getMonth()), ar.getTripsWithCancelledRidesharePaymentCount());
		tripsMonoModalCount.put(String.format("notificationAckedCount_%d_%02d", ar.getYear(), ar.getMonth()), ar.getTripsMonoModalCount());
		// No RGP-8
		
		//9 - 12
		tripsMultiModalCount.put(String.format("notificationAckedCount_%d_%02d", ar.getYear(), ar.getMonth()), ar.getTripsMultiModalCount());
		// No RGP-10
		tripPlanShoutOutIssuedCount.put(String.format("notificationAckedCount_%d_%02d", ar.getYear(), ar.getMonth()), ar.getTripPlanShoutOutIssuedCount());
		tripPlanShoutOutAtLeastOneOfferCount.put(String.format("notificationAckedCount_%d_%02d", ar.getYear(), ar.getMonth()), ar.getTripPlanShoutOutAtLeastOneOfferCount());
		tripPlanShoutOutAcceptedCount.put(String.format("notificationAckedCount_%d_%02d", ar.getYear(), ar.getMonth()), ar.getTripPlanShoutOutAcceptedCount());
	}

	public MultiValuedMap<String, Integer> getTripsCreatedCount() {
		return tripsCreatedCount;
	}

	public void setTripsCreatedCount(MultiValuedMap<String, Integer> tripsCreatedCount) {
		this.tripsCreatedCount = tripsCreatedCount;
	}

	public MultiValuedMap<String, Integer> getTripsCancelledCount() {
		return tripsCancelledCount;
	}

	public void setTripsCancelledCount(MultiValuedMap<String, Integer> tripsCancelledCount) {
		this.tripsCancelledCount = tripsCancelledCount;
	}

	public MultiValuedMap<String, Integer> getTripsCancelledByPassengerCount() {
		return tripsCancelledByPassengerCount;
	}

	public void setTripsCancelledByPassengerCount(MultiValuedMap<String, Integer> tripsCancelledByPassengerCount) {
		this.tripsCancelledByPassengerCount = tripsCancelledByPassengerCount;
	}

	public MultiValuedMap<String, Integer> getTripsCancelledByProviderCount() {
		return tripsCancelledByProviderCount;
	}

	public void setTripsCancelledByProviderCount(MultiValuedMap<String, Integer> tripsCancelledByProviderCount) {
		this.tripsCancelledByProviderCount = tripsCancelledByProviderCount;
	}

	public MultiValuedMap<String, Integer> getTripsWithConfirmedRideshareCount() {
		return tripsWithConfirmedRideshareCount;
	}

	public void setTripsWithConfirmedRideshareCount(MultiValuedMap<String, Integer> tripsWithConfirmedRideshareCount) {
		this.tripsWithConfirmedRideshareCount = tripsWithConfirmedRideshareCount;
	}

	public MultiValuedMap<String, Integer> getTripsWithCancelledRidesharePaymentCount() {
		return tripsWithCancelledRidesharePaymentCount;
	}

	public void setTripsWithCancelledRidesharePaymentCount(
			MultiValuedMap<String, Integer> tripsWithCancelledRidesharePaymentCount) {
		this.tripsWithCancelledRidesharePaymentCount = tripsWithCancelledRidesharePaymentCount;
	}

	public MultiValuedMap<String, Integer> getTripsMonoModalCount() {
		return tripsMonoModalCount;
	}

	public void setTripsMonoModalCount(MultiValuedMap<String, Integer> tripsMonoModalCount) {
		this.tripsMonoModalCount = tripsMonoModalCount;
	}

	public MultiValuedMap<String, Integer> getTripsMultiModalCount() {
		return tripsMultiModalCount;
	}

	public void setTripsMultiModalCount(MultiValuedMap<String, Integer> tripsMultiModalCount) {
		this.tripsMultiModalCount = tripsMultiModalCount;
	}

	public MultiValuedMap<String, Integer> getTripPlanShoutOutIssuedCount() {
		return tripPlanShoutOutIssuedCount;
	}

	public void setTripPlanShoutOutIssuedCount(MultiValuedMap<String, Integer> tripPlanShoutOutIssuedCount) {
		this.tripPlanShoutOutIssuedCount = tripPlanShoutOutIssuedCount;
	}

	public MultiValuedMap<String, Integer> getTripPlanShoutOutAtLeastOneOfferCount() {
		return tripPlanShoutOutAtLeastOneOfferCount;
	}

	public void setTripPlanShoutOutAtLeastOneOfferCount(
			MultiValuedMap<String, Integer> tripPlanShoutOutAtLeastOneOfferCount) {
		this.tripPlanShoutOutAtLeastOneOfferCount = tripPlanShoutOutAtLeastOneOfferCount;
	}

	public MultiValuedMap<String, Integer> getTripPlanShoutOutAcceptedCount() {
		return tripPlanShoutOutAcceptedCount;
	}

	public void setTripPlanShoutOutAcceptedCount(MultiValuedMap<String, Integer> tripPlanShoutOutAcceptedCount) {
		this.tripPlanShoutOutAcceptedCount = tripPlanShoutOutAcceptedCount;
	}

}


