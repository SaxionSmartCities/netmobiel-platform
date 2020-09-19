package eu.netmobiel.planner.model;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedEntityGraph;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.User;
import eu.netmobiel.planner.util.PlannerUrnHelper;

@NamedEntityGraph()
@Entity
// You cannot have a table called 'user' in postgres, it is a reserved keyword
@Table(name = "pl_user", uniqueConstraints = {
	    @UniqueConstraint(name = "cs_managed_identity_unique", columnNames = { "managed_identity" })
})
@Vetoed
@SequenceGenerator(name = "user_sg", sequenceName = "user_id_seq", allocationSize = 1, initialValue = 50)
public class PlannerUser extends User {

	private static final long serialVersionUID = -4237705703151528786L;
	public static final String URN_PREFIX = PlannerUrnHelper.createUrnPrefix("user");
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_sg")
    private Long id;
    
    public PlannerUser() {
    	
    }
    public PlannerUser(String identity) {
    	super(identity, null, null, null);
    }
    
    public PlannerUser(NetMobielUser nbuser) {
    	super(nbuser);
    }

    public PlannerUser(String identity, String givenName, String familyName) {
    	this(identity, givenName, familyName, null);
    }
    
    public PlannerUser(String identity, String givenName, String familyName, String email) {
    	super(identity, givenName, familyName, email);
    }
    
	@Override
	public String getUrnPrefix() {
		return URN_PREFIX;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

    
}
