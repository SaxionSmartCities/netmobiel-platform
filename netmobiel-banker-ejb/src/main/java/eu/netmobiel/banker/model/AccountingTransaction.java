package eu.netmobiel.banker.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.inject.Vetoed;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import eu.netmobiel.banker.exception.BalanceInsufficientException;
import eu.netmobiel.banker.util.BankerUrnHelper;
import eu.netmobiel.commons.util.UrnHelper;

/**
 * An AccountingTransaction captures an atomic transfer of an amount between two or more accounts. A transaction is always balanced, 
 * i.e., the total sum of the amount transferred in an transaction is always zero.
 * 
 * Transactions can be part of a conversation: A conversation starts with a reservation and finishes ultimately with either a cancel
 * or a payment. With the introduction of the 'unvalidate' feature, both a payment and cancel can be reversed. However, the original reservation
 * must not be forgotten, because the origin of the reserved credits must be tracked: Are these premium credits or regular credits?
 * The top of the conversation points to the first reservation. 
 * 
 * In case of a direct charge, the charge transaction itself has already the information about the origin of the credits.
 * Transactions are by definition monotonic increasing with time, so tracking the initial transaction is sufficient in
 * combination with the transaction time to order the transactions by conversation and time.  
 * 
 * Transaction are build using a builder pattern.
 * 
 * @author Jaap Reitsma
 *
 */
@NamedEntityGraph(
		// Only users involved in a role can view the roles and the accounts.
		name = AccountingTransaction.ENTRIES_ENTITY_GRAPH, 
		attributeNodes = { 
			@NamedAttributeNode(value = "ledger"),		
			@NamedAttributeNode(value = "accountingEntries", subgraph = "subgraph.accountingEntry"),		
//			@NamedAttributeNode(value = "head", subgraph = "subgraph.accountingTransaction"),		
		}, subgraphs = {
				@NamedSubgraph(
						name = "subgraph.accountingEntry",
						attributeNodes = {
								@NamedAttributeNode(value = "account"),
								@NamedAttributeNode(value = "counterparty")
						}
				)
		}
)
@Entity
@Table(name = "accounting_transaction")
@Vetoed
@SequenceGenerator(name = "accounting_transaction_sg", sequenceName = "accounting_transaction_seq", allocationSize = 1, initialValue = 50)
public class AccountingTransaction  implements Serializable {
	private static final long serialVersionUID = 3727743146030632429L;
	public static final String URN_PREFIX = BankerUrnHelper.createUrnPrefix("transaction");
	public static final int DESCRIPTION_MAX_LENGTH = 256;
	public static final String ENTRIES_ENTITY_GRAPH = "accounting_transaction-entries-graph";

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
	 * The ledger containing all the transactions for the financial period.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ledger", nullable = false, foreignKey = @ForeignKey(name = "accounting_transaction_ledger_fk"))
    private Ledger ledger;
    
	/**
	 * The entries making up the transaction. Debet and credit must balance.
	 */
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private List<AccountingEntry> accountingEntries;

    /**
     * If set to true this is a rollback of an earlier transaction (with the same context).
     * Used to indicate to the involved users that this concerns a special transaction. 
     */
	@Column(name = "is_rollback", nullable = false)
    private boolean rollback;

