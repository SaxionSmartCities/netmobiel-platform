package eu.netmobiel.banker.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * An AccountingTransaction captures an atomic transfer of an amount between two or more accounts. A transaction is always balanced, 
 * i.e., the total sum of the amount transferred in an transaction is always zero.
 * 
 * @author Jaap Reitsma
 *
 */
@Entity
@Table(name = "accounting_transaction")
@Vetoed
@SequenceGenerator(name = "accounting_transaction_sg", sequenceName = "accounting_transaction_seq", allocationSize = 1, initialValue = 50)
public class AccountingTransaction {
    private static final Predicate<AccountingEntry> isProcessable;

    static {
        Predicate<AccountingEntry> canProcessEntry = AccountingEntry::canProcess;
        Predicate<AccountingEntry> isProcessedEntry = AccountingEntry::isProcessed;
        // entry is processable if it hasn't been processed, even though it can be processed by its account
        isProcessable = isProcessedEntry.negate().and(canProcessEntry);
    }

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accounting_transaction_sg")
    private Long id;

	@NotNull
	@Column(name = "description", nullable = false)
    private String description;

    private Instant when;

    @ManyToOne
    private Ledger ledger;
    
    @OneToMany(mappedBy = "transaction")
    private List<AccountingEntry> accountingEntries;

    public AccountingTransaction() {
        this.accountingEntries = new ArrayList<>();
    }
    
    public AccountingTransaction(String description, OffsetDateTime someTime) {
    	this();
    	this.description = description;
        this.when = someTime.toInstant();
    }

    public void addEntry(Account account, int amount) {
    	AccountingEntry entry = new AccountingEntry(amount);
    	entry.setAccount(account);
    	entry.setTransaction(this);
    }

    void process() {
        if (accountingEntries.size() < 2) {
        	throw new IllegalStateException("Transactions require at least two entries");
        }
        if (accountingEntries.stream().mapToInt(AccountingEntry::getAmount).sum() != 0) {
        	throw new IllegalStateException("Transaction sums must be zero");
        }
        if (! accountingEntries.stream().allMatch(isProcessable)) {
        	throw new IllegalStateException("All entries of a transaction must be processable");
        }
        accountingEntries.forEach(AccountingEntry::process);
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Instant getWhen() {
		return when;
	}

	public void setWhen(Instant when) {
		this.when = when;
	}

	public List<AccountingEntry> getAccountingEntries() {
		return accountingEntries;
	}

	public void setAccountingEntries(List<AccountingEntry> accountingEntries) {
		this.accountingEntries = accountingEntries;
	}


}
