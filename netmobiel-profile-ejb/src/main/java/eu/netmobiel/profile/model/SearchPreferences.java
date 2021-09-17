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

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@NamedEntityGraphs({
	@NamedEntityGraph(name = SearchPreferences.DEFAULT_SEARCH_PREFS_ENTITY_GRAPH
	),
	@NamedEntityGraph(name = SearchPreferences.FULL_SEARCH_PREFS_ENTITY_GRAPH,
		attributeNodes = {
			@NamedAttributeNode(value = "allowedTraverseModes"),
			@NamedAttributeNode(value = "luggageOptions")
	})
})
@Entity
@Table(name = "search_preferences")
@Vetoed
@Access(AccessType.FIELD)
public class SearchPreferences implements Serializable {
	private static final long serialVersionUID = 7052181227403511232L;
	private static final int DEFAULT_MAX_WALK_DISTANCE = 2000;
	private static final int DEFAULT_NR_PASSENGERS = 1;
	public static final String FULL_SEARCH_PREFS_ENTITY_GRAPH = "full-search-prefs-entity-graph";
	public static final String DEFAULT_SEARCH_PREFS_ENTITY_GRAPH = "default-search-prefs-entity-graph";

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
	@JoinColumn(name= "id", foreignKey = @ForeignKey(name = "search_preferences_profile_fk"))
	@OneToOne(fetch = FetchType.LAZY)
	@MapsId
	private Profile profile;

	/**
	 * Default maximum walk distance meter. 
	 */
	@NotNull
	@Positive
	@Column(name = "max_walk_distance")
	private Integer maxWalkDistance = DEFAULT_MAX_WALK_DISTANCE;

	/**
	 * Default number of passengers to go along on a rideshare trip.
	 */
	@NotNull
	@Min(1)
	@Max(8)
	@Positive
	@Column(name = "number_of_passengers")
	private Integer numberOfPassengers = DEFAULT_NR_PASSENGERS;
	
	/**
	 * Default luggage to take on a trip.
	 */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "passenger_luggage", joinColumns = { 
    	@JoinColumn(name = "prefs", foreignKey = @ForeignKey(name = "passenger_luggage_prefs_fk")) 
    })
    @Column(name = "luggage", length = 2)
    @OrderBy("ASC")
    @JoinColumn(name = "prefs")	// This definition is required by OnDelete, just a copy of the same column in @CollectionTable 
    @OnDelete(action = OnDeleteAction.CASCADE)
	private Set<LuggageOption> luggageOptions;
    
    /**
     * How many transfers are allowed?
     */
	@Column(name = "max_transfers")
    private Integer maxTransfers;
    
    /**
     * Is (in a multilegged trip) a first leg with rideshare allowed (default)? Example: From home to a train station.
     */
	@Column(name = "allow_first_leg_rideshare", nullable = false)
    private boolean allowFirstLegRideshare = false;
    
    /**
     * Is (in a multilegged trip) a last leg with rideshare allowed (default)? Example: train station to home.
     */
	@Column(name = "allow_last_leg_rideshare", nullable = false)
    private boolean allowLastLegRideshare = false;
    
    /**
     * The default eligible traverse modes.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "preferred_traverse_mode", joinColumns = { 
    	@JoinColumn(name = "prefs", foreignKey = @ForeignKey(name = "preferred_traverse_mode_prefs_fk")) 
    })
    @Column(name = "traverse_mode", length = 2)
    @OrderBy("ASC")
    @JoinColumn(name = "prefs")	// This definition is required by OnDelete, just a copy of the same column in @CollectionTable 
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<TraverseMode> allowedTraverseModes;

    public static SearchPreferences createDefault(Profile profile) {
    	SearchPreferences prefs = new SearchPreferences();
    	prefs.luggageOptions = new HashSet<>(Set.of(LuggageOption.values()));
    	prefs.allowedTraverseModes = new HashSet<>(Set.of(TraverseMode.BUS, TraverseMode.RAIL, TraverseMode.RIDESHARE, TraverseMode.WALK));
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

	public Integer getNumberOfPassengers() {
		return numberOfPassengers;
	}

	public void setNumberOfPassengers(Integer numberOfPassengers) {
		this.numberOfPassengers = numberOfPassengers;
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

	public boolean isAllowFirstLegRideshare() {
		return allowFirstLegRideshare;
	}

	public void setAllowFirstLegRideshare(boolean allowFirstLegRideshare) {
		this.allowFirstLegRideshare = allowFirstLegRideshare;
	}

	public boolean isAllowLastLegRideshare() {
		return allowLastLegRideshare;
	}

	public void setAllowLastLegRideshare(boolean allowLastLegRideshare) {
		this.allowLastLegRideshare = allowLastLegRideshare;
	}

	public Set<TraverseMode> getAllowedTraverseModes() {
		if (allowedTraverseModes == null) {
			allowedTraverseModes = new HashSet<>();
		}
		return allowedTraverseModes;
	}

	public void setAllowedTraverseModes(Set<TraverseMode> allowedTraverseModes) {
		this.allowedTraverseModes = allowedTraverseModes;
	}

	public Integer getMaxWalkDistance() {
		return maxWalkDistance;
	}

	public void setMaxWalkDistance(Integer maxWalkDistance) {
		this.maxWalkDistance = maxWalkDistance;
	}

	public Integer getMaxTransfers() {
		return maxTransfers;
	}

	public void setMaxTransfers(Integer maxTransfers) {
		this.maxTransfers = maxTransfers;
	}
}
