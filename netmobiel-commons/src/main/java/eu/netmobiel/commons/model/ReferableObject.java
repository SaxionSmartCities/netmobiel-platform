package eu.netmobiel.commons.model;

import java.io.Serializable;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.MappedSuperclass;

import eu.netmobiel.commons.util.UrnHelper;

@MappedSuperclass
@Access(AccessType.FIELD)
public abstract class ReferableObject implements Serializable {
	private static final long serialVersionUID = 6280262978437984020L;

    public ReferableObject() {
    }
    
    /**
     * Returns a URN for this particular instance.
     * IMPORTANT: A call to this function will initialize the entity, i.e., unproxy it. ONLY use the method as a convenience. 
     * In lazy relationships you must ONLY use the getId() method.
     * @return A URN reflecting the instance.
     */
	public String getUrn() {
		if (getId() == null) {
			throw new IllegalStateException("Object has not persisted yet");
		}
		return UrnHelper.createUrn(getUrnPrefix(), getId());
	}

	public abstract Long getId();

	public abstract String getUrnPrefix();

}
