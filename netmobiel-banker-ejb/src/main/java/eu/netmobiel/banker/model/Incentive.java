package eu.netmobiel.banker.model;

import java.io.Serializable;
import java.util.Objects;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import eu.netmobiel.banker.util.BankerUrnHelper;
import eu.netmobiel.commons.model.ReferableObject;

/**
 * Definition of an incentive.
 *  
 * @author Jaap Reitsma
 *
 */
@Entity
@Table(name = "incentive", uniqueConstraints = {
	    @UniqueConstraint(name = "cs_incentive_code_unique", columnNames = { "code" })
})
@Vetoed
@SequenceGenerator(name = "incentive_sg", sequenceName = "incentive_seq", allocationSize = 1, initialValue = 50)
public class Incentive extends ReferableObject implements Serializable {
	
	private static final long serialVersionUID = 1285808506461012273L;
	public static final String URN_PREFIX = BankerUrnHelper.createUrnPrefix(Incentive.class);

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "incentive_sg")
    private Long id;

	/**
	 * An incentive has a short code as (internal) reference to an incentive. The code is direct or indirect  
	 * attached to the trigger object. Example code: 'survey-0' to indicate the first survey offered. 
	 */
	@Size(max = 16)
	@NotNull
	@Column(name = "code")
    private String code;

	/**
	 * An incentive has a category, just to make up some sensible text in the reward or subsequent transaction. 
	 * It is not an enumerated type, because the categories are opaque for the banker service.
	 */
	@Size(max = 16)
	@NotNull
	@Column(name = "category")
    private String category;

	/**
     * The incentive description, for use in the GUI.
     */
	@Size(max = 256)
	@NotNull
    @Column(name = "description")
    private String description;

    /**
     * The amount to reward.
     */
    @Column(name = "amount")
    @NotNull
    @PositiveOrZero
    private int amount;

    /**
     * Is this incentive amount the absolute value of the reward or a relative percentage value?
     * The value is floored after calculation.
     */
    @Column(name = "relative")
    @NotNull
    private boolean relative;

    /**
     * Is the incentive intended as a payment of premium credits or is it a redemption of premium credits?
     */
    @Column(name = "redemption")
    @NotNull
    private boolean redemption;
    
    /**
     * If set, the maximum amount to pay in any case.
     */
    @Column(name = "max_amount")
    @PositiveOrZero
    private Integer maxAmount;

	@Override
	public String getUrnPrefix() {
		return URN_PREFIX;
	}

    public Incentive() {
		// Constructor
    }

    @Override
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

    public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}

	public boolean isRelative() {
		return relative;
	}

	public void setRelative(boolean relative) {
		this.relative = relative;
	}

	public boolean isRedemption() {
		return redemption;
	}

	public void setRedemption(boolean redemption) {
		this.redemption = redemption;
	}

	public Integer getMaxAmount() {
		return maxAmount;
	}

	public void setMaxAmount(Integer maxAmount) {
		this.maxAmount = maxAmount;
	}

	/**
	 * Calculate the amount to reward, given a yield (that might be null).
	 * Only relative rewards are calculated from the yield.
	 * @param turnover
	 * @return
	 */
	public int calculateAmountToReward(Integer yield) {
		int reward = getAmount();
		if (isRelative()) {
			if (yield == null) {
				throw new IllegalStateException("Specify the yield for a relative incentive!");
			}
			reward = Math.floorDiv(getAmount() * yield, 100);
		}
		if (getMaxAmount() != null) {
			// Cap the reward, if necessary
			reward = Math.min(getMaxAmount(), reward);
		}
		return reward;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(code);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Incentive)) {
			return false;
		}
		Incentive other = (Incentive) obj;
		return Objects.equals(code, other.code);
	}

	@Override
	public String toString() {
		return String.format("Incentive [%s %s %d]", id, code, amount);
	}

}
