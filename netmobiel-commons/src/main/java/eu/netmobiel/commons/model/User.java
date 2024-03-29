package eu.netmobiel.commons.model;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;

@MappedSuperclass
public abstract class User extends ReferableObject implements NetMobielUser {
	private static final long serialVersionUID = 9057079525058141265L;
	public static final int MAX_LENGTH_EMAIL = 64;
	@NotNull
    @NotEmpty
    @Size(max = 36)
    @Column(name = "managed_identity")
    private String managedIdentity;

    @Size(max = 32)
    @Column(name = "given_name")
	private String givenName;

    @Size(max = 64)
    @Column(name = "family_name")
	private String familyName;
	
    @Size(max = MAX_LENGTH_EMAIL)
    @Column(name = "email")
	private String email;

    protected User() {
    }
    
    protected User(String identity) {
    	this(identity, null, null, null);
    }
    
    protected User(NetMobielUser nbuser) {
    	this(nbuser.getManagedIdentity(), nbuser.getGivenName(), nbuser.getFamilyName(), nbuser.getEmail());
    }
    
    protected User(String identity, String givenName, String familyName, String email) {
    	this.managedIdentity = identity;
    	this.givenName = givenName;
    	this.familyName = familyName;
    	this.email = email;
    }
    
	@Override
	public abstract Long getId();

	public abstract void setId(Long id);

	public void from(NetMobielUser nmuser) {
		setEmail(nmuser.getEmail());
		setFamilyName(nmuser.getFamilyName());
		setGivenName(nmuser.getGivenName());
		setManagedIdentity(nmuser.getManagedIdentity());
	}

	@Override
	public String getManagedIdentity() {
		return managedIdentity;
	}

	public void setManagedIdentity(String managedIdentity) {
		this.managedIdentity = managedIdentity;
	}

	@Override
	public String getGivenName() {
		return givenName;
	}

	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}

	@Override
	public String getFamilyName() {
		return familyName;
	}

	public void setFamilyName(String familyName) {
		this.familyName = familyName;
	}

	@Override
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public String toString() {
		return String.format("%s [%s %s %s %s]", getClass().getSimpleName(), StringUtils.abbreviate(managedIdentity, 8), email, givenName, familyName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(managedIdentity);
	}

	@Override
	public boolean isSame(NetMobielUser other) {
		if (this == other) {
			return true;
		}
		if (other == null) {
			return false;
		}
		return  Objects.equals(getManagedIdentity(), other.getManagedIdentity()) &&
				Objects.equals(getEmail(), other.getEmail()) && 
				Objects.equals(getFamilyName(), other.getFamilyName()) && 
				Objects.equals(getGivenName(), other.getGivenName()) 
		;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof User)) {
			return false;
		}
		User other = (User) obj;
		return Objects.equals(managedIdentity, other.managedIdentity);
	}

}
