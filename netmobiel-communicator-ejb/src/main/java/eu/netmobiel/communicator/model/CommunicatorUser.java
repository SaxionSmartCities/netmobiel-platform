package eu.netmobiel.communicator.model;

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
import eu.netmobiel.communicator.util.CommunicatorUrnHelper;

@NamedEntityGraph()
@Entity
// You cannot have a table called 'user' in postgres, it is a reserved keyword
@Table(name = "cm_user", uniqueConstraints = {
	    @UniqueConstraint(name = "cs_managed_identity_unique", columnNames = { "managed_identity" })
})
@Vetoed
@SequenceGenerator(name = "user_sg", sequenceName = "user_id_seq", allocationSize = 1, initialValue = 50)
public class CommunicatorUser extends User  {

	private static final long serialVersionUID = -4237705703151528786L;
	public static final String URN_PREFIX = CommunicatorUrnHelper.createUrnPrefix(CommunicatorUser.class);
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_sg")
    private Long id;

    public CommunicatorUser() {
    	
    }
    
    public CommunicatorUser(NetMobielUser nbuser) {
    	super(nbuser);
    }
    
    public CommunicatorUser(String identity, String givenName, String familyName, String email) {
    	super(identity, givenName, familyName, email);
    }
    
    public CommunicatorUser(String identity) {
    	super(identity, null, null, null);
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
	

}
