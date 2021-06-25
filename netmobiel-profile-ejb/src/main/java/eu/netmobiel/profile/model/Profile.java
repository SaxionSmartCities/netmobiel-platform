package eu.netmobiel.profile.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.HashSet;
import java.util.Set;

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
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.User;
import eu.netmobiel.profile.util.ProfileUrnHelper;

@NamedEntityGraphs({
	@NamedEntityGraph(name = Profile.DEFAULT_PROFILE_ENTITY_GRAPH
	),
})
@Entity
@Table(name = "profile", uniqueConstraints = {
	    @UniqueConstraint(name = "cs_managed_identity_unique", columnNames = { "managed_identity" }),
	    @UniqueConstraint(name = "cs_email_unique", columnNames = { "email" }),
})
@Vetoed
@SequenceGenerator(name = "profile_sg", sequenceName = "profile_id_seq", allocationSize = 1, initialValue = 50)
@AttributeOverride(name = "email", column = @Column(name = "email", length = User.MAX_LENGTH_EMAIL, nullable = false))
@Access(AccessType.FIELD)
public class Profile extends User  {

	private static final long serialVersionUID = -4237705703151528786L;
	public static final String URN_PREFIX = ProfileUrnHelper.createUrnPrefix("user");
	public static final String DEFAULT_PROFILE_ENTITY_GRAPH = "default-profile-entity-graph";

