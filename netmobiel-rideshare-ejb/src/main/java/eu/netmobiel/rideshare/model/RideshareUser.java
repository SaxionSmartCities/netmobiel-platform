package eu.netmobiel.rideshare.model;

import java.util.List;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.User;
import eu.netmobiel.rideshare.util.RideshareUrnHelper;

@NamedEntityGraph(
		name = RideshareUser.LIST_USERS_WITH_CARS_ENTITY_GRAPH,
		attributeNodes = { 
			@NamedAttributeNode(value = "carsInUse"),		
		}
	)
@Entity
// You cannot have a table called 'user' in postgres, it is a reserved keyword
@Table(name = "rs_user", uniqueConstraints = {
	    @UniqueConstraint(name = "cs_managed_identity_unique", columnNames = { "managed_identity" })
})
@Vetoed
@SequenceGenerator(name = "user_sg", sequenceName = "user_id_seq", allocationSize = 1, initialValue = 50)
public class RideshareUser extends User {

	private static final long serialVersionUID = -4237705703151528786L;
	public static final String URN_PREFIX = RideshareUrnHelper.createUrnPrefix("user");
	public static final String LIST_USERS_WITH_CARS_ENTITY_GRAPH = "list-users-cars-graph";

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_sg")
    private Long id;

    @OneToMany(mappedBy = "driver")
    private List<Car> carsInUse;

    public RideshareUser() {
    	// No args constructor
    }
    
    public RideshareUser(String identity, String givenName, String familyName) {
    	super(identity, givenName, familyName, null);
    }
    
    /**
     * Copy constructor from general definition.
     * @param bu the basic user fields
     */
    public RideshareUser(NetMobielUser nmu) {
    	super(nmu);
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
    
	public List<Car> getCarsInUse() {
		return carsInUse;
	}

	public void setCarsInUse(List<Car> carsInUse) {
		this.carsInUse = carsInUse;
	}

}
