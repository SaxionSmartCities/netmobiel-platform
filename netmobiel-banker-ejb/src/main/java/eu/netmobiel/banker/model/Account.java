package eu.netmobiel.banker.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;

import eu.netmobiel.banker.util.BankerUrnHelper;
import eu.netmobiel.banker.validator.IBANBankAccount;
import nl.garvelink.iban.IBAN;

/**
 * Formal record that represents, in words, money or other unit of measurement, certain resources, claims to such 
 * resources, transactions or other events that result in changes to those resources and claims. An account is 
 * externally identified by a unique accountNumber. In Netmobiel, we do not expose the account reference, as 
 * the entities owning an account have a one-to-one relation with an account. It is therefore sufficient to
 * address the higher-level entities outside the credit service (at REST level).  
 * 
 * @author Jaap Reitsma
 *
 */
@Entity
@Table(name = "account", uniqueConstraints = {
	    @UniqueConstraint(name = "cs_account_unique", columnNames = { "ncan" })
})
@Vetoed
@SequenceGenerator(name = "account_sg", sequenceName = "account_seq", allocationSize = 1, initialValue = 50)
public class Account implements Serializable {
	private static final long serialVersionUID = 2944298919351335815L;
	public static final String URN_PREFIX = BankerUrnHelper.createUrnPrefix(Account.class);
	public static final int ACCOUNT_NCAN_MAX_LENGTH = 32;
	public static final int ACCOUNT_NAME_MAX_LENGTH = 96;
	
	public static final Predicate<Account> isAsset = acc -> acc.getAccountType() == AccountType.ASSET;
	public static final Predicate<Account> isLiability = acc -> acc.getAccountType() == AccountType.LIABILITY;
	public static final Predicate<Account> isOpen = acc -> acc.getClosedTime() == null;

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_sg")
    private Long id;

    @Transient
    private String accountRef;
    
	/**
	 * An account has a NetMobiel Credit Account Number (NCAN). The account number could be seen as the external 
	 * identifier of the account, but in practise it is only used for lookup of the system accounts by well-known names.
	 * The accounts are not exposed to the outside world, so the NCAN is not used by users. 
	 */
	@Size(max = ACCOUNT_NCAN_MAX_LENGTH)
	@NotNull
	@Column(name = "ncan", nullable = false, length = ACCOUNT_NCAN_MAX_LENGTH)
    private String ncan;

	/**
     * The account display name. For asset accounts the accounting name, for personal accounts the name of the user.
     */
	@Size(max = ACCOUNT_NAME_MAX_LENGTH)
	@NotNull
    @Column(name = "name", length = ACCOUNT_NAME_MAX_LENGTH, nullable = false)
    private String name;

    /**
     * The account type.
     */
    @Column(name = "account_type", length = 1, nullable = false)
    private AccountType accountType;

    /**
     * Time of creation of the account.
     */
    @Column(name = "created_time", nullable = false, updatable = false)
    private Instant createdTime;

    /**
     * Time of closing of the account. A closed account cannot be used anymore. Before closing, the balance must be neutralized.
     * If null then the account is open.
     */
    @Column(name = "closed_time", nullable = true)
    private Instant closedTime;

    /**
     * The balances (one per ledger) coupled to the account.
     */
    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    private Set<Balance> balances;
    
    /**
     * Reference to the actual balance, i.e. the balance referring to the ledger currently in use.
     * Only present after certain calls, when the balance is explicitly queried.
     */
    @Transient
    private Balance actualBalance;
    
    @IBANBankAccount
	@Size(max = 48)
    @Column(name = "iban")
    private String iban;

	@Size(max = 96)
    @Column(name = "iban_holder")
    private String ibanHolder;

	public Account() {
    }

    public static Account newInstant(String aReference, String aName, AccountType aType) {
    	Account acc = new Account();
    	acc.ncan = aReference;
    	acc.name = aName;
    	acc.accountType = aType;
        return newInstant(aReference, aName, aType, null);
    }

    public static Account newInstant(String aReference, String aName, AccountType aType, Instant aCreatedTime) {
    	Account acc = new Account();
    	acc.ncan = aReference;
    	acc.name = aName;
    	acc.accountType = aType;
    	acc.createdTime = aCreatedTime;
        return acc;
    }

    @PrePersist
    void onPersist() {
    	if (this.createdTime == null) {
    		this.createdTime = Instant.now();
    	}
    }

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNcan() {
		return ncan;
	}

	public void setNcan(String ncan) {
		this.ncan = ncan;
	}

	public AccountType getAccountType() {
		return accountType;
	}

	public void setAccountType(AccountType accountType) {
		this.accountType = accountType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public Instant getClosedTime() {
		return closedTime;
	}

	public void setClosedTime(Instant closedTime) {
		this.closedTime = closedTime;
	}

	public Balance getActualBalance() {
		return actualBalance;
	}

	public void setActualBalance(Balance actualBalance) {
		this.actualBalance = actualBalance;
	}

	public Set<Balance> getBalances() {
		return balances;
	}

	public String getIban() {
		return iban;
	}

	public void setIban(String ibanValue) {
		if (ibanValue != null) {
			ibanValue = IBAN.toPretty(ibanValue);
		}
		this.iban = ibanValue;
	}

	public String getIbanHolder() {
		return ibanHolder;
	}

	public void setIbanHolder(String ibanHolder) {
		this.ibanHolder = ibanHolder;
	}

	@Override
	public int hashCode() {
		return Objects.hash(ncan);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Account)) {
			return false;
		}
		Account other = (Account) obj;
		return Objects.equals(ncan, other.ncan);
	}

    @Override
	public String toString() {
		return String.format("Account [%s %s %s %s]", id, StringUtils.abbreviate(ncan, 11), name, accountType);
	}

}
