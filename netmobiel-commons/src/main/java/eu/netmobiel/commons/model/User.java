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

	@NotNull
    @NotEmpty
    @Size(max = 36)
    @Column(name = "managed_identity")
    private String managedIdentity;

    @Column(name = "given_name", length = 32)
	private String givenName;
    
    @Column(name = "family_name", length = 64)
	private String familyName;
	
    @Size(max = 64)
    @Column(name = "email", length = 64)
	private String email;

    public User() {
    }
    
    public User(String identity) {
    	this(identity, null, null, null);
    }
    
    public User(NetMobielUser nbuser) {
    	this(nbuser.getManagedIdentity(), nbuser.getGivenName(), nbuser.getFamilyName(), nbuser.getEmail());
    }
    
    public User(String identity, String givenName, String familyName, String email) {
    	this.managedIdentity = identity;
    	this.givenName = givenName;
    	this.familyName = familyName;
    	this.email = email;
    }
    
	public abstract Long getId();

	public abstract void setId(Long id);

	public void from(NetMobielUser nmuser) {
		setEmail(nmuser.getEmail());
		setFamilyName(nmuser.getFamilyName());
		setGivenName(nmuser.getGivenName());
		setManagedIdentity(nmuser.getManagedIdentity());
	}

	public String getManagedIdentity() {
		return managedIdentity;
	}

	public void setManagedIdentity(String managedIdentity) {
		this.managedIdentity = managedIdentity;
	}

	public String getGivenName() {
		return givenName;
	}

	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}

	public String getFamilyName() {
		return familyName;
	}

	public void setFamilyName(String familyName) {
		this.familyName = familyName;
	}

	public String getName() {
		StringBuilder sb = new StringBuilder();
		if (getGivenName() != null) {
			sb.append(getGivenName()).append(" ");
		}
		if (getFamilyName() != null) {
			sb.append(getFamilyName());
		}
		String name = sb.toString().trim();
		return name.length() > 0 ? name : null;
	}

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
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		User other = (User) obj;
		return Objects.equals(managedIdentity, other.managedIdentity);
	}
    
}
