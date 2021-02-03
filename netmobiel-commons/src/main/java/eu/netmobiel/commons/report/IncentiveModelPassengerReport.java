package eu.netmobiel.commons.report;

import com.opencsv.bean.CsvBindByName;

public class IncentiveModelPassengerReport extends ReportPeriodKey {
	
	private static final long serialVersionUID = -7202730003166618230L;

	/**
	 * IMP-1: The number of earned credits.
	 */
	@CsvBindByName
	private int earnedCreditsTotal;

	/**
	 * IMP-2: The number of earned credits through usage of the app.  
	 */
	@CsvBindByName
	private int earnedCreditsByAppUsage;
	
	/**
	 * IMP-3: The number of credits spent.
	 */
	@CsvBindByName
	private int spentCreditsTotal;

	/**
	 * IMP-4: The number of credits spent for travelling.
	 */
	@CsvBindByName
	private int spentCreditsForTravelling;
	
	/**
	 * IMP-5: The number of credits spent for charities (i.e., donated to charities).
	 */
	@CsvBindByName
	private int spentCreditsForCharities;
	
	/**
	 * IMP-6: The number of credits spent for personal rewards. 
	 */
	@CsvBindByName
	private int spentCreditsOnRewards;
	
	/**
	 * IMP-7: The number of credits deposited through the bank.
	 */
	@CsvBindByName
	private int depositedCredits;
	
	/**
	 * IMP-8: The number of credits withdrawn through the bank.
	 */
	@CsvBindByName
	private int withdrawnCredits;
	
	/**
	 * IMP-9: The number of trip that have been reviewed.
	 */
	@CsvBindByName
	private int tripsReviewsCount;

	
	public IncentiveModelPassengerReport() {
		
	}
	
	public IncentiveModelPassengerReport(ReportPeriodKey key) {
		super(key);
	}

	public IncentiveModelPassengerReport(String managedIdentity, int year, int month) {
		super(managedIdentity, year, month);
	}

	public int getEarnedCreditsTotal() {
		return earnedCreditsTotal;
	}

	public void setEarnedCreditsTotal(int earnedCreditsTotal) {
		this.earnedCreditsTotal = earnedCreditsTotal;
	}

	public int getEarnedCreditsByAppUsage() {
		return earnedCreditsByAppUsage;
	}

	public void setEarnedCreditsByAppUsage(int earnedCreditsByAppUsage) {
		this.earnedCreditsByAppUsage = earnedCreditsByAppUsage;
	}

	public int getSpentCreditsTotal() {
		return spentCreditsTotal;
	}

	public void setSpentCreditsTotal(int spentCreditsTotal) {
		this.spentCreditsTotal = spentCreditsTotal;
	}

	public int getSpentCreditsForTravelling() {
		return spentCreditsForTravelling;
	}

	public void setSpentCreditsForTravelling(int spentCreditsForTravelling) {
		this.spentCreditsForTravelling = spentCreditsForTravelling;
	}

	public int getSpentCreditsForCharities() {
		return spentCreditsForCharities;
	}

	public void setSpentCreditsForCharities(int spentCreditsForCharities) {
		this.spentCreditsForCharities = spentCreditsForCharities;
	}

	public int getSpentCreditsOnRewards() {
		return spentCreditsOnRewards;
	}

	public void setSpentCreditsOnRewards(int spentCreditsOnRewards) {
		this.spentCreditsOnRewards = spentCreditsOnRewards;
	}

	public int getDepositedCredits() {
		return depositedCredits;
	}

	public void setDepositedCredits(int depositedCredits) {
		this.depositedCredits = depositedCredits;
	}

	public int getWithdrawnCredits() {
		return withdrawnCredits;
	}

	public void setWithdrawnCredits(int withdrawnCredits) {
		this.withdrawnCredits = withdrawnCredits;
	}

	public int getTripsReviewsCount() {
		return tripsReviewsCount;
	}

	public void setTripsReviewsCount(int tripsReviewsCount) {
		this.tripsReviewsCount = tripsReviewsCount;
	}

	@Override
	public String toString() {
		return String.format("%s", getKey());
	}

	
}


