package eu.netmobiel.banker.model;

import java.io.Serializable;
import java.time.format.DateTimeFormatter;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
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
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedSubgraph;
import javax.persistence.SequenceGenerator;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import eu.netmobiel.banker.util.BankerUrnHelper;
import eu.netmobiel.commons.report.NumericReportValue;

/**
 * An accounting (or journal) entry has an amount and an description and concerns a specific account. An entry is processed as part of a
 * particular transaction. In each transaction an account can only appear once.
 * 
 * @author Jaap Reitsma
 *
 */

@NamedNativeQueries({
	@NamedNativeQuery(
			name = AccountingEntry.IMP_1_EARNED_CREDITS,
			query = "select u.managed_identity as managed_identity, "
	        		+ "date_part('year', t.transaction_time) as year, " 
	        		+ "date_part('month', t.transaction_time) as month, "
	        		+ "sum(e.amount) as count "
	        		+ "from accounting_entry e "
	        		+ "join accounting_transaction t on t.id = e.transaction "
	        		+ "join account a on a.id = e.account "
	        		+ "join bn_user u on u.personal_account = a.id "
	        		+ "where t.transaction_time >= ? and t.transaction_time < ? and t.transaction_type = 'PY' "
	        		+ " and e.entry_type = 'C' "
	        		+ "group by u.managed_identity, year, month "
	        		+ "order by u.managed_identity, year, month",
	        resultSetMapping = AccountingEntry.BN_ACC_ENTRY_USER_YEAR_MONTH_COUNT_MAPPING),
	@NamedNativeQuery(
			name = AccountingEntry.IMP_3_SPENT_CREDITS,
			query = "select u.managed_identity as managed_identity, "
	        		+ "date_part('year', t.transaction_time) as year, " 
	        		+ "date_part('month', t.transaction_time) as month, "
	        		+ "sum(e.amount) as count "
	        		+ "from accounting_entry e "
	        		+ "join accounting_transaction t on t.id = e.transaction "
	        		+ "join account a on a.id = e.account "
	        		+ "join bn_user u on u.personal_account = a.id "
	        		+ "where t.transaction_time >= ? and t.transaction_time < ? and t.transaction_type = 'PY' "
	        		+ " and e.entry_type = 'D' "
	        		+ "group by u.managed_identity, year, month "
	        		+ "order by u.managed_identity, year, month",
	        resultSetMapping = AccountingEntry.BN_ACC_ENTRY_USER_YEAR_MONTH_COUNT_MAPPING),
	@NamedNativeQuery(
			name = AccountingEntry.IMP_4_SPENT_CREDITS_TRAVELLING,
			query = "select u.managed_identity as managed_identity, "
	        		+ "date_part('year', t.transaction_time) as year, " 
	        		+ "date_part('month', t.transaction_time) as month, "
	        		+ "sum(e.amount) as count "
	        		+ "from accounting_entry e "
	        		+ "join accounting_transaction t on t.id = e.transaction "
	        		+ "join account a on a.id = e.account "
	        		+ "join bn_user u on u.personal_account = a.id "
	        		+ "where t.transaction_time >= ? and t.transaction_time < ? and t.transaction_type = 'PY' "
	        		+ " and e.entry_type = 'D' and t.context like 'urn:nb:pn:leg:%' "
	        		+ "group by u.managed_identity, year, month "
	        		+ "order by u.managed_identity, year, month",
	        resultSetMapping = AccountingEntry.BN_ACC_ENTRY_USER_YEAR_MONTH_COUNT_MAPPING),
	@NamedNativeQuery(
			name = AccountingEntry.IMP_5_SPENT_CREDITS_CHARITIES,
			query = "select u.managed_identity as managed_identity, "
	        		+ "date_part('year', t.transaction_time) as year, " 
	        		+ "date_part('month', t.transaction_time) as month, "
	        		+ "sum(e.amount) as count "
	        		+ "from accounting_entry e "
	        		+ "join accounting_transaction t on t.id = e.transaction "
	        		+ "join account a on a.id = e.account "
	        		+ "join bn_user u on u.personal_account = a.id "
	        		+ "where t.transaction_time >= ? and t.transaction_time < ? and t.transaction_type = 'PY' "
	        		+ " and e.entry_type = 'D' and t.context like 'urn:nb:bn:donation:%' "
	        		+ "group by u.managed_identity, year, month "
	        		+ "order by u.managed_identity, year, month",
	        resultSetMapping = AccountingEntry.BN_ACC_ENTRY_USER_YEAR_MONTH_COUNT_MAPPING),
	@NamedNativeQuery(
		name = AccountingEntry.IMP_7_DEPOSITED_CREDITS,
		query = "select u.managed_identity as managed_identity, "
        		+ "date_part('year', t.transaction_time) as year, " 
        		+ "date_part('month', t.transaction_time) as month, "
        		+ "sum(e.amount) as count "
        		+ "from accounting_entry e "
        		+ "join accounting_transaction t on t.id = e.transaction "
        		+ "join account a on a.id = e.account "
        		+ "join bn_user u on u.personal_account = a.id "
        		+ "where t.transaction_time >= ? and t.transaction_time < ? and t.transaction_type = 'DP' "
        		+ "group by u.managed_identity, year, month "
        		+ "order by u.managed_identity, year, month",
        resultSetMapping = AccountingEntry.BN_ACC_ENTRY_USER_YEAR_MONTH_COUNT_MAPPING),
	@NamedNativeQuery(
			name = AccountingEntry.IMP_8_WITHDRAWN_CREDITS,
			query = "select u.managed_identity as managed_identity, "
	        		+ "date_part('year', t.transaction_time) as year, " 
	        		+ "date_part('month', t.transaction_time) as month, "
	        		+ "sum(e.amount) as count "
	        		+ "from accounting_entry e "
	        		+ "join accounting_transaction t on t.id = e.transaction "
	        		+ "join account a on a.id = e.account "
	        		+ "join bn_user u on u.personal_account = a.id "
	        		+ "where t.transaction_time >= ? and t.transaction_time < ? and t.transaction_type = 'WD' "
	        		+ "group by u.managed_identity, year, month "
	        		+ "order by u.managed_identity, year, month",
	        resultSetMapping = AccountingEntry.BN_ACC_ENTRY_USER_YEAR_MONTH_COUNT_MAPPING),
	@NamedNativeQuery(
			name = AccountingEntry.IMC_2_EARNED_CREDITS_BY_RIDES,
			query = "select u.managed_identity as managed_identity, "
	        		+ "date_part('year', t.transaction_time) as year, " 
	        		+ "date_part('month', t.transaction_time) as month, "
	        		+ "sum(e.amount) as count "
	        		+ "from accounting_entry e "
	        		+ "join accounting_transaction t on t.id = e.transaction "
	        		+ "join account a on a.id = e.account "
	        		+ "join bn_user u on u.personal_account = a.id "
	        		+ "where t.transaction_time >= ? and t.transaction_time < ? and t.transaction_type = 'PY' "
	        		+ " and e.entry_type = 'C' and t.context like 'urn:nb:pn:leg:%' "
	        		+ "group by u.managed_identity, year, month "
	        		+ "order by u.managed_identity, year, month",
	        resultSetMapping = AccountingEntry.BN_ACC_ENTRY_USER_YEAR_MONTH_COUNT_MAPPING),
})
@SqlResultSetMappings({
	@SqlResultSetMapping(
			name = AccountingEntry.BN_ACC_ENTRY_USER_YEAR_MONTH_COUNT_MAPPING, 
			classes = @ConstructorResult(
				targetClass = NumericReportValue.class, 
				columns = {
						@ColumnResult(name = "managed_identity", type = String.class),
						@ColumnResult(name = "year", type = int.class),
						@ColumnResult(name = "month", type = int.class),
						@ColumnResult(name = "count", type = int.class)
				}
			)
		),
})
@NamedEntityGraph(
		// Only users involved in a role can view the roles and the accounts.
		name = AccountingEntry.STATEMENT_ENTITY_GRAPH, 
		attributeNodes = { 
			@NamedAttributeNode(value = "transaction", subgraph = "subgraph.transaction"),		
			@NamedAttributeNode(value = "counterparty")
		}, subgraphs = {
				@NamedSubgraph(
						name = "subgraph.transaction",
						attributeNodes = {
								@NamedAttributeNode(value = "ledger")
						}
				)
		}
)

