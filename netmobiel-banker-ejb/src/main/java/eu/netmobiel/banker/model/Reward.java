package eu.netmobiel.banker.model;

import java.time.Instant;

import javax.enterprise.inject.Vetoed;
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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import eu.netmobiel.banker.util.BankerUrnHelper;
import eu.netmobiel.commons.model.ReferableObject;

/**
 * A reward is the realization of an incentive. The actual credits involved are issued in a transaction.
 * So, it is possible to have a reward, but not have received the credits yet. 
 *   
 * @author Jaap Reitsma
 *
 */
@NamedEntityGraph(
		// Only users involved in a role can view the roles and the accounts.
		name = Reward.GRAPH_WITH_INCENTIVE, 
		attributeNodes = { 
			@NamedAttributeNode(value = "incentive"),		
		}
)

@Entity
@Table(name = "reward", uniqueConstraints = {
	    @UniqueConstraint(name = "cs_reward_unique", columnNames = { "incentive", "recipient" })
})
@Vetoed
@SequenceGenerator(name = "reward_sg", sequenceName = "reward_seq", allocationSize = 1, initialValue = 50)
public class Reward extends ReferableObject {
	private static final long serialVersionUID = 8541278830973217300L;

	public static final String URN_PREFIX = BankerUrnHelper.createUrnPrefix(Reward.class);
	public static final String GRAPH_WITH_INCENTIVE = "graph-with-incentive";

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reward_sg")
    private Long id;

    /**
     * The amount to receive.
     */
	@NotNull
    @PositiveOrZero
    @Column(name = "amount")
    private int amount;
    
    /**
     * The time of reward.
     */
	@NotNull
    @Column(name = "reward_time")
    private Instant rewardTime;

    /**
     * Reference to the incentive.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @JoinColumn(name = "incentive", foreignKey = @ForeignKey(name = "reward_incentive_fk"))
    private Incentive incentive;
    
    /**
     * Reference to the recipient of the reward.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @JoinColumn(name = "recipient", foreignKey = @ForeignKey(name = "reward_recipient_fk"))
    private BankerUser recipient;

    /**
     * Reference to the actual transaction of credits to the (premium) account of the recipient.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction", foreignKey = @ForeignKey(name = "reward_transaction_fk"))
    private AccountingTransaction transaction;

    /**
     * The URN of the fact that is the source of the reward being issued. For a survey it is the urn 
     * to the survey interaction of a user.
     */
	@Size(max = 32)
	@NotNull
    @Column(name = "fact_context")
    private String factContext;

    public Reward() {
    }

    /**
     * Constructor.
     */
    public Reward(Incentive anIncentive, BankerUser aRecipient, String fact, int anAmount) {
    	this.rewardTime = Instant.now();
    	this.incentive = anIncentive;
    	this.recipient = aRecipient;
    	this.amount = anAmount;
    	this.factContext = fact;
    }

    @Override
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public String getUrnPrefix() {
		return URN_PREFIX;
	}

	
	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}

	public Instant getRewardTime() {
		return rewardTime;
	}

	public void setRewardTime(Instant rewardTime) {
		this.rewardTime = rewardTime;
	}

	public Incentive getIncentive() {
		return incentive;
	}

	public void setIncentive(Incentive incentive) {
		this.incentive = incentive;
	}

	public BankerUser getRecipient() {
		return recipient;
	}

	public void setRecipient(BankerUser recipient) {
		this.recipient = recipient;
	}

	public AccountingTransaction getTransaction() {
		return transaction;
	}

	public void setTransaction(AccountingTransaction transaction) {
		this.transaction = transaction;
	}

	public String getFactContext() {
		return factContext;
	}

	public void setFactContext(String factContext) {
		this.factContext = factContext;
	}

	/**
	 * Using the database ID as equals test!
	 * @see https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/
	 */
	@Override
    public boolean equals(Object o) {
        if (this == o) {
        	return true;
        }
         if (!(o instanceof Reward)) {
            return false;
        }
         Reward other = (Reward) o;
        return id != null && id.equals(other.getId());
    }

	/**
	 * Using the database ID as equals test!
	 * @see https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/
	 */
    @Override
    public int hashCode() {
        return 31;
    }

	@Override
	public String toString() {
		return String.format("Reward [%s %s]", id, amount);
	}

}
