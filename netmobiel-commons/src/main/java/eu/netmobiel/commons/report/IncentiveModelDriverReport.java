package eu.netmobiel.commons.report;

import com.opencsv.bean.CsvBindByName;

public class IncentiveModelDriverReport extends ReportPeriodKey {
	
	private static final long serialVersionUID = -7202730003166618230L;

	/**
	 * IMC-1: The number of earned credits.
	 */
	@CsvBindByName
	private int earnedCreditsTotal;

	/**
	 * IMC-2: The number of credits earned through driving.  
	 */
	@CsvBindByName
	private int earnedCreditsRides;

	/**
	 * IMC-3: The number of earned credits through usage of the app.  
	 */
	@CsvBindByName
	private int earnedCreditsByAppUsage;
	
	/**
	 * IMC-4: The number of credits spent.
	 */
	@CsvBindByName
	private int spentCreditsTotal;

	/**
	 * IMC-5: The number of credits spent for travelling.
	 */
	@CsvBindByName
	private int spentCreditsForTravelling;
	
	/**
	 * IMC-6: The number of credits spent for charities (i.e., donated to charities).
	 */
	@CsvBindByName
	private int spentCreditsForCharities;
	
	/**
	 * IMC-7: The number of credits spent for personal rewards. 
	 */
	@CsvBindByName
	private int spentCreditsOnRewards;
	
	/**
	 * IMC-8: The number of credits deposited through the bank.
	 */
	@CsvBindByName
	private int depositedCredits;
	
	/**
	 * IMC-9: The number of credits withdrawn through the bank.
	 */
	@CsvBindByName
	private int withdrawnCredits;
	
	/**
	 * IMC-10: The number of rides that have been reviewed by the driver.
	 */
	@CsvBindByName
	private int ridesReviewsCount;

	/**
	 * IMC-11: The number of rides that have lead to credits for the driver.
	 */
	@CsvBindByName
	private int ridesCreditedCount;
	
	public IncentiveModelDriverReport() {
		
	}
	
	public IncentiveModelDriverReport(ReportPeriodKey key) {
		super(key);
	}

	public IncentiveModelDriverReport(String managedIdentity, int year, int month) {
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

	public int getEarnedCreditsRides() {
		return earnedCreditsRides;
	}

	public void setEarnedCreditsRides(int earnedCreditsRides) {
		this.earnedCreditsRides = earnedCreditsRides;
	}

	public int getRidesReviewsCount() {
		return ridesReviewsCount;
	}

	public void setRidesReviewsCount(int ridesReviewsCount) {
		this.ridesReviewsCount = ridesReviewsCount;
	}

	public int getRidesCreditedCount() {
		return ridesCreditedCount;
	}

	public void setRidesCreditedCount(int ridesCreditedCount) {
		this.ridesCreditedCount = ridesCreditedCount;
	}

	@Override
	public String toString() {
		return String.format("%s", getKey());
	}

	
}


