package eu.netmobiel.banker.model;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Size;

/**
 * A ledger is the collection of all accounting transactions during a certain formal period, such as a fiscal year.
 * A ledger keeps track of the balance of each account involved in a transaction through the balances.
 * A ledger has a specific and unique date range. Multiple ledger comprise of a continuous date range.
 * The most recent ledger is open-ended, i.e. no end date is set yet. When closing a ledger, the ledger is in fact split.
 * In effect a new ledger is created and all relevant transactions are moved to the new ledger, balances are recalculated. 
 * 
 * @author Jaap Reitsma
 *
 */
@Entity
@Table(name = "ledger", uniqueConstraints = {
	    @UniqueConstraint(name = "cs_ledger_unique", columnNames = { "name" })
})
@Vetoed
@SequenceGenerator(name = "ledger_sg", sequenceName = "ledger_seq", allocationSize = 1, initialValue = 50)
public class Ledger {
	public static final int NAME_MAX_LENGTH = 32;

	public static final Predicate<Ledger> notClosed = ldg -> ldg.getEndPeriod() == null;

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ledger_sg")
    private Long id;

	/**
	 * Start of the accounting period inclusive.
	 */
	@Column(name = "start_period", nullable = false)
	private Instant startPeriod;

	/**
	 * End of the accounting period exclusive. If not set then this ledger is not yet closed.
	 */
	@Column(name = "end_period", nullable = true)
	private Instant endPeriod;
	
	/**
	 * Name of the period.
	 */
	@Size(max = NAME_MAX_LENGTH)
	@Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
	private String name;

	/**
	 * The list of transactions. Transactions are never removed.
	 */
	@OneToMany(mappedBy = "ledger")
    private List<AccountingTransaction> transactions;

	/**
	 * The list of balances. Each account is coupled to the ledger through a balance.
	 */
	@OneToMany(mappedBy = "ledger")
    private List<Balance> balances;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Instant getStartPeriod() {
		return startPeriod;
	}

	public void setStartPeriod(Instant startPeriod) {
		this.startPeriod = startPeriod;
	}

	public Instant getEndPeriod() {
		return endPeriod;
	}

	public void setEndPeriod(Instant endPeriod) {
		this.endPeriod = endPeriod;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<AccountingTransaction> getTransactions() {
		if (transactions == null) {
			transactions = new ArrayList<>();
		}
		return transactions;
	}

	public void setTransactions(List<AccountingTransaction> transactions) {
		this.transactions = transactions;
	}

	public List<Balance> getBalances() {
		if (balances == null) {
			balances = new ArrayList<>();
		}
		return balances;
	}

	public void setBalances(List<Balance> balances) {
		this.balances = balances;
	}
	
	public AccountingTransaction createTransaction(String description, Instant accountingTime, Instant transactionTime) {
		AccountingTransaction tr = new AccountingTransaction(description, accountingTime, transactionTime);
		tr.setLedger(this);
		this.getTransactions().add(tr);
		return tr;
	}
	
	public void expect(Predicate<Ledger> predicate, String msg) {
		if (!predicate.test(this)) {
			throw new IllegalStateException(msg);  
		}
	}

	public void expectOpen() {
    	expect(Ledger.notClosed, "Ledger is already closed: " + this);
	}

	private String formatDateTime(Instant instant) {
		if (instant == null) {
			return "---";
		} else {
			return DateTimeFormatter.ISO_INSTANT.format(instant);
		}
	}

	@Override
	public String toString() {
		return String.format("Ledger [%s '%s' %s %s]", id, name, formatDateTime(startPeriod), formatDateTime(endPeriod));
	}
	
	
}
