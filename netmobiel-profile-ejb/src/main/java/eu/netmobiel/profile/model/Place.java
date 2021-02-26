package eu.netmobiel.profile.model;

import java.io.Serializable;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import eu.netmobiel.commons.model.GeoLocation;

@Entity
@Table(name = "place")
@Vetoed
@Access(AccessType.FIELD)
@SequenceGenerator(name = "place_sg", sequenceName = "place_id_seq", allocationSize = 1, initialValue = 50)
public class Place implements Serializable {
	private static final long serialVersionUID = -1112263880340112338L;

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "place_sg")
    private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name= "profile", foreignKey = @ForeignKey(name = "place_profile_fk"))
	private Profile profile;

	@Embedded
    @AttributeOverrides({ 
    	@AttributeOverride(name = "countryCode", column = @Column(name = "country_code", length = Address.MAX_COUNTRY_CODE_LENGTH)), 
    	@AttributeOverride(name = "locality", column = @Column(name = "locality", length = Address.MAX_LOCALITY_LENGTH)), 
    	@AttributeOverride(name = "street", column = @Column(name = "street", length = Address.MAX_STREET_LENGTH)), 
    	@AttributeOverride(name = "houseNumber", column = @Column(name = "house_nr", length = Address.MAX_HOUSE_NR_LENGTH)), 
    	@AttributeOverride(name = "postalCode", column = @Column(name = "postal_code", length = Address.MAX_POSTAL_CODE_LENGTH)), 
    	@AttributeOverride(name = "location.label", column = @Column(name = "label", length = GeoLocation.MAX_LABEL_LENGTH)), 
    	@AttributeOverride(name = "location.point", column = @Column(name = "point")), 
   	} )
	private Address address;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Profile getProfile() {
		return profile;
	}

	public void setProfile(Profile profile) {
		this.profile = profile;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Place[");
		if (id != null) {
			builder.append(id);
			builder.append(", ");
		}
		if (address != null) {
			builder.append(address);
		}
		builder.append("]");
		return builder.toString();
	}
	
}
