package eu.netmobiel.banker.model;

import javax.enterprise.inject.Vetoed;
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
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import eu.netmobiel.banker.util.BankerUrnHelper;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.User;

@NamedEntityGraphs({
	@NamedEntityGraph(
			name = BankerUser.GRAPH_WITH_ACCOUNT, 
			attributeNodes = { 
					@NamedAttributeNode(value = "managedIdentity"),
					@NamedAttributeNode(value = "personalAccount", subgraph = "subgraph.account"),
					@NamedAttributeNode(value = "premiumAccount", subgraph = "subgraph.account")
			}, subgraphs = {
					@NamedSubgraph(
							name = "subgraph.account",
							attributeNodes = {
									@NamedAttributeNode(value = "ncan")
							}
					)
			}
	),
	@NamedEntityGraph(
			includeAllAttributes = false,
			name = BankerUser.GRAPH_WITHOUT_ACCOUNT, 
			attributeNodes = { 
					@NamedAttributeNode(value = "managedIdentity")
			}
	)

})
@Entity
// You cannot have a table called 'user' in postgres, it is a reserved keyword
@Table(name = "bn_user", uniqueConstraints = {
	    @UniqueConstraint(name = "cs_managed_identity_unique", columnNames = { "managed_identity" }),
	    @UniqueConstraint(name = "cs_personal_account_unique", columnNames = { "personal_account" }),
	    @UniqueConstraint(name = "cs_premium_account_unique", columnNames = { "premium_account" })
})
@Vetoed
@SequenceGenerator(name = "user_sg", sequenceName = "user_id_seq", allocationSize = 1, initialValue = 50)
public class BankerUser extends User {
	private static final long serialVersionUID = -4237705703151528786L;
	public static final String URN_PREFIX = BankerUrnHelper.createUrnPrefix("user");
	public static final String GRAPH_WITH_ACCOUNT = "user-graph-with-account";
	public static final String GRAPH_WITHOUT_ACCOUNT = "user-graph-without-account";
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_sg")
    private Long id;

	/**
	 * The personal account of a user. The foreign key definition avoids issues with testing because of the sequence of deleting tables.
	 * This is the running or current account.
	 */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_account", 
    		    foreignKey = @ForeignKey(name = "user_personal_account_fk",
    		    foreignKeyDefinition = "FOREIGN KEY (personal_account) REFERENCES account (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE SET NULL")
	)
    private Account personalAccount;

	/**
	 * The premium account of a user. The foreign key definition avoids issues with testing because of the sequence of deleting tables.
	 * The premium account tracks the incentive-based transactions. Premium credits can be spend to selected goods and services or are
	 * transferred to the current account as a reward for certain behaviour..  
	 */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "premium_account", 
    		    foreignKey = @ForeignKey(name = "user_premium_account_fk",
    		    foreignKeyDefinition = "FOREIGN KEY (premium_account) REFERENCES account (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE SET NULL")
	)
    private Account premiumAccount;

    /**
     * The total amount of donated credits. The value depends on the precise question.
     */
    @Transient
    private Integer donatedCredits;
    
    public BankerUser() {
    	super();
    }
    
    public BankerUser(String identity) {
    	super(identity, null, null, null);
    }
    
    public BankerUser(NetMobielUser nbuser) {
    	super(nbuser);
    }
    
    public BankerUser(String identity, String givenName, String familyName, String email) {
    	super(identity, givenName, familyName, email);
    }
    
	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public String getUrnPrefix() {
		return URN_PREFIX;
	}
	
	public Account getPersonalAccount() {
		return personalAccount;
	}

	public void setPersonalAccount(Account personalAccount) {
		this.personalAccount = personalAccount;
	}

	public Account getPremiumAccount() {
		return premiumAccount;
	}

	public void setPremiumAccount(Account premiumAccount) {
		this.premiumAccount = premiumAccount;
	}

	public Integer getDonatedCredits() {
		return donatedCredits;
	}

	public void setDonatedCredits(Integer donatedCredits) {
		this.donatedCredits = donatedCredits;
	}

	public String createAccountName() {
		return String.format("%s %s", getGivenName() != null ? getGivenName() : "", getFamilyName() != null ? getFamilyName() : "").trim();
	}

}
