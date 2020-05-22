package eu.netmobiel.rideshare.model;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedEntityGraph;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.rideshare.util.RideshareUrnHelper;

@NamedEntityGraph()
@Entity
// You cannot have a table called 'user' in postgres, it is a reserved keyword
@Table(name = "rs_user", uniqueConstraints = {
	    @UniqueConstraint(name = "cs_managed_identity_unique", columnNames = { "managed_identity" })
})
@Vetoed
@SequenceGenerator(name = "user_sg", sequenceName = "user_id_seq", allocationSize = 1, initialValue = 50)
public class User implements NetMobielUser, Serializable {

	private static final long serialVersionUID = -4237705703151528786L;
	public static final String URN_PREFIX = RideshareUrnHelper.createUrnPrefix("user");
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_sg")
    private Long id;

    @NotNull
    @NotEmpty
    @Size(max = 36)
    @Column(name = "managed_identity")
    private String managedIdentity;

    @OneToMany(mappedBy = "driver")
    private List<Car> carsInUse;

    @Size(max = 4)
    @Column(name = "year_of_birth")
    private String yearOfBirth;

	@Column(length = 1)
    private Gender gender;

    @Column(name = "email", length = 64)
	private String email;
    
    @Column(name = "given_name", length = 32)
	private String givenName;
    
    @Column(name = "family_name", length = 64)
	private String familyName;

    public User() {
    	// No args constructor
    }
    
    public User(String identity, String givenName, String familyName) {
    	this.managedIdentity = identity;
    	this.givenName = givenName;
    	this.familyName = familyName;
    }
    
    /**
     * Copy constructor from general definition.
     * @param bu the basic user fields
     */
    public User(NetMobielUser bu) {
    	this.familyName = bu.getFamilyName();
    	this.givenName = bu.getGivenName();
    	this.managedIdentity = bu.getManagedIdentity();
    }

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getYearOfBirth() {
		return yearOfBirth;
	}

	public void setYearOfBirth(String yearOfBirth) {
		this.yearOfBirth = yearOfBirth;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public String getManagedIdentity() {
		return managedIdentity;
	}

	public void setManagedIdentity(String managedIdentity) {
		this.managedIdentity = managedIdentity;
	}

	public List<Car> getCarsInUse() {
		return carsInUse;
	}

	public void setCarsInUse(List<Car> carsInUse) {
		this.carsInUse = carsInUse;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
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
	@Override
	public String toString() {
		return String.format("User [%s %s]", managedIdentity, email);
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