	/**
	 * Primary key.
	 */
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "profile_sg")
	@Access(AccessType.PROPERTY)
    private Long id;

	/**
	 * The favorite places of this user.
	 */
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "profile")
	private Set<Place> places;

	/** 
	 * The home address of this user.
	 */
	@Embedded
    @AttributeOverrides({ 
    	@AttributeOverride(name = "countryCode", column = @Column(name = "home_country_code", length = Address.MAX_COUNTRY_CODE_LENGTH)), 
    	@AttributeOverride(name = "stateCode", column = @Column(name = "home_state_code", length = Address.MAX_STATE_CODE_LENGTH)), 
    	@AttributeOverride(name = "locality", column = @Column(name = "home_locality", length = Address.MAX_LOCALITY_LENGTH)), 
    	@AttributeOverride(name = "street", column = @Column(name = "home_street", length = Address.MAX_STREET_LENGTH)), 
    	@AttributeOverride(name = "houseNumber", column = @Column(name = "home_house_nr", length = Address.MAX_HOUSE_NR_LENGTH)), 
    	@AttributeOverride(name = "postalCode", column = @Column(name = "home_postal_code", length = Address.MAX_POSTAL_CODE_LENGTH))
   	})
	private Address homeAddress;

	/**
	 * The home GPS location and short name.
	 */
	@Embedded
    @AttributeOverrides({ 
    	@AttributeOverride(name = "label", column = @Column(name = "home_label", length = GeoLocation.MAX_LABEL_LENGTH, nullable = true)), 
    	@AttributeOverride(name = "point", column = @Column(name = "home_point", nullable = true)) 
   	})
	private GeoLocation homeLocation;
	
	/**
	 * Rideshare preferences. Only needed for a driver. 
	 * Because of the one-to-one nature, the foreign key is on the side on the preferences.
	 * Experiments showed that bidirectional one-to-one leads to 3 separate queries, even if only the profile is needed. 
	 * Decision: The preferences are fetched explicitly and only when required.
	 * The relation is still in the profile, for easier handling in the rest interface.
	 */
	@Transient
	private RidesharePreferences ridesharePreferences;

	/**
	 * Search preferences. Only needed for a passenger.  
	 * Because of the one-to-one nature, the foreign key is on the side on the preferences.
	 * Experiments showed that bidirectional one-to-one leads to 3 separate queries, even if only the profile is needed. 
	 * Decision: The preferences are fetched explicitly and only when required.
	 * The relation is still in the profile, for easier handling in the rest interface.
	 */
	@Transient
	private SearchPreferences searchPreferences;

	/**
	 * Whether is user consents to some conditions.
	 */
	@Embedded
	private UserConsent consent;

	/**
	 * The birthday of the user, used for reporting.
	 */
	@Column(name = "date_of_birth")
	private LocalDate dateOfBirth;
	
	/**
	 * The Firebase Cloud Messaging token, a unique token to send push messages to a mobile phone.
	 */
	@Size(max = 512)
	@Column(name = "fcm_token")
	private String fcmToken;
	
	/**
	 * The (relative) path to a profile image of the user.
	 */
	@Size(max = 128)
	@Column(name = "image_path")
	private String imagePath;

	/**
	 * The phomne number of the user.
	 */
	@Size(max = 16)
	@Column(name = "phone_number")
	private String phoneNumber;
	
	/**
	 * A list of notification options for this user.
	 */
	@Embedded
	private NotificationOptions notificationOptions;

	/**
	 * The role of this user in NetMobiel.
	 */
	@NotNull
	@Column(name = "user_role", length = 2)
	private UserRole userRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", foreignKey = @ForeignKey(name = "profile_created_by_fk"), updatable = false)
    private Profile createdBy;

	/**
	 * The creation time of the profile.
	 */
    @NotNull
    @Column(name = "creation_time", updatable = false)
    private Instant creationTime;

	public Profile() {
		creationTime = Instant.now();
    }
    
	public Profile(String identity) {
		super(identity);
    }

	public Profile(NetMobielUser nbuser, UserRole role) {
    	super(nbuser);
    	this.userRole = role;
    }
    
    public Profile(String identity, String givenName, String familyName, String email, UserRole role) {
    	super(identity, givenName, familyName, email);
    	this.userRole = role;
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
	
	public Address getHomeAddress() {
		return homeAddress;
	}

	public void setHomeAddress(Address homeAddress) {
		this.homeAddress = homeAddress;
	}

	public void addAddressIfNotExists() {
		if (this.homeAddress == null) {
			this.homeAddress = Address.createDefault();
		}
	}

	public GeoLocation getHomeLocation() {
		return homeLocation;
	}

	public void setHomeLocation(GeoLocation homeLocation) {
		this.homeLocation = homeLocation;
	}

	public void addLocationIfNotExists() {
		if (this.homeLocation == null) {
			this.homeLocation = new GeoLocation();
		}
	}

	public RidesharePreferences getRidesharePreferences() {
		return ridesharePreferences;
	}

	public void setRidesharePreferences(RidesharePreferences ridesharePreferences) {
		this.ridesharePreferences = ridesharePreferences;
	}
	public SearchPreferences getSearchPreferences() {
		return searchPreferences;
	}

	public void setSearchPreferences(SearchPreferences searchPreferences) {
		this.searchPreferences = searchPreferences;
	}

	public UserConsent getConsent() {
		return consent;
	}

	public void setConsent(UserConsent consent) {
		this.consent = consent;
	}

	public LocalDate getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(LocalDate dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public Integer getAge() {
		if (this.dateOfBirth == null) {
			return null;
		}
		return Period.between(this.dateOfBirth, LocalDate.now()).getYears();
	}

	public String getFcmToken() {
		return fcmToken;
	}

	public void setFcmToken(String fcmToken) {
		this.fcmToken = fcmToken;
	}

	public String getImagePath() {
		return imagePath;
	}

	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public NotificationOptions getNotificationOptions() {
		return notificationOptions;
	}

	public void setNotificationOptions(NotificationOptions notificationOptions) {
		this.notificationOptions = notificationOptions;
	}

	public UserRole getUserRole() {
		return userRole;
	}

	public void setUserRole(UserRole userRole) {
		this.userRole = userRole;
	}

	public Profile getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Profile createdBy) {
		this.createdBy = createdBy;
	}

	public Instant getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Instant creationTime) {
		this.creationTime = creationTime;
	}

	public Set<Place> getPlaces() {
		if (places == null) {
			places = new HashSet<>();
		}
		return places;
	}

	public void setPlaces(Set<Place> places) {
		this.places = places;
	}

	public void addPlace(Place p) {
		p.setProfile(this);
		getPlaces().add(p);
	}

	public void removePlace(Place p) {
		p.setProfile(null);
		getPlaces().remove(p);
	}

	public final void addSearchPreferences() {
		this.searchPreferences = SearchPreferences.createDefault(this);
	}

	public final void removeSearchPreferences() {
		this.searchPreferences = null;
	}
	
	public final void addRidesharePreferences() {
		this.ridesharePreferences = RidesharePreferences.createDefault(this);
	}

	public final void removeRidesharePreferences() {
		this.ridesharePreferences = null;
	}

	/**
	 * Assures that required associations and embedded objects are defined. 
	 */
	public void linkOneToOneChildren() {
		if (this.notificationOptions == null) {
			this.notificationOptions = NotificationOptions.createDefault();
		}
		if (this.consent == null) {
			this.consent = UserConsent.createDefault();
		}
		if (userRole == null) {
			userRole = UserRole.PASSENGER;
		}
		if (userRole == UserRole.PASSENGER || userRole == UserRole.BOTH) {
			if (searchPreferences == null) {
				searchPreferences = SearchPreferences.createDefault(this);
			}
		}
		if (this.searchPreferences != null) {
			if (this.searchPreferences.getProfile() == null) {
				this.searchPreferences.setProfile(this);
			}
		}
		if (userRole == UserRole.DRIVER || userRole == UserRole.BOTH) {
			if (ridesharePreferences == null) {
				ridesharePreferences = RidesharePreferences.createDefault(this);
			}
		}
		if (this.ridesharePreferences != null) {
			if (ridesharePreferences.getProfile() == null) {
				this.ridesharePreferences.setProfile(this);
			}
		}
		// Once the preferences are present, they will not be removed on a change of role.
	}
	
	public boolean isPassenger() {
		return userRole == UserRole.PASSENGER || userRole == UserRole.BOTH;
	}

	public boolean isDriver() {
		return userRole == UserRole.DRIVER || userRole == UserRole.BOTH;
	}

	public String getNameEmailPhone() {
		StringBuilder sb = new StringBuilder();
		if (getGivenName() != null) {
			sb.append(getGivenName()).append(" ");
		}
		if (getFamilyName() != null) {
			sb.append(getFamilyName()).append(" ");
		}
		if (getFamilyName() != null) {
			sb.append(getPhoneNumber()).append(" ");
		}
		if (getEmail() != null) {
			sb.append("(").append(getEmail()).append(")");
		}
		return sb.toString().trim();
	}

	public String getDefaultCountry() {
		String defaultCountryCode = Address.DEFAULT_COUNTRY_CODE;
	    if (getHomeAddress() != null && !StringUtils.isAllBlank(getHomeAddress().getCountryCode())) {
	    	defaultCountryCode = getHomeAddress().getCountryCode();
	    }
	    return defaultCountryCode;
	}
}
