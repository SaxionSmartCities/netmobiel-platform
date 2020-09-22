package eu.netmobiel.banker.model;

import java.io.Serializable;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
@Access(AccessType.FIELD)	// Add this annotation, otherwise no JPA ModelGen attribute is generated.
public class CharityUserRoleId implements Serializable {
	private static final long serialVersionUID = 6457485948050543420L;

	/**
	 * The charity PK.
	 */
	@Column(name = "charity_id", nullable = false)
	private Long charityId;

	/**
	 * The banker user PK.
	 */
	@Column(name = "user_id", nullable = false)
	private Long userId;

	public CharityUserRoleId() {
	}

	public CharityUserRoleId(Long aCharity, Long aUser) { 
		this.charityId = aCharity;
		this.userId = aUser;
	}
	
	public Long getCharityId() {
		return charityId;
	}

	public void setCharityId(Long charityId) {
		this.charityId = charityId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((charityId == null) ? 0 : charityId.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof CharityUserRoleId)) {
			return false;
		}
		CharityUserRoleId other = (CharityUserRoleId) obj;
		if (charityId == null) {
			if (other.charityId != null) {
				return false;
			}
		} else if (!charityId.equals(other.charityId)) {
			return false;
		}
		if (userId == null) {
			if (other.userId != null) {
				return false;
			}
		} else if (!userId.equals(other.userId)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return String.format("CharityUserRoleId [%s %s]", charityId, userId);
	}
	
}
