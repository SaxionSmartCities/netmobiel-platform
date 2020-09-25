package eu.netmobiel.banker.model;

import java.time.Instant;

/**
 * Value class for a settlement order.
 * 
 * @author Jaap Reitsma
 *
 */
public class SettlementOrder {
	private Account originator;
	private Account beneficiary;
	private Integer amount;
	private String description;
	private Instant entryTime;
	private String context;
	
	public SettlementOrder(Account originator, Account beneficiary, 
			Integer amount, String description, Instant entryTime, String context) {
		this.originator = originator;
		this.beneficiary = beneficiary;
		this.amount = amount;
		this.description = description;
		this.entryTime = entryTime;
		this.context = context;
	}
	
	public SettlementOrder(Donation donation) {
		this(donation.getUser().getPersonalAccount(), 
			 donation.getCharity().getAccount(), 
			 donation.getAmount(), 
			 donation.getDescription(), 
			 donation.getDonationTime(), 
			 donation.getReference()
		);
	}
	
	public Account getOriginator() {
		return originator;
	}
	
	public Account getBeneficiary() {
		return beneficiary;
	}
	
	public Integer getAmount() {
		return amount;
	}
	
	public String getDescription() {
		return description;
	}
	
	public Instant getEntryTime() {
		return entryTime;
	}

	public String getContext() {
		return context;
	}
	
}
