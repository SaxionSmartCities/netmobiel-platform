package eu.netmobiel.banker.model;

import java.beans.Transient;
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
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import eu.netmobiel.banker.util.BankerUrnHelper;
import eu.netmobiel.commons.model.ReferableObject;
import eu.netmobiel.commons.util.UrnHelper;

/**
 * A Donation is a gift from a user to a charity. The donation acts as an order for the ledger service.
 * The Donation class is used for projections as well, some fields are omitted in that case and the count field
 * represents a frequency of charities or users, depending on the question. 
 *   
 * @author Jaap Reitsma
 *
 */
@NamedEntityGraphs({
	@NamedEntityGraph(
			name = Donation.CHARITY_GRAPH, 
			attributeNodes = { 
					@NamedAttributeNode(value = "charity")		
			}
	),
	@NamedEntityGraph(
			name = Donation.USER_GRAPH, 
			attributeNodes = { 
					@NamedAttributeNode(value = "user")		
			}
	),
	@NamedEntityGraph(
			name = Donation.CHARITY_USER_GRAPH, 
			attributeNodes = { 
					@NamedAttributeNode(value = "charity"),		
					@NamedAttributeNode(value = "user")		
			}
	),
})
@Entity
@Table(name = "donation")
@Vetoed
@SequenceGenerator(name = "donation_sg", sequenceName = "donation_seq", allocationSize = 1, initialValue = 50)
public class Donation extends ReferableObject {
	private static final long serialVersionUID = 4014950654313924539L;
	public static final String URN_PREFIX = BankerUrnHelper.createUrnPrefix(Donation.class);
	public static final int DONATION_DESCRIPTION_MAX_LENGTH = 256;
	public static final String CHARITY_USER_GRAPH = "donations-with-charity-and-user-graph";
	public static final String CHARITY_GRAPH = "donations-with-charity";
	public static final String USER_GRAPH = "donations-with-user";

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "donation_sg")
    private Long id;

	/**
     * The donation description.
     */
	@Size(max = DONATION_DESCRIPTION_MAX_LENGTH)
    @Column(name = "description", length = DONATION_DESCRIPTION_MAX_LENGTH)
    private String description;

    /**
     * The donated amount. 
     */
    @PositiveOrZero
    @Column(name = "amount", nullable = false)
    private Integer amount;
    
    /**
     * Reference to the account of the charity. This is a one to one relation.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charity", nullable = false, foreignKey = @ForeignKey(name = "donation_charity_fk"))
    private Charity charity;
    
    /**
     * Reference to the account of the charity. This is a one to one relation.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bn_user", nullable = false, foreignKey = @ForeignKey(name = "donation_user_fk"))
    private BankerUser user;

    /**
     * The time of donation.
     */
    @Column(name = "donation_time", nullable = false)
    private Instant donationTime;

    /**
     * If true then the donor prefers to stay anonymous. The donor will never appear in the top-N lists with this donation.
     */
    @Column(name = "anonymous", nullable = false)
    private boolean anonymous;

    public Donation() {
    }

    /**
     * Constructor for the top-N donor generousity report.
     * @param charity
     * @param count
     */
    public Donation(BankerUser user, Long totalAmount) {
    	this.user = user;
    	this.amount = totalAmount.intValue();
    }

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

	@Transient
	public String getCharityRef() {
		return getCharity() != null ? UrnHelper.createUrn(Charity.URN_PREFIX, getCharity().getId()) : null;
	}

	@Transient
	public String getDonorRef() {
		return getUser() != null ? UrnHelper.createUrn(BankerUser.URN_PREFIX, getUser().getId()) : null;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	
    public Integer getAmount() {
		return amount;
	}

	public void setAmount(Integer amount) {
		this.amount = amount;
	}

	public Charity getCharity() {
		return charity;
	}

	public void setCharity(Charity charity) {
		this.charity = charity;
	}

	public BankerUser getUser() {
		return user;
	}

	public void setUser(BankerUser user) {
		this.user = user;
	}

	public Instant getDonationTime() {
		return donationTime;
	}

	public void setDonationTime(Instant donationTime) {
		this.donationTime = donationTime;
	}

	public boolean isAnonymous() {
		return anonymous;
	}

	public void setAnonymous(boolean anonymous) {
		this.anonymous = anonymous;
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
         if (!(o instanceof Donation)) {
            return false;
        }
         Donation other = (Donation) o;
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
		return String.format("Donation [%s %s]", id, amount);
	}

}