@Entity
@Table(name = "accounting_entry", uniqueConstraints = {
	    @UniqueConstraint(name = "cs_transaction_account_unique", columnNames = { "account", "transaction" })
})
@Vetoed
@SequenceGenerator(name = "accounting_entry_sg", sequenceName = "accounting_entry_seq", allocationSize = 1, initialValue = 50)
public class AccountingEntry implements Serializable {
	private static final long serialVersionUID = -9042884607740676891L;
	public static final String URN_PREFIX = BankerUrnHelper.createUrnPrefix("statement");

	public static final String BN_ACC_ENTRY_USER_YEAR_MONTH_COUNT_MAPPING = "BNAccEntryUserYearMonthCountMapping";
	public static final String IMP_1_EARNED_CREDITS = "ListEarnedCredits";
	public static final String IMP_2_EARNED_CREDITS_BY_APP_USAGE = "ListEarnedCreditsByAppUsage";
	public static final String IMP_3_SPENT_CREDITS = "ListSpentCredits";
	public static final String IMP_4_SPENT_CREDITS_TRAVELLING = "ListSpentCreditsForTravelling";
	public static final String IMP_5_SPENT_CREDITS_CHARITIES = "ListSpentCreditsForCharities";
	public static final String IMP_6_SPENT_CREDITS_REWARDS = "ListSpentCreditsForRewards";
	public static final String IMP_7_DEPOSITED_CREDITS = "ListDepositedCredits";
	public static final String IMP_8_WITHDRAWN_CREDITS = "ListWithdrawnCredits";
	public static final String IMP_9_TRIPS_REVIEWED_COUNT = "ListReviewedTripsCount";

