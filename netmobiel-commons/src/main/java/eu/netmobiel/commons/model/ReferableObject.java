package eu.netmobiel.commons.model;

import java.io.Serializable;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import eu.netmobiel.commons.util.UrnHelper;

@MappedSuperclass
public abstract class ReferableObject implements Serializable {
	private static final long serialVersionUID = 6280262978437984020L;

	@Transient
    private String reference;
    
    public ReferableObject() {
    }
    
	public String getReference() {
		if (getId() == null) {
			throw new IllegalStateException("Object has not persisted yet");
		}
		if (reference == null) {
			reference  = UrnHelper.createUrn(getUrnPrefix(), getId());
		}
		return reference;
	}

	public abstract Long getId();

	public abstract String getUrnPrefix();

}
