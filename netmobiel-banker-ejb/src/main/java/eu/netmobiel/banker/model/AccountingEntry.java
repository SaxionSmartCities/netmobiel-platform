package eu.netmobiel.banker.model;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

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
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accounting_entry_sg")
    private Long id;

	@NotNull
	@ManyToOne()
	@Column(name = "account", nullable = false, updatable = false)
    private Account account;

	@NotNull
	@Column(name = "amount", nullable = false, updatable = false)
    private int amount;

	@NotNull
	@ManyToOne()
	@Column(name = "transaction", nullable = false, updatable = false)
    private AccountingTransaction transaction;
	
    public AccountingEntry() {
    	
    }
    
    public AccountingEntry(int amount) {
        assert amount != 0 : "Amount of accounting entry must be nonzero";
        this.amount = amount;
    }

    public boolean canProcess() {
        return account.canProcess(this);
    }

    public boolean isProcessed() {
        return account.hasProcessed(this);
    }

    public void process() {
        if (!account.hasProcessed(this)) {
        	throw new IllegalStateException("Cannot process accounting entries twice");
        }
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
		transaction.getAccountingEntries().add(this);
	}
    
}
