package eu.netmobiel.banker.model;

import java.io.Serializable;
import java.util.Objects;

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
import javax.persistence.NamedSubgraph;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import eu.netmobiel.banker.util.BankerUrnHelper;
import eu.netmobiel.commons.model.NetMobielUser;

@NamedEntityGraphs({
	@NamedEntityGraph(
			name = User.GRAPH_WITH_BALANCE, 
			attributeNodes = { 
					@NamedAttributeNode(value = "personalAccount", subgraph = "subgraph.account")
			}, subgraphs = {
					@NamedSubgraph(
							name = "subgraph.account",
							attributeNodes = {
									@NamedAttributeNode(value = "actualBalance")
							}
					)
			}
	)
})
@Entity
// You cannot have a table called 'user' in postgres, it is a reserved keyword
@Table(name = "bn_user", uniqueConstraints = {
	    @UniqueConstraint(name = "cs_managed_identity_unique", columnNames = { "managed_identity" }),
	    @UniqueConstraint(name = "cs_personal_account_unique", columnNames = { "personal_account" })
})
@Vetoed
@SequenceGenerator(name = "user_sg", sequenceName = "user_id_seq", allocationSize = 1, initialValue = 50)
public class User implements NetMobielUser, Serializable {

	private static final long serialVersionUID = -4237705703151528786L;
	public static final String URN_PREFIX = BankerUrnHelper.createUrnPrefix(User.class);
	public static final String GRAPH_WITH_BALANCE = "user-graph-with-balance";
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_sg")
    private Long id;

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

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "personal_account", foreignKey = @ForeignKey(name = "user_personal_account_fk"))
    private Account personalAccount;
    
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


	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Account getPersonalAccount() {
		return personalAccount;
	}

	public void setPersonalAccount(Account personalAccount) {
		this.personalAccount = personalAccount;
	}

	public String createAccountName() {
		return String.format("%s %s", getGivenName() != null ? getGivenName() : "", getFamilyName() != null ? getFamilyName() : "").trim();
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