	public static final String IMC_1_EARNED_CREDITS = IMP_1_EARNED_CREDITS;
	public static final String IMC_2_EARNED_CREDITS_BY_RIDES = "ListEarnedCreditsByRides";
	public static final String IMC_3_EARNED_CREDITS_BY_APP_USAGE = IMP_2_EARNED_CREDITS_BY_APP_USAGE;
	public static final String IMC_4_SPENT_CREDITS = IMP_3_SPENT_CREDITS;
	public static final String IMC_5_SPENT_CREDITS_TRAVELLING = IMP_4_SPENT_CREDITS_TRAVELLING;
	public static final String IMC_6_SPENT_CREDITS_CHARITIES = IMP_5_SPENT_CREDITS_CHARITIES;
	public static final String IMC_7_SPENT_CREDITS_REWARDS = IMP_6_SPENT_CREDITS_REWARDS;
	public static final String IMC_8_DEPOSITED_CREDITS = IMP_7_DEPOSITED_CREDITS;
	public static final String IMC_9_WITHDRAWN_CREDITS = IMP_8_WITHDRAWN_CREDITS;
	public static final String IMC_10_RIDES_REVIEWED_COUNT = "ListReviewedRidesCount";

	public static final String STATEMENT_ENTITY_GRAPH = "accounting_entry-statement-graph";

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accounting_entry_sg")
    private Long id;

	/**
	 * The account involved in the entry. Once saved in the database it cannot be changed.
	 */
	@NotNull
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "account", foreignKey = @ForeignKey(name = "accounting_entry_account_fk"))
    private Account account;

    /**
     * The counterparty in any transfer.
     * FIXME This attribute does not belong in the entry. The other entry (or entries) in a transaction are the counterparty.
     * It is now included only as a convenience for generating statements. From a modelling view, it is not correct.
     */
	@NotNull
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "counterparty", foreignKey = @ForeignKey(name = "accounting_entry_counterparty_fk"))
    private Account counterparty;

	/**
	 * The amount (of credits). Once saved in the database it cannot be changed.
	 */
	@NotNull
	@Column(name = "amount")
    private int amount;

    /**
     * The accounting entry type.
     */
	@NotNull
    @Column(name = "entry_type", length = 1)
    private AccountingEntryType entryType;

    /**
     * The transaction this entry belongs to.
     */
    @NotNull
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "transaction", foreignKey = @ForeignKey(name = "accounting_entry_transaction_fk"))
    private AccountingTransaction transaction;
	
    /**
	 * The purpose of the transaction.
	 */
    @Column(name = "purpose", nullable = false, length = 2)
    private TransactionType purpose;
    
    public AccountingEntry() {
    	
    }
    
    public AccountingEntry(AccountingEntryType entryType, int amount, TransactionType purpose) {
        assert amount != 0 : "Amount of accounting entry must be nonzero";
        this.entryType = entryType;
        this.amount = amount;
        this.purpose = purpose;
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

	public Account getCounterparty() {
		return counterparty;
	}

	public void setCounterparty(Account counterparty) {
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

	public TransactionType getPurpose() {
		return purpose;
	}

	public void setPurpose(TransactionType purpose) {
		this.purpose = purpose;
	}

	@Override
	public String toString() {
		return String.format("AccountingEntry [%s, %s, %s, %s, %s, '%s', %s ]", id,
				account.getNcan(), amount, entryType, purpose, transaction.getDescription(), 
				DateTimeFormatter.ISO_INSTANT.format(transaction.getTransactionTime()));
	}
    
}