	/**
	 * The head points to the first transaction of an accounting conversation. If null then this transaction is the head.
	 * Transaction are ordered by transaction time, implicitly by the primary key as well.
	 * When a reservation is cancelled, always use the head transaction to cancel. Only the head contains the information
	 * about the origin of the credits reserved, relevant in case of premium credits. 
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "head", nullable = true, foreignKey = @ForeignKey(name = "accounting_transaction_head_fk"))
    private AccountingTransaction head;

	public AccountingTransaction() {
        this.accountingEntries = new ArrayList<>();
    }
    
    AccountingTransaction(AccountingTransaction theHead, String description, String context, Instant accountingTime, Instant transactionTime) {
    	this();
    	this.head = theHead;
    	this.description = description;
    	this.context = context;
        this.accountingTime = accountingTime;
        this.transactionTime = transactionTime;
    }

    AccountingTransaction(AccountingTransaction theHead, Instant accountingTime, Instant transactionTime) {
    	this();
    	this.head = theHead;
    	this.ledger = theHead.getLedger();
    	this.description = theHead.getDescription();
    	this.context = theHead.getContext();
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
    		transactionRef = UrnHelper.createUrn(AccountingTransaction.URN_PREFIX, getId());
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

	public boolean isRollback() {
		return rollback;
	}

	public Ledger getLedger() {
		return ledger;
	}

	public AccountingTransaction getHead() {
		return head;
	}

	public Optional<AccountingEntry> entryOf(Account acc) {
		return getAccountingEntries().stream()
				.filter(e -> e.getAccount().equals(acc))
				.findFirst();
	}

	static AccountingTransaction.Builder newTransaction(Ledger ledger, AccountingTransaction theHead, String description, String context, Instant accountingTime, Instant transactionTime) {
		AccountingTransaction tr = new AccountingTransaction(theHead, description, context, accountingTime, transactionTime);
		tr.ledger = ledger;
		return new Builder(tr);
	}

	public static class Builder {
		private AccountingTransaction transaction;
		private boolean finished = false;
		
		Builder(AccountingTransaction tr) {
			this.transaction = tr;
		}
		
		protected void addAccountingEntry(Account account, Account counterparty, AccountingEntry entry) {
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
		
		public AccountingTransaction.Builder rollback(boolean isRollback) {
			this.transaction.rollback = isRollback;
			return this;
		}

		public AccountingTransaction.Builder head(AccountingTransaction theHead) {
			this.transaction.head = theHead;
			return this;
		}

		public AccountingTransaction.Builder debit(Balance balance, int amount, TransactionType purpose, Account counterparty) throws BalanceInsufficientException {
			expectNotFinished();
			addAccountingEntry(balance.getAccount(), counterparty, new AccountingEntry(AccountingEntryType.DEBIT, amount, purpose));
			balance.debit(amount);
			return this;
		}
		
		public AccountingTransaction.Builder credit(Balance balance, int amount, TransactionType purpose, Account counterparty) throws BalanceInsufficientException {
			expectNotFinished();
			addAccountingEntry(balance.getAccount(), counterparty, new AccountingEntry(AccountingEntryType.CREDIT, amount, purpose));
			balance.credit(amount);
			return this;
		}

		public AccountingTransaction.Builder transfer(Balance fromBalance, int amount, TransactionType purpose, Balance toBalance) throws BalanceInsufficientException {
			debit(fromBalance, amount, purpose, toBalance.getAccount());
			credit(toBalance, amount, purpose, fromBalance.getAccount());
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
	
    public AccountingEntry findByEntryType(AccountingEntryType type) {
    	List<AccountingEntry> entries = getAccountingEntries().stream()
    			.filter(e -> e.getEntryType() == type)
    			.collect(Collectors.toList());
    	if (entries.size() != 1) {
    		throw new IllegalStateException("No support for multiple entries of the same type in a transaction: " + getId());
    	}
    	return entries.get(0);
    }

    public AccountingEntry lookupByEntryAccount(String ncan) {
    	List<AccountingEntry> rs_entries = getAccountingEntries();
    	AccountingEntry entry = rs_entries.stream()
    			.filter(e -> e.getAccount().getNcan().equals(ncan))
    			.findFirst()
    			.orElseThrow(() -> new IllegalStateException("No such account in transaction: " + ncan + " " + getId()));
    	return entry;
    }

    public AccountingEntry lookupByCounterParty(String ncan) {
    	List<AccountingEntry> rs_entries = getAccountingEntries();
    	AccountingEntry entry = rs_entries.stream()
    			.filter(e -> e.getCounterparty().getNcan().equals(ncan))
    			.findFirst()
    			.orElseThrow(() -> new IllegalStateException("No such counterparty account in transaction: " + ncan + " " + getId()));
    	return entry;
    }

    public AccountingEntry lookupByEntryAccount(Account acc) {
    	List<AccountingEntry> rs_entries = getAccountingEntries();
    	AccountingEntry entry = rs_entries.stream()
    			.filter(e -> e.getAccount().getId().equals(acc.getId()))
    			.findFirst()
    			.orElseThrow(() -> new IllegalStateException("No such account in transaction: " + acc.getId() + " " + getId()));
    	return entry;
    }

    public AccountingEntry lookupByCounterParty(Account acc) {
    	List<AccountingEntry> rs_entries = getAccountingEntries();
    	AccountingEntry entry = rs_entries.stream()
    			.filter(e -> e.getCounterparty().getId().equals(acc.getId()))
    			.findFirst()
    			.orElseThrow(() -> new IllegalStateException("No such counterparty account in transaction: " + acc.getId() + " " + getId()));
    	return entry;
    }

    public AccountingEntry lookup(TransactionType purpose, AccountingEntryType entryType) {
    	List<AccountingEntry> rs_entries = getAccountingEntries();
    	AccountingEntry entry = rs_entries.stream()
    			.filter(e -> e.getPurpose() == purpose && e.getEntryType() == entryType)
    			.findFirst()
    			.orElseThrow(() -> new IllegalStateException("No such purpose/entryType combination in transaction: " 
    					+ purpose + " " + entryType + " " + getId()));
    	return entry;
    }

    public boolean hasEntry(TransactionType purpose) {
    	return getAccountingEntries().stream()
    			.filter(e -> e.getPurpose() == purpose)
    			.findFirst()
    			.isPresent();
    }
}
