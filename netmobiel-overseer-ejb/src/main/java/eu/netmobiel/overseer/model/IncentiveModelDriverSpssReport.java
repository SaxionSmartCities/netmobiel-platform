package eu.netmobiel.overseer.model;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import com.opencsv.bean.CsvBindAndJoinByName;

import eu.netmobiel.commons.report.IncentiveModelDriverReport;
import eu.netmobiel.commons.report.SpssReportBase;

public class IncentiveModelDriverSpssReport  extends SpssReportBase<IncentiveModelDriverReport> {
	/**
	 * IMC-1: The number of earned credits.
	 */
	@CsvBindAndJoinByName(column = "earnedCreditsTotal_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> earnedCreditsTotal = new ArrayListValuedHashMap<>();

	/**
	 * IMC-2: The number of credits earned through driving.  
	 */
	@CsvBindAndJoinByName(column = "earnedCreditsRides_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> earnedCreditsRides = new ArrayListValuedHashMap<>();

	/**
	 * IMC-3: The number of earned credits through usage of the app.  
	 */
	@CsvBindAndJoinByName(column = "earnedCreditsByAppUsage_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> earnedCreditsByAppUsage = new ArrayListValuedHashMap<>();
	
	/**
	 * IMC-4: The number of credits spent.
	 */
	@CsvBindAndJoinByName(column = "spentCreditsTotal_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> spentCreditsTotal = new ArrayListValuedHashMap<>();

	/**
	 * IMC-5: The number of credits spent for travelling.
	 */
	@CsvBindAndJoinByName(column = "spentCreditsForTravelling_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> spentCreditsForTravelling = new ArrayListValuedHashMap<>();
	
	/**
	 * IMC-6: The number of credits spent for charities (i.e., donated to charities).
	 */
	@CsvBindAndJoinByName(column = "spentCreditsForCharities_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> spentCreditsForCharities = new ArrayListValuedHashMap<>();
	
	/**
	 * IMC-7: The number of credits spent for personal rewards. 
	 */
	@CsvBindAndJoinByName(column = "spentCreditsOnRewards_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> spentCreditsOnRewards = new ArrayListValuedHashMap<>();
	
	/**
	 * IMC-8: The number of credits deposited through the bank.
	 */
	@CsvBindAndJoinByName(column = "depositedCredits_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> depositedCredits = new ArrayListValuedHashMap<>();
	
	/**
	 * IMC-9: The number of credits withdrawn through the bank.
	 */
	@CsvBindAndJoinByName(column = "withdrawnCredits_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> withdrawnCredits = new ArrayListValuedHashMap<>();
	
	/**
	 * IMC-10: The number of rides that have been reviewed by the driver.
	 */
	@CsvBindAndJoinByName(column = "ridesReviewsCount_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> ridesReviewsCount = new ArrayListValuedHashMap<>();

	/**
	 * IMC-11: The number of rides that have lead to credits for the driver.
	 */
	@CsvBindAndJoinByName(column = "ridesCreditedCount_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> ridesCreditedCount = new ArrayListValuedHashMap<>();
	
	

	public IncentiveModelDriverSpssReport(String managedIdentity, String home) {
		super(managedIdentity, home);
	}

	@Override
	public void addReportValues(IncentiveModelDriverReport r) {
		// 1 - 4
		earnedCreditsTotal.put(String.format("earnedCreditsTotal_%d_%02d", r.getYear(), r.getMonth()), r.getEarnedCreditsTotal());
		earnedCreditsRides.put(String.format("earnedCreditsRides_%d_%02d", r.getYear(), r.getMonth()), r.getEarnedCreditsRides());
		earnedCreditsByAppUsage.put(String.format("earnedCreditsByAppUsage_%d_%02d", r.getYear(), r.getMonth()), r.getEarnedCreditsByAppUsage());
		spentCreditsTotal.put(String.format("spentCreditsTotal_%d_%02d", r.getYear(), r.getMonth()), r.getSpentCreditsTotal());
		
		// 5 - 8
		spentCreditsForTravelling.put(String.format("spentCreditsForTravelling_%d_%02d", r.getYear(), r.getMonth()), r.getSpentCreditsForTravelling());
		spentCreditsForCharities.put(String.format("spentCreditsForCharities_%d_%02d", r.getYear(), r.getMonth()), r.getSpentCreditsForCharities());
		spentCreditsOnRewards.put(String.format("spentCreditsOnRewards_%d_%02d", r.getYear(), r.getMonth()), r.getSpentCreditsOnRewards());
		depositedCredits.put(String.format("depositedCredits_%d_%02d", r.getYear(), r.getMonth()), r.getDepositedCredits());

		// 9 - 11
		withdrawnCredits.put(String.format("withdrawnCredits_%d_%02d", r.getYear(), r.getMonth()), r.getWithdrawnCredits());
		ridesReviewsCount.put(String.format("ridesReviewsCount_%d_%02d", r.getYear(), r.getMonth()), r.getRidesReviewsCount());
		ridesCreditedCount.put(String.format("ridesCreditedCount_%d_%02d", r.getYear(), r.getMonth()), r.getRidesCreditedCount());
}

	public MultiValuedMap<String, Integer> getEarnedCreditsTotal() {
		return earnedCreditsTotal;
	}

	public void setEarnedCreditsTotal(MultiValuedMap<String, Integer> earnedCreditsTotal) {
		this.earnedCreditsTotal = earnedCreditsTotal;
	}

	public MultiValuedMap<String, Integer> getEarnedCreditsRides() {
		return earnedCreditsRides;
	}

	public void setEarnedCreditsRides(MultiValuedMap<String, Integer> earnedCreditsRides) {
		this.earnedCreditsRides = earnedCreditsRides;
	}

	public MultiValuedMap<String, Integer> getEarnedCreditsByAppUsage() {
		return earnedCreditsByAppUsage;
	}

	public void setEarnedCreditsByAppUsage(MultiValuedMap<String, Integer> earnedCreditsByAppUsage) {
		this.earnedCreditsByAppUsage = earnedCreditsByAppUsage;
	}

	public MultiValuedMap<String, Integer> getSpentCreditsTotal() {
		return spentCreditsTotal;
	}

	public void setSpentCreditsTotal(MultiValuedMap<String, Integer> spentCreditsTotal) {
		this.spentCreditsTotal = spentCreditsTotal;
	}

	public MultiValuedMap<String, Integer> getSpentCreditsForTravelling() {
		return spentCreditsForTravelling;
	}

	public void setSpentCreditsForTravelling(MultiValuedMap<String, Integer> spentCreditsForTravelling) {
		this.spentCreditsForTravelling = spentCreditsForTravelling;
	}

	public MultiValuedMap<String, Integer> getSpentCreditsForCharities() {
		return spentCreditsForCharities;
	}

	public void setSpentCreditsForCharities(MultiValuedMap<String, Integer> spentCreditsForCharities) {
		this.spentCreditsForCharities = spentCreditsForCharities;
	}

	public MultiValuedMap<String, Integer> getSpentCreditsOnRewards() {
		return spentCreditsOnRewards;
	}

	public void setSpentCreditsOnRewards(MultiValuedMap<String, Integer> spentCreditsOnRewards) {
		this.spentCreditsOnRewards = spentCreditsOnRewards;
	}

	public MultiValuedMap<String, Integer> getDepositedCredits() {
		return depositedCredits;
	}

	public void setDepositedCredits(MultiValuedMap<String, Integer> depositedCredits) {
		this.depositedCredits = depositedCredits;
	}

	public MultiValuedMap<String, Integer> getWithdrawnCredits() {
		return withdrawnCredits;
	}

	public void setWithdrawnCredits(MultiValuedMap<String, Integer> withdrawnCredits) {
		this.withdrawnCredits = withdrawnCredits;
	}

	public MultiValuedMap<String, Integer> getRidesReviewsCount() {
		return ridesReviewsCount;
	}

	public void setRidesReviewsCount(MultiValuedMap<String, Integer> ridesReviewsCount) {
		this.ridesReviewsCount = ridesReviewsCount;
	}

	public MultiValuedMap<String, Integer> getRidesCreditedCount() {
		return ridesCreditedCount;
	}

	public void setRidesCreditedCount(MultiValuedMap<String, Integer> ridesCreditedCount) {
		this.ridesCreditedCount = ridesCreditedCount;
	}

}


