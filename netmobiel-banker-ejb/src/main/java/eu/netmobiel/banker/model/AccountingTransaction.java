package eu.netmobiel.banker.model;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Vetoed;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import eu.netmobiel.banker.exception.BalanceInsufficientException;
import eu.netmobiel.banker.util.BankerUrnHelper;

/**
 * An AccountingTransaction captures an atomic transfer of an amount between two or more accounts. A transaction is always balanced, 
 * i.e., the total sum of the amount transferred in an transaction is always zero.
 * 
 * Transaction are build using a builder pattern.
 * 
 * @author Jaap Reitsma
 *
 */
@Entity
@Table(name = "accounting_transaction")
@Vetoed
@SequenceGenerator(name = "accounting_transaction_sg", sequenceName = "accounting_transaction_seq", allocationSize = 1, initialValue = 50)
public class AccountingTransaction {
	public static final String URN_PREFIX = BankerUrnHelper.createUrnPrefix("transaction");
	public static final int DESCRIPTION_MAX_LENGTH = 256;

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accounting_transaction_sg")
    private Long id;

	/**
	 * Reference urn to the transaction.
	 */
    @Transient
    private String transactionRef;

    /**
	 * The description of the transaction. For now all entries share the same description.
	 */
	@Size(max = DESCRIPTION_MAX_LENGTH)
	@NotNull
	@Column(name = "description", nullable = false, length = DESCRIPTION_MAX_LENGTH)
    private String description;

	/**
	 * The time of the actual transaction.
	 */
	@NotNull
	@Column(name = "transaction_time", nullable = false)
    private Instant transactionTime;

	/**
	 * The time of the financial fact the transaction applies to.
	 * The accounting time must lay within the ledger period.
	 */
	@NotNull
	@Column(name = "accounting_time", nullable = false)
    private Instant accountingTime;

	/**
	 * The context of the accounting entry. The context is a urn, referring to an object in the system.
	 */
	@Column(name = "context", length = 32, nullable = true)
	private String context;

    /**
	 * The type of the transaction.
	 */
    @Column(name = "transaction_type", nullable = false, length = 2)
    private TransactionType transactionType;
    
	/**
	 * The ledger containing all the transactions for the financial period.
	 */
	@ManyToOne
	@JoinColumn(name = "ledger", nullable = false, foreignKey = @ForeignKey(name = "accounting_transaction_ledger_fk"))
    private Ledger ledger;
    
	/**
	 * The entries making up the transaction. Debet and credit must balance.
	 */
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.PERSIST)
    private List<AccountingEntry> accountingEntries;

    public AccountingTransaction() {
        this.accountingEntries = new ArrayList<>();
    }
    
    AccountingTransaction(TransactionType type, String description, String context, Instant accountingTime, Instant transactionTime) {
    	this();
    	this.transactionType = type;
    	this.description = description;
    	this.context = context;
        this.accountingTime = accountingTime;
        this.transactionTime = transactionTime;
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTransactionRef() {
    	if (transactionRef == null) {
    		transactionRef = BankerUrnHelper.createUrn(AccountingTransaction.URN_PREFIX, getId());
    	}
		return transactionRef;
	}
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<AccountingEntry> getAccountingEntries() {
		if (accountingEntries == null) {
			accountingEntries = new ArrayList<>();
		}
		return accountingEntries;
	}

	public Instant getTransactionTime() {
		return transactionTime;
	}

	public void setTransactionTime(Instant transactionTime) {
		this.transactionTime = transactionTime;
	}

	public Instant getAccountingTime() {
		return accountingTime;
	}

	public void setAccountingTime(Instant accountingTime) {
		this.accountingTime = accountingTime;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public TransactionType getTransactionType() {
		return transactionType;
	}

	public void setTransactionType(TransactionType transactionType) {
		this.transactionType = transactionType;
	}

	public Ledger getLedger() {
		return ledger;
	}

	static AccountingTransaction.Builder newTransaction(Ledger ledger, TransactionType type, String description, String context, Instant accountingTime, Instant transactionTime) {
		AccountingTransaction tr = new AccountingTransaction(type, description, context, accountingTime, transactionTime);
		tr.ledger = ledger;
		return new Builder(tr);
	}

	public static class Builder {
		private AccountingTransaction transaction;
		private boolean finished = false;
		
		Builder(AccountingTransaction tr) {
			this.transaction = tr;
		}
		
		protected void addAccountingEntry(Account account, String counterparty, AccountingEntry entry) {
			entry.setAccount(account);
			entry.setCounterparty(counterparty);
			entry.setTransaction(transaction);
			transaction.getAccountingEntries().add(entry);
		}

		protected void expectNotFinished() {
			if (finished) {
				throw new IllegalStateException("Cannot rebuild transaction: " + transaction.toString());
			}
		}
		
		public AccountingTransaction.Builder debit(Balance balance, int amount, String counterparty) throws BalanceInsufficientException {
			expectNotFinished();
			addAccountingEntry(balance.getAccount(), counterparty, new AccountingEntry(AccountingEntryType.DEBIT, amount));
			balance.debit(amount);
			return this;
		}
		
		public AccountingTransaction.Builder credit(Balance balance, int amount, String counterparty) throws BalanceInsufficientException {
			expectNotFinished();
			addAccountingEntry(balance.getAccount(), counterparty, new AccountingEntry(AccountingEntryType.CREDIT, amount));
			balance.credit(amount);
			return this;
		}

	    public AccountingTransaction build() {
			expectNotFinished();
	        if (transaction.accountingEntries.size() < 2) {
	        	throw new IllegalStateException("Transactions require at least two entries");
	        }
	        int credit = transaction.accountingEntries.stream()
	        		.filter(e -> e.getEntryType() == AccountingEntryType.CREDIT)
	        		.mapToInt(AccountingEntry::getAmount).sum();
	        int debit = transaction.accountingEntries.stream()
	        		.filter(e -> e.getEntryType() == AccountingEntryType.DEBIT)
	        		.mapToInt(AccountingEntry::getAmount).sum();
	        if (debit != credit) {
	        	throw new IllegalStateException("Transaction is not balanced");
	        }
	        if (! transaction.ledger.fitsPeriod(transaction.accountingTime)) {
	        	throw new IllegalStateException(String.format("AccountingTime %s does not match ledger '%s' period", 
	        			DateTimeFormatter.ISO_INSTANT.format(transaction.accountingTime), transaction.ledger.getName()));
	        }
	        finished = true;
	        return transaction;
	    }

	}
	
}
