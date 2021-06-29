package eu.netmobiel.profile.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@NamedEntityGraphs({
	@NamedEntityGraph(name = RidesharePreferences.DEFAULT_RIDESHARE_PREFS_ENTITY_GRAPH
	),
	@NamedEntityGraph(name = RidesharePreferences.FULL_RIDESHARE_PREFS_ENTITY_GRAPH,
		attributeNodes = {
			@NamedAttributeNode(value = "luggageOptions")
	})
})
@Entity
@Table(name = "rideshare_preferences")
@Vetoed
@Access(AccessType.FIELD)
public class RidesharePreferences implements Serializable {
	private static final long serialVersionUID = 7052181227403511232L;
	private static final int DEFAULT_MAX_DETOUR = 10;
	private static final int DEFAULT_MAX_PASSENGERS = 1;
	public static final String FULL_RIDESHARE_PREFS_ENTITY_GRAPH = "full-rideshare-prefs-entity-graph";
	public static final String DEFAULT_RIDESHARE_PREFS_ENTITY_GRAPH = "default-rideshare-prefs-entity-graph";

	/**
	 * Primary key.
	 */
	@Id
	@Access(AccessType.PROPERTY)
    private Long id;
	
	/**
	 * Reference to the profile. The id of the profile is mapped upon the id of the preferences. The foreign definition
	 * is not correctly copied by Hibernate. 
	 */
	@JoinColumn(name= "id", foreignKey = @ForeignKey(name = "rideshare_preferences_profile_fk"))
	@OneToOne(fetch = FetchType.LAZY)
	@MapsId
	private Profile profile;

	/**
	 * Default maximum detour to pick up a passenger in minutes. 
	 */
	@NotNull
	@Positive
	@Column(name = "max_minutes_detour")
	private Integer maxMinutesDetour = DEFAULT_MAX_DETOUR;

	/**
	 * The (default) maximum number of passengers to take in at a ride.
	 */
	@NotNull
	@Min(1)
	@Max(8)
	@Positive
	@Column(name = "max_passengers")
	private Integer maxPassengers = DEFAULT_MAX_PASSENGERS;
	
	/**
	 * Default accepted luggage options for rideshare rides driven by this driver.  
	 */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "rideshare_luggage", joinColumns = { 
    	@JoinColumn(name = "prefs", foreignKey = @ForeignKey(name = "rideshare_luggage_prefs_fk")) 
    })
    @Column(name = "luggage", length = 2)
    @OrderBy("ASC")
    @JoinColumn(name = "prefs")	// This definition is required by OnDelete, just a copy of the same column in @CollectionTable 
    @OnDelete(action = OnDeleteAction.CASCADE)
	private Set<LuggageOption> luggageOptions;

    /**
     * Default car to use with rideshare
     */
    @Size(max = 32)
    @Column(name = "default_car_ref", length = 2)
    private String defaultCarRef;
    
    public static RidesharePreferences createDefault(Profile profile) {
    	RidesharePreferences prefs = new RidesharePreferences();
    	prefs.luggageOptions = new HashSet<>(Set.of(LuggageOption.GROCERIES, LuggageOption.HANDLUGGAGE));
    	prefs.profile = profile;
    	prefs.id = profile.getId();
    	return prefs;
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

	public Integer getMaxMinutesDetour() {
		return maxMinutesDetour;
	}

	public void setMaxMinutesDetour(Integer maxMinutesDetour) {
		this.maxMinutesDetour = maxMinutesDetour;
	}

	public Integer getMaxPassengers() {
		return maxPassengers;
	}

	public void setMaxPassengers(Integer maxPassengers) {
		this.maxPassengers = maxPassengers;
	}

	public Set<LuggageOption> getLuggageOptions() {
		if (luggageOptions == null) {
			luggageOptions = new HashSet<>();
		}
		return luggageOptions;
	}

	public void setLuggageOptions(Set<LuggageOption> luggageOptions) {
		this.luggageOptions = luggageOptions;
	}

	public String getDefaultCarRef() {
		return defaultCarRef;
	}

	public void setDefaultCarRef(String defaultCarRef) {
		this.defaultCarRef = defaultCarRef;
	}
    

}
