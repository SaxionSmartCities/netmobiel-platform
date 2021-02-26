package eu.netmobiel.profile.model;

import java.io.Serializable;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.validation.constraints.Size;

import eu.netmobiel.commons.model.GeoLocation;

@Embeddable
@Vetoed
@Access(AccessType.FIELD)
public class Address implements Serializable {
	private static final long serialVersionUID = -1112263880340112338L;
	public static final int MAX_COUNTRY_CODE_LENGTH = 3;
	public static final int MAX_LOCALITY_LENGTH = 64;
	public static final int MAX_STREET_LENGTH = 64;
	public static final int MAX_HOUSE_NR_LENGTH = 8;
	public static final int MAX_POSTAL_CODE_LENGTH = 8;

	/**
	 * The country code according to ISO 3166-2.
	 */
	@Size(max = MAX_COUNTRY_CODE_LENGTH)
	@Column(name = "country_code")
	private String countryCode;

	/**
	 * The city, village etc of this address
	 */
	@Size(max = MAX_LOCALITY_LENGTH)
	@Column(name = "locality")
	private String locality;

	/**
	 * The street name.
	 */
	@Size(max = MAX_STREET_LENGTH)
	@Column(name = "street")
	private String street;
	
	/**
	 * The house number.
	 */
	@Size(max = MAX_HOUSE_NR_LENGTH)
	@Column(name = "house_number")
	private String houseNumber;
	
	/**
	 * The postal code.
	 */
	@Size(max = MAX_POSTAL_CODE_LENGTH)
	@Column(name = "postal_code")
	private String postalCode;
	
	/**
	 * The GPS location and short name.
	 */
	@Embedded
	private GeoLocation location;

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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (street != null) {
			builder.append(street);
			builder.append(" ");
		}
		if (houseNumber != null) {
			builder.append(houseNumber);
			builder.append(", ");
		}
		if (postalCode != null) {
			builder.append(postalCode);
			builder.append(" ");
		}
		if (locality != null) {
			builder.append(locality);
			builder.append(" ");
		}
		if (countryCode != null) {
			builder.append(countryCode);
			builder.append(", ");
		}
		if (location != null) {
			builder.append(location);
		}
		return builder.toString();
	}
	
}
