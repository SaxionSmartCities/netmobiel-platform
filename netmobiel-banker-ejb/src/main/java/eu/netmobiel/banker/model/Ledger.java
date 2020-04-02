package eu.netmobiel.banker.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.Size;

/**
 * A ledger is the collection of all accounting transactions during a certain formal period, such as a fiscal year.
 * A ledger keeps track of the balance of each account involved in a transaction through the balances.
 * 
 * @author Jaap Reitsma
 *
 */
@Entity
@Table(name = "ledger")
@Vetoed
@SequenceGenerator(name = "ledger_sg", sequenceName = "ledger_seq", allocationSize = 1, initialValue = 50)
public class Ledger {
	public static final int NAME_MAX_LENGTH = 32;

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ledger_sg")
    private Long id;

	/**
	 * Start of the accounting period inclusive.
	 */
	@Column(name = "start_period", nullable = false)
	private Instant startPeriod;

	/**
	 * Start of the accounting period exclusive.
	 */
	@Column(name = "end_period", nullable = false)
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
		return transactions;
	}

	public void setTransactions(List<AccountingTransaction> transactions) {
		this.transactions = transactions;
	}

	public List<Balance> getBalances() {
		return balances;
	}

	public void setBalances(List<Balance> balances) {
		this.balances = balances;
	}
	
	public AccountingTransaction createTransaction(String description, OffsetDateTime accountingTime) {
		AccountingTransaction tr = new AccountingTransaction(description, accountingTime);
		tr.setLedger(this);
		this.getTransactions().add(tr);
		return tr;
	}
}
