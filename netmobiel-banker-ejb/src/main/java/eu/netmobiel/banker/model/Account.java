package eu.netmobiel.banker.model;

import java.util.Objects;

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

@Entity
@Table(name = "account", uniqueConstraints = {
	    @UniqueConstraint(name = "cs_account_unique", columnNames = { "label" })
})
@Vetoed
@SequenceGenerator(name = "account_sg", sequenceName = "account_seq", allocationSize = 1, initialValue = 50)
public class Account {
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_sg")
    private Long id;

	/**
	 * An account has a label. The label is the external identifier of the account.
	 */
	@NotNull
	@Column(name = "label", nullable = false)
    private String label;

    /**
     * An account is owned by someone.
     */
    @ManyToOne
	@Column(name = "holder", nullable = false)
    private User holder;
    
    @Column(name = "account_type")
    private AccountType accountType;
    
    public Account() {
//        accountingEntries = new ArrayList<>();
    }

    public Account(String aLabel) {
        assert aLabel.length() > 0 : "Account label cannot be empty";
        this.label = aLabel;
    }

    public boolean canProcess(@SuppressWarnings("unused") AccountingEntry entry) {
        // by default, accounts are unable to process entries
        return false;
    }

    public boolean hasProcessed(AccountingEntry entry) {
        return this.equals(entry.getAccount()) && entry.getId() != null;
    }

//    public int getBalance() {
//        return entries.values().stream().mapToInt(AccountingEntry::getAmount).sum();
//    }
//
    
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public User getHolder() {
		return holder;
	}

	public void setHolder(User holder) {
		this.holder = holder;
	}

	@Override
	public int hashCode() {
		return Objects.hash(label);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Account other = (Account) obj;
		return Objects.equals(label, other.label);
	}
}
