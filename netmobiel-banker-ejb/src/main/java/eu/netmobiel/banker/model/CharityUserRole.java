package eu.netmobiel.banker.model;

import java.io.Serializable;
import java.time.Instant;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "charity_user_role")
@Vetoed
public class CharityUserRole implements Serializable {
	private static final long serialVersionUID = 1865372319549877302L;

	/**
	 * The embedded primary key.
	 */
	@EmbeddedId
	private CharityUserRoleId id;

	/**
	 * Mapped reference to the charity.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("charityId")
	@JoinColumn(name = "charity", foreignKey = @ForeignKey(name = "charity_user_role_charity_fk"), nullable = false)
	private Charity charity;

	/**
	 * Mapped reference to the banker user having some (administrative) access right to the charity.
	 */
	// Never, never try to use an attribute 'user' as column name in Postgresql. 
	// You will get no warning what so ever that there is an issue with this reserved name.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "bn_user", foreignKey = @ForeignKey(name = "charity_user_role_user_fk"), nullable = false)
	@MapsId("userId")
	private BankerUser user;

	/**
     * Time of creation of the record.
     */
    @Column(name = "created_time", nullable = false, updatable = false)
    private Instant createdTime;

    /**
     * Time of modification of the record.
     */
    @Column(name = "modified_time", nullable = false)
    private Instant modifiedTime;

    /**
     * The access rights to the charity.
     */
    @Column(name = "role", length = 1, nullable = false)
    private CharityUserRoleType role;

    public CharityUserRole() {
    	
    }
    public CharityUserRole(Charity charity, BankerUser user) {
    	this.charity = charity;
    	this.user = user;
    	this.id = new CharityUserRoleId(charity.getId(), user.getId());
    }
    
    @PrePersist
    void onPersist() {
        this.createdTime = Instant.now();
        this.modifiedTime = this.createdTime;
    }

	public CharityUserRoleId getId() {
		return id;
	}
	public void setId(CharityUserRoleId id) {
		this.id = id;
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
	public Instant getModifiedTime() {
		return modifiedTime;
	}

	public void setModifiedTime(Instant modifiedTime) {
		this.modifiedTime = modifiedTime;
	}

	public CharityUserRoleType getRole() {
		return role;
	}

	public void setRole(CharityUserRoleType role) {
		this.role = role;
	}

	public Instant getCreatedTime() {
		return createdTime;
	}

	@Override
	public String toString() {
		return String.format("CharityUserRole [%s %s %s]", id.getCharityId(), id.getUserId(), role);
	}
 
    
}
