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
import javax.persistence.UniqueConstraint;

import eu.netmobiel.banker.util.BankerUrnHelper;
import eu.netmobiel.commons.model.User;
import eu.netmobiel.commons.model.NetMobielUser;

@NamedEntityGraphs({
	@NamedEntityGraph(
			name = BankerUser.GRAPH_WITH_BALANCE, 
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
public class BankerUser extends User {
	private static final long serialVersionUID = -4237705703151528786L;
	public static final String URN_PREFIX = BankerUrnHelper.createUrnPrefix(BankerUser.class);
	public static final String GRAPH_WITH_BALANCE = "user-graph-with-balance";
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_sg")
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "personal_account", foreignKey = @ForeignKey(name = "user_personal_account_fk"))
    private Account personalAccount;
    
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
	
	public Account getPersonalAccount() {
		return personalAccount;
	}

	public void setPersonalAccount(Account personalAccount) {
		this.personalAccount = personalAccount;
	}

	public String createAccountName() {
		return String.format("%s %s", getGivenName() != null ? getGivenName() : "", getFamilyName() != null ? getFamilyName() : "").trim();
	}

}
