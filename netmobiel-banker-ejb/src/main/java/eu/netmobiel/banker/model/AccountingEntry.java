package eu.netmobiel.banker.model;

import java.time.format.DateTimeFormatter;

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
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * An accounting (or journal) entry has an amount and an description and concerns a specific account. An entry is processed as part of a
 * particular transaction. In each transaction an account can only appear once.
 * 
 * @author Jaap Reitsma
 *
 */
@Entity
@Table(name = "accounting_entry", uniqueConstraints = {
	    @UniqueConstraint(name = "cs_transaction_account_unique", columnNames = { "account", "transaction" })
})
@Vetoed
@SequenceGenerator(name = "accounting_entry_sg", sequenceName = "accounting_entry_seq", allocationSize = 1, initialValue = 50)
public class AccountingEntry {
	public static final int COUNTERPARTY_MAX_LENGTH = 96;

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accounting_entry_sg")
    private Long id;

	/**
	 * The account involved in the entry. Once saved in the database it cannot be changed.
	 */
	@NotNull
	@ManyToOne
	@JoinColumn(name = "account", nullable = false, foreignKey = @ForeignKey(name = "accounting_entry_account_fk"))
    private Account account;

    /**
     * The name of the counterparty in case of a payment. This field is added for convenience to create
     * a statement for a particular account, without the necessity to retrieve the counter entry.
     * The name of the counterparty account will be used for simplicity.  
     */
	@Size(max = COUNTERPARTY_MAX_LENGTH)
    @Column(name = "counterparty", length = COUNTERPARTY_MAX_LENGTH, nullable = true)
    private String counterparty;

	/**
	 * The amount (of credits). Once saved in the database it cannot be changed.
	 */
	@NotNull
	@Column(name = "amount", nullable = false)
    private int amount;

    /**
     * The accounting entry type.
     */
    @Column(name = "entry_type", nullable = false, length = 1)
    private AccountingEntryType entryType;

    /**
     * The transaction this entry belongs to.
     */
    @NotNull
	@ManyToOne
	@JoinColumn(name = "transaction", nullable = false, foreignKey = @ForeignKey(name = "accounting_entry_transaction_fk"))
    private AccountingTransaction transaction;
	
    public AccountingEntry() {
    	
    }
    
    public AccountingEntry(AccountingEntryType entryType, int amount) {
        assert amount != 0 : "Amount of accounting entry must be nonzero";
        this.entryType = entryType;
        this.amount = amount;
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	public String getCounterparty() {
		return counterparty;
	}

	public void setCounterparty(String counterparty) {
		this.counterparty = counterparty;
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}

	public AccountingTransaction getTransaction() {
		return transaction;
	}

	public void setTransaction(AccountingTransaction transaction) {
		this.transaction = transaction;
	}

	public AccountingEntryType getEntryType() {
		return entryType;
	}

	public void setEntryType(AccountingEntryType entryType) {
		this.entryType = entryType;
	}

	@Override
	public String toString() {
		return String.format("AccountingEntry [%s, %s, %s, %s, '%s', %s, %s ]", id,
				account.getReference(), amount, entryType, transaction.getDescription(), 
				DateTimeFormatter.ISO_INSTANT.format(transaction.getAccountingTime()), 
				DateTimeFormatter.ISO_INSTANT.format(transaction.getTransactionTime()));
	}
    
}
