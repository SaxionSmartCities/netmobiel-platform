package eu.netmobiel.profile.model;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.Vetoed;
import javax.persistence.CascadeType;
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
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.NamedSubgraph;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.User;
import eu.netmobiel.profile.util.ProfileUrnHelper;

/*
 * The graphs are extracted as an carthesian product. In the case of the luggage options it is the question whether
 * it is worthwhile to repeat the complete profile structure just of a single luggage options, it is a lot of data.   
 * same with the addresses.
 */
@NamedEntityGraphs({
	@NamedEntityGraph(name = Profile.DEFAULT_PROFILE_ENTITY_GRAPH
	),
	@NamedEntityGraph(name = Profile.HOME_PROFILE_ENTITY_GRAPH,
		attributeNodes = {
			@NamedAttributeNode(value = "homeAddress"),
	}),
	@NamedEntityGraph(name = Profile.FULL_PROFILE_ENTITY_GRAPH,
		attributeNodes = {
			@NamedAttributeNode(value = "homeAddress"),
			@NamedAttributeNode(value = "ridesharePreferences"),
			@NamedAttributeNode(value = "searchPreferences")
	}),
	@NamedEntityGraph(name = Profile.FULLEST_PROFILE_ENTITY_GRAPH,
		attributeNodes = {
			@NamedAttributeNode(value = "addresses"),
			@NamedAttributeNode(value = "homeAddress"),
			@NamedAttributeNode(value = "ridesharePreferences", subgraph = "subgraph.rideshare-prefs"),
			@NamedAttributeNode(value = "searchPreferences", subgraph = "subgraph.search-prefs"),
		}, subgraphs =  {
			@NamedSubgraph(
					name = "subgraph.rideshare-prefs",
					attributeNodes = {
							@NamedAttributeNode(value = "luggageOptions")
			}),
			@NamedSubgraph(
					name = "subgraph.search-prefs",
					attributeNodes = {
							@NamedAttributeNode(value = "allowedTraverseModes"),
							@NamedAttributeNode(value = "luggageOptions")
			}),
	})
})
@Entity
@Table(name = "profile", uniqueConstraints = {
	    @UniqueConstraint(name = "cs_managed_identity_unique", columnNames = { "managed_identity" })
})
@Vetoed
@SequenceGenerator(name = "profile_sg", sequenceName = "profile_id_seq", allocationSize = 1, initialValue = 50)
public class Profile extends User  {

	private static final long serialVersionUID = -4237705703151528786L;
	public static final String URN_PREFIX = ProfileUrnHelper.createUrnPrefix("user");
	public static final String FULLEST_PROFILE_ENTITY_GRAPH = "fullest-profile-entity-graph";
	public static final String FULL_PROFILE_ENTITY_GRAPH = "full-profile-entity-graph";
	public static final String HOME_PROFILE_ENTITY_GRAPH = "home-profile-entity-graph";
	public static final String DEFAULT_PROFILE_ENTITY_GRAPH = "default-profile-entity-graph";
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "profile_sg")
    private Long id;

	@OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.ALL } , mappedBy = "profile", orphanRemoval = true)
	private Set<Address> addresses;
	
	@ManyToOne(fetch = FetchType.LAZY) 
	@JoinColumn(name = "home_address", nullable = true, foreignKey = 
		@ForeignKey(
			name = "profile_home_address_fk", 
			foreignKeyDefinition = "FOREIGN KEY (home_address) REFERENCES address (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE SET NULL")
	)
	private Address homeAddress;

	@OneToOne(cascade = CascadeType.ALL, mappedBy = "profile", fetch = FetchType.LAZY, orphanRemoval = true, optional = true)
	private RidesharePreferences ridesharePreferences;

	@OneToOne(cascade = CascadeType.ALL, mappedBy = "profile", fetch = FetchType.LAZY, orphanRemoval = true, optional = false) 
	private SearchPreferences searchPreferences;

	@Embedded
	private UserConsent consent;

	@Column(name = "date_of_birth")
	private LocalDate dateOfBirth;
	
	@Size(max = 512)
	@Column(name = "fcm_token")
	private String fcmToken;
	
	@Size(max = 128)
	@Column(name = "image_path")
	private String imagePath;

	@Size(max = 16)
	@Column(name = "phone_number")
	private String phoneNumber;
	
	@Embedded
	private NotificationOptions notificationOptions;

	@NotNull
	@Column(name = "user_role", length = 2)
	private UserRole userRole;

	public Profile() {
		initializeEmbeddedRelations();
    }
    
    public Profile(NetMobielUser nbuser) {
    	super(nbuser);
		initializeEmbeddedRelations();
    }
    
    public Profile(String identity, String givenName, String familyName, String email, UserRole role) {
    	super(identity, givenName, familyName, email);
    	this.userRole = role;
		initializeEmbeddedRelations();
    }
    
    public Profile(String identity, UserRole role) {
    	this(identity, null, null, null, role);
    }

    private void initializeEmbeddedRelations() {
		consent = new UserConsent();
		notificationOptions = new NotificationOptions();
		searchPreferences = new SearchPreferences(this);
		if (userRole == UserRole.DRIVER || userRole == UserRole.BOTH) {
			addRidesharePreferences();
		}
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

	public Set<Address> getAddresses() {
		if (addresses == null) {
			addresses = new HashSet<>();
		}
		return addresses;
	}

	public void setAddresses(Set<Address> addresses) {
		this.addresses = addresses;
	}

	public void addAddress(Address a) {
		a.setProfile(this);
		getAddresses().add(a);
	}

	public void removeAddress(Address a) {
		a.setProfile(null);
		getAddresses().remove(a);
	}

	public final void addRidesharePreferences() {
		this.ridesharePreferences = new RidesharePreferences(this);
	}

	public final void removeRidesharePreferences() {
		this.ridesharePreferences = null;
	}
	
	public void linkOneToOneChildren() {
		if (this.searchPreferences != null) {
			this.searchPreferences.setProfile(this);
			this.searchPreferences.setId(this.getId());
		}
		if (this.ridesharePreferences != null) {
			this.ridesharePreferences.setProfile(this);
			this.ridesharePreferences.setId(this.getId());
		}
	}
	
	public void linkAddresses() {
		getAddresses().forEach(addr -> addr.setProfile(this));
	}
}
