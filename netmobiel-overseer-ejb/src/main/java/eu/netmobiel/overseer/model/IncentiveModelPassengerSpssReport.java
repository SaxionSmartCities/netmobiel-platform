package eu.netmobiel.overseer.model;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import com.opencsv.bean.CsvBindAndJoinByName;

import eu.netmobiel.commons.report.IncentiveModelPassengerReport;
import eu.netmobiel.commons.report.SpssReportBase;

public class IncentiveModelPassengerSpssReport  extends SpssReportBase<IncentiveModelPassengerReport> {
	/**
	 * IMP-1: The number of earned credits.
	 */
	@CsvBindAndJoinByName(column = "earnedCreditsTotal_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> earnedCreditsTotal = new ArrayListValuedHashMap<>();

	/**
	 * IMP-2: The number of earned credits through usage of the app.  
	 */
	@CsvBindAndJoinByName(column = "earnedCreditsByAppUsage_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> earnedCreditsByAppUsage = new ArrayListValuedHashMap<>();
	
	/**
	 * IMP-3: The number of credits spent.
	 */
	@CsvBindAndJoinByName(column = "spentCreditsTotal_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> spentCreditsTotal = new ArrayListValuedHashMap<>();

	/**
	 * IMP-4: The number of credits spent for travelling.
	 */
	@CsvBindAndJoinByName(column = "spentCreditsForTravelling_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> spentCreditsForTravelling = new ArrayListValuedHashMap<>();
	
	/**
	 * IMP-5: The number of credits spent for charities (i.e., donated to charities).
	 */
	@CsvBindAndJoinByName(column = "spentCreditsForCharities_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> spentCreditsForCharities = new ArrayListValuedHashMap<>();
	
	/**
	 * IMP-6: The number of credits spent for personal rewards. 
	 */
	@CsvBindAndJoinByName(column = "spentCreditsOnRewards_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> spentCreditsOnRewards = new ArrayListValuedHashMap<>();
	
	/**
	 * IMP-7: The number of credits deposited through the bank.
	 */
	@CsvBindAndJoinByName(column = "depositedCredits_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> depositedCredits = new ArrayListValuedHashMap<>();
	
	/**
	 * IMP-8: The number of credits withdrawn through the bank.
	 */
	@CsvBindAndJoinByName(column = "withdrawnCredits_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> withdrawnCredits = new ArrayListValuedHashMap<>();
	
	/**
	 * IMP-9: The number of trip that have been reviewed.
	 */
	@CsvBindAndJoinByName(column = "etripsReviewsCount_\\d{4}_\\d{2}", elementType = Integer.class)
	private MultiValuedMap<String, Integer> tripsReviewsCount = new ArrayListValuedHashMap<>();



	public IncentiveModelPassengerSpssReport(String managedIdentity, String home) {
		super(managedIdentity, home);
	}

	@Override
	public void addReportValues(IncentiveModelPassengerReport r) {
		// 1 - 4
		earnedCreditsTotal.put(String.format("earnedCreditsTotal_%d_%02d", r.getYear(), r.getMonth()), r.getEarnedCreditsTotal());
		earnedCreditsByAppUsage.put(String.format("earnedCreditsByAppUsage_%d_%02d", r.getYear(), r.getMonth()), r.getEarnedCreditsByAppUsage());
		spentCreditsTotal.put(String.format("spentCreditsTotal_%d_%02d", r.getYear(), r.getMonth()), r.getSpentCreditsTotal());
		spentCreditsForTravelling.put(String.format("spentCreditsForTravelling_%d_%02d", r.getYear(), r.getMonth()), r.getSpentCreditsForTravelling());
		
		// 5 - 8
		spentCreditsForCharities.put(String.format("spentCreditsForCharities_%d_%02d", r.getYear(), r.getMonth()), r.getSpentCreditsForCharities());
		spentCreditsOnRewards.put(String.format("spentCreditsOnRewards_%d_%02d", r.getYear(), r.getMonth()), r.getSpentCreditsOnRewards());
		depositedCredits.put(String.format("depositedCredits_%d_%02d", r.getYear(), r.getMonth()), r.getDepositedCredits());
		withdrawnCredits.put(String.format("withdrawnCredits_%d_%02d", r.getYear(), r.getMonth()), r.getTripsReviewsCount());

		// 9 - 11
		tripsReviewsCount.put(String.format("tripsReviewsCount_%d_%02d", r.getYear(), r.getMonth()), r.getWithdrawnCredits());
	}

	public MultiValuedMap<String, Integer> getEarnedCreditsTotal() {
		return earnedCreditsTotal;
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

	public MultiValuedMap<String, Integer> getTripsReviewsCount() {
		return tripsReviewsCount;
	}

	public void setTripsReviewsCount(MultiValuedMap<String, Integer> tripsReviewsCount) {
		this.tripsReviewsCount = tripsReviewsCount;
	}

	public void setEarnedCreditsTotal(MultiValuedMap<String, Integer> earnedCreditsTotal) {
		this.earnedCreditsTotal = earnedCreditsTotal;
	}

}


