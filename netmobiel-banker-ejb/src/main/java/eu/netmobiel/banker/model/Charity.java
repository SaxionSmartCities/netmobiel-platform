package eu.netmobiel.banker.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;

import eu.netmobiel.banker.util.BankerUrnHelper;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.ReferableObject;

/**
 * Charity in the banker: A noble purpose to donate money to.
 *   
 * @author Jaap Reitsma
 *
 */
@Entity
@Table(name = "charity", uniqueConstraints = {
	    @UniqueConstraint(name = "cs_charity_account_unique", columnNames = { "account" })
})
@Vetoed
@SequenceGenerator(name = "charity_sg", sequenceName = "charity_seq", allocationSize = 1, initialValue = 50)
public class Charity extends ReferableObject {
	private static final long serialVersionUID = 4014950654313924539L;
	public static final String URN_PREFIX = BankerUrnHelper.createUrnPrefix(Charity.class);
	public static final int CHARITY_DESCRIPTION_MAX_LENGTH = 256;
	public static final int CHARITY_PICTURE_URL_MAX_LENGTH = 256;
	

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "charity_sg")
    private Long id;

	/**
     * The charity description.
     */
	@Size(max = CHARITY_DESCRIPTION_MAX_LENGTH)
    @Column(name = "description", length = CHARITY_DESCRIPTION_MAX_LENGTH)
    private String description;

	/**
     * The charity description.
     */
	@Size(max = CHARITY_PICTURE_URL_MAX_LENGTH)
    @Column(name = "pictureUrl", length = CHARITY_PICTURE_URL_MAX_LENGTH)
    private String pictureUrl;

	/**
	 * Place of the charity.
	 */
    @Embedded
    private GeoLocation location;

    /**
     * The donation goal of this charity.
     */
    @PositiveOrZero
    @Column(name = "goal_amount")
    private int goalAmount;

    /**
     * The total sum of all donations. Should be in the end the sum of all withdrawals from the charity account. 
     */
    @PositiveOrZero
    @Column(name = "donated_amount")
    private int donatedAmount;
    
    /**
     * Reference to the account of the charity. This is a one to one relation.
     */
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account", foreignKey = @ForeignKey(name = "charity_account_fk"))
    private Account account;
    
    /**
     * The roles having administrative access to the charity.
     */
	@OneToMany(mappedBy = "charity", fetch = FetchType.LAZY)
	private List<CharityUserRole> roles = new ArrayList<>();

    public Charity() {
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
    public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getPictureUrl() {
		return pictureUrl;
	}

	public void setPictureUrl(String pictureUrl) {
		this.pictureUrl = pictureUrl;
	}

	public GeoLocation getLocation() {
		return location;
	}

	public void setLocation(GeoLocation location) {
		this.location = location;
	}

	public int getGoalAmount() {
		return goalAmount;
	}

	public void setGoalAmount(int goalAmount) {
		this.goalAmount = goalAmount;
	}

	public int getDonatedAmount() {
		return donatedAmount;
	}

	public void setDonatedAmount(int donatedAmount) {
		this.donatedAmount = donatedAmount;
	}

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	public List<CharityUserRole> getRoles() {
		return roles;
	}

	public void setRoles(List<CharityUserRole> roles) {
		this.roles = roles;
	}

	public void addUserRole(BankerUser user, CharityUserRoleType role) {
		CharityUserRole cur = new CharityUserRole(this, user);
        roles.add(cur);
    }
 
    public void removeUserRole(BankerUser user) {
        for (Iterator<CharityUserRole> ix = roles.iterator(); ix.hasNext(); ) {
        	CharityUserRole cur = ix.next();
            if (cur.getCharity().equals(this) && cur.getUser().equals(user)) {
                ix.remove();
                cur.setCharity(null);
                cur.setUser(null);
            }
        }
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
         if (!(o instanceof Charity)) {
            return false;
        }
         Charity other = (Charity) o;
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
		return String.format("Charity [%s %s]", id, StringUtils.abbreviate(account.getName(), 15));
	}

}
