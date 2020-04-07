package eu.netmobiel.banker.model;

import java.sql.Timestamp;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

/**
 * The balance of an account concerns a specific ledger (of a fiscal year). The balance(s) of each account 
 * involved in a transaction is updated with each transaction. The balance reflects the state at the end of the period. 
 * Journal entries are never modified or removed, so the balance is always accurate (as long as the start is correctly calculated).
 * If accounting entries are inserted between older entries the balance is still correct, because it shows the balance at the end of
 * the period.
 * 
 * @author Jaap Reitsma
 *
 */
@Entity
@Table(name = "balance", uniqueConstraints = {
	    @UniqueConstraint(name = "cs_balance_unique", columnNames = { "account", "ledger" })
})
@Vetoed
@SequenceGenerator(name = "balance_sg", sequenceName = "balance_seq", allocationSize = 1, initialValue = 50)
public class Balance {

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "balance_sg")
    private Long id;

	@Version
	@Column(name = "version")
	private int version;
	
	@Column(name = "modifiedAt", insertable = false, updatable = false)
	private Timestamp modifiedAt;
	
	@Column(name = "start_amount", nullable = false)
	private int startAmount;
	
	@Column(name = "end_amount", nullable = false)
	private int endAmount;
	
	@ManyToOne
	@JoinColumn(name = "ledger", nullable = false, foreignKey = @ForeignKey(name = "balance_ledger_fk"))
	private Ledger ledger;
	
	@ManyToOne
	@JoinColumn(name = "account", nullable = false, foreignKey = @ForeignKey(name = "balance_account_fk"))
	private Account account;

	public Balance() {
		
	}
	
	public Balance(Ledger ledger, Account acc, int start) {
		this.ledger = ledger;
		this.account = acc;
		this.startAmount = start;
		this.endAmount = this.startAmount;
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getStartAmount() {
		return startAmount;
	}

	public void setStartAmount(int startAmount) {
		this.startAmount = startAmount;
	}

	public int getEndAmount() {
		return endAmount;
	}

	public void setEndAmount(int endAmount) {
		this.endAmount = endAmount;
	}

	public Ledger getLedger() {
		return ledger;
	}

	public void setLedger(Ledger ledger) {
		this.ledger = ledger;
	}

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	public Timestamp getModifiedAt() {
		return modifiedAt;
	}
	
	public void debit(int amount) {
		if (account.getAccountType() == AccountType.ASSET) {
			endAmount += amount;
		} else if (account.getAccountType() == AccountType.LIABILITY) {
			endAmount -= amount;
		} else {
			throw new IllegalArgumentException("Account type is not supported");
		}
	}

	public void credit(int amount) {
		if (account.getAccountType() == AccountType.ASSET) {
			endAmount -= amount;
		} else if (account.getAccountType() == AccountType.LIABILITY) {
			endAmount += amount;
		} else {
			throw new IllegalArgumentException("Account type is not supported");
		}
	}
}
