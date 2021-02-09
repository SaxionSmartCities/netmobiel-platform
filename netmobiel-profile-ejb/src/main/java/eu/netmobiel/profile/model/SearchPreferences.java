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
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Entity
@Table(name = "search_preferences")
@Vetoed
@Access(AccessType.FIELD)
public class SearchPreferences implements Serializable {
	private static final long serialVersionUID = 7052181227403511232L;
	private static final int DEFAULT_MAX_TRANSFER_TIME = 10;
	private static final int DEFAULT_NR_PASSENGERS = 1;

	@Id
    private Long id;
	
	@JoinColumn(name= "id", foreignKey = @ForeignKey(name = "search_preferences_profile_fk"))
	@OneToOne(fetch = FetchType.LAZY)
	@MapsId
	private Profile profile;

	/**
	 * Maximum transfer time in minutes. 
	 */
	@NotNull
	@Positive
	@Column(name = "max_transfer_time")
	private Integer maxTransferTime = DEFAULT_MAX_TRANSFER_TIME;

	/**
	 * Default number of passengers to go along on a rideshare trip.
	 */
	@NotNull
	@Min(1)
	@Max(8)
	@Positive
	@Column(name = "number_of_passengers")
	private Integer numberOfPassengers = DEFAULT_NR_PASSENGERS;
	
    @ElementCollection()
    @CollectionTable(name = "passenger_luggage", joinColumns = { 
    	@JoinColumn(name = "profile", foreignKey = @ForeignKey(foreignKeyDefinition = "passenger_luggage_profile_fk")) 
    })
    @Column(name = "luggage", length = 2)
    @OrderBy("ASC")
	private Set<LuggageOption> luggageOptions;
    
	@Column(name = "allow_transfers", nullable = false)
    private boolean allowTransfers = true;
    
	@Column(name = "allow_first_leg_rideshare", nullable = false)
    private boolean allowFirstLegRideshare = false;
    
	@Column(name = "allow_last_leg_rideshare", nullable = false)
    private boolean allowLastLegRideshare = false;
    
    /**
     * The eligible traverse modes
     */
    @ElementCollection()
    @CollectionTable(name = "preferred_traverse_mode", joinColumns = { 
    	@JoinColumn(name = "profile", foreignKey = @ForeignKey(foreignKeyDefinition = "preferred_traverse_mode__profile_fk")) 
    })
    @Column(name = "traverse_mode", length = 2)
    @OrderBy("ASC")
    private Set<TraverseMode> allowedTraverseModes;

    public SearchPreferences() {
    	super();
    }
    
    public SearchPreferences(Profile profile) {
    	this();
    	this.profile = profile;
    	// Add a modifiable set to allow chnages before saving.
    	this.luggageOptions = new HashSet<>(Set.of(LuggageOption.GROCERIES, LuggageOption.HANDLUGGAGE));
    	this.allowedTraverseModes = new HashSet<>(Set.of(TraverseMode.BUS, TraverseMode.RAIL, TraverseMode.RIDESHARE, TraverseMode.WALK));
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

	public Integer getMaxTransferTime() {
		return maxTransferTime;
	}

	public void setMaxTransferTime(Integer maxTransferTime) {
		this.maxTransferTime = maxTransferTime;
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

	public boolean isAllowTransfers() {
		return allowTransfers;
	}

	public void setAllowTransfers(boolean allowTransfers) {
		this.allowTransfers = allowTransfers;
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

}
