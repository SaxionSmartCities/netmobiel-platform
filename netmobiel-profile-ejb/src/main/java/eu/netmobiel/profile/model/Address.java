package eu.netmobiel.profile.model;

import java.io.Serializable;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
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
import javax.validation.constraints.Size;

import eu.netmobiel.commons.model.GeoLocation;

@Entity
@Table(name = "address")
@Vetoed
@Access(AccessType.FIELD)
@SequenceGenerator(name = "address_sg", sequenceName = "address_id_seq", allocationSize = 1, initialValue = 50)
public class Address implements Serializable {
	private static final long serialVersionUID = -1112263880340112338L;

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "address_sg")
    private Long id;
	
//	@NotNull
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name= "profile", foreignKey = @ForeignKey(name = "address_profile_fk"))
	private Profile profile;

	/**
	 * The country code according to ISO 3166-2.
	 */
	@Size(max = 2)
	@Column(name = "country_code")
	private String countryCode;

	/**
	 * The city, village etc of this address
	 */
	@Size(max = 64)
	@Column(name = "locality")
	private String locality;

	/**
	 * The street name.
	 */
	@Size(max = 64)
	@Column(name = "street")
	private String street;
	
	/**
	 * The house number.
	 */
	@Size(max = 8)
	@Column(name = "house_number")
	private String houseNumber;
	
	/**
	 * The postal code.
	 */
	@Size(max = 8)
	@Column(name = "postal_code")
	private String postalCode;
	
	/**
	 * The GPS location and short name.
	 */
	@Embedded
	private GeoLocation location;

	public Address() {
		super();
	}
	
	public Address(Profile owner) {
		this.profile = owner;
	}
	
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

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public String getLocality() {
		return locality;
	}

	public void setLocality(String locality) {
		this.locality = locality;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getHouseNumber() {
		return houseNumber;
	}

	public void setHouseNumber(String houseNumber) {
		this.houseNumber = houseNumber;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public GeoLocation getLocation() {
		return location;
	}

	public void setLocation(GeoLocation location) {
		this.location = location;
	}
	
}
