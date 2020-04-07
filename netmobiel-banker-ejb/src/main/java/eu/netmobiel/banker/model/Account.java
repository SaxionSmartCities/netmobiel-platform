package eu.netmobiel.banker.model;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Predicate;

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
 * Formal record that represents, in words, money or other unit of measurement, certain resources, claims to such 
 * resources, transactions or other events that result in changes to those resources and claims. An account is 
 * externally identified by a unique accountNumber.
 * 
 * How do we relate the bank account number to the netmobiel account number?
 *  
 * @author Jaap Reitsma
 *
 */
@Entity
@Table(name = "account", uniqueConstraints = {
	    @UniqueConstraint(name = "cs_account_unique", columnNames = { "reference" })
})
@Vetoed
@SequenceGenerator(name = "account_sg", sequenceName = "account_seq", allocationSize = 1, initialValue = 50)
public class Account {
	public static final int ACCOUNT_NUMBER_MAX_LENGTH = 32;
	
	public static final Predicate<Account> isAsset = acc -> acc.getAccountType() == AccountType.ASSET;
	public static final Predicate<Account> isLiability = acc -> acc.getAccountType() == AccountType.LIABILITY;
	public static final Predicate<Account> isOpen = acc -> acc.getClosedTime() == null;

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_sg")
    private Long id;

	/**
	 * An account has a accountNumber. The accountNumber is the external identifier of the account.
	 */
	@Size(max = ACCOUNT_NUMBER_MAX_LENGTH)
	@NotNull
	@Column(name = "reference", nullable = false, length = ACCOUNT_NUMBER_MAX_LENGTH)
    private String reference;

    /**
     * An account is owned by someone.
     */
    @ManyToOne
	@JoinColumn(name = "holder", nullable = false, foreignKey = @ForeignKey(name = "account_user_fk"))
    private User holder;
    
    /**
     * The account type.
     */
    @Column(name = "account_type", length = 1)
    private AccountType accountType;

    /**
     * Time of creation of the account.
     */
    @Column(name = "created_time", nullable = false)
    private Instant createdTime;

    /**
     * Time of closing of the account. A closed account cannot be used anymore. Before closing, the balance must be neutralized.
     * If null then the account is open.
     */
    @Column(name = "closed_time", nullable = true)
    private Instant closedTime;

    public Account() {
    }

    public static Account newInstant(User aHolder, String aReference, AccountType type) {
    	Account acc = new Account();
    	acc.holder = aHolder;
    	acc.reference = aReference;
    	acc.accountType = type;
        acc.createdTime = Instant.now();
        return acc;
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public User getHolder() {
		return holder;
	}

	public void setHolder(User holder) {
		this.holder = holder;
	}

	public AccountType getAccountType() {
		return accountType;
	}

	public void setAccountType(AccountType accountType) {
		this.accountType = accountType;
	}

	public void expect(Predicate<Account> predicate, String msg) {
		if (!predicate.test(this)) {
			throw new IllegalStateException(msg);  
		}
	}

	
//    protected void expectType(Predicate<Account> predicate) {
//    	if (getAccountType() != type) {
//    		throw new IllegalArgumentException(String.format("Expected account type %s, got %s", type.toString(), account.toString()));
//    	}
//    }


	public Instant getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(Instant createdTime) {
		this.createdTime = createdTime;
	}

	public Instant getClosedTime() {
		return closedTime;
	}

	public void setClosedTime(Instant closedTime) {
		this.closedTime = closedTime;
	}

	@Override
	public int hashCode() {
		return Objects.hash(reference);
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
		return Objects.equals(reference, other.reference);
	}

	@Override
	public String toString() {
		return String.format("Account [%s %s %s %s]", id, reference, accountType, DateTimeFormatter.ISO_INSTANT.format(createdTime));
	}
}
