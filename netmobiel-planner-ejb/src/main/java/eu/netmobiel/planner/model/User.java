package eu.netmobiel.planner.model;

import java.io.Serializable;
import java.util.Objects;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedEntityGraph;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.planner.util.PlannerUrnHelper;

@NamedEntityGraph()
@Entity
// You cannot have a table called 'user' in postgres, it is a reserved keyword
@Table(name = "pl_user", uniqueConstraints = {
	    @UniqueConstraint(name = "cs_managed_identity_unique", columnNames = { "managed_identity" })
})
@Vetoed
@SequenceGenerator(name = "user_sg", sequenceName = "user_id_seq", allocationSize = 1, initialValue = 50)
public class User implements NetMobielUser, Serializable {

	private static final long serialVersionUID = -4237705703151528786L;
	public static final String URN_PREFIX = PlannerUrnHelper.createUrnPrefix("user");
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_sg")
    private Long id;

    @Transient
    private String userRef;

    @NotNull
    @NotEmpty
    @Size(max = 36)
    @Column(name = "managed_identity", length = 36)
    private String managedIdentity;

    @Size(max = 32)
    @Column(name = "given_name", length = 32)
	private String givenName;
    
    @Size(max = 64)
    @Column(name = "family_name", length = 64)
	private String familyName;

    @Size(max = 64)
    @Column(name = "email", length = 64)
	private String email;
    
    public User() {
    	
    }
    
    public User(NetMobielUser nbuser) {
    	this(nbuser.getManagedIdentity(), nbuser.getGivenName(), nbuser.getFamilyName(), nbuser.getEmail());
    }

    public User(String identity, String givenName, String familyName) {
    	this(identity, givenName, familyName, null);
    }
    
    public User(String identity, String givenName, String familyName, String email) {
    	this.managedIdentity = identity;
    	this.givenName = givenName;
    	this.familyName = familyName;
    	this.email = email;
    }
    
	public String getUserRef() {
    	if (userRef == null) {
    		userRef = PlannerUrnHelper.createUrn(URN_PREFIX, getId());
    	}
		return userRef;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	@Override
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public String toString() {
		return String.format("User [%s %s %s]", managedIdentity, givenName, familyName);
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
