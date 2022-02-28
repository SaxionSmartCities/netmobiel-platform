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
public class Incentive implements Serializable {
	
	private static final long serialVersionUID = 1285808506461012273L;

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

    public Incentive() {
		// Constructor
    }

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
