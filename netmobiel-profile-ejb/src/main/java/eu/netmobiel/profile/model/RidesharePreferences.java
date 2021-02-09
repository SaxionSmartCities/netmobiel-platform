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
@Table(name = "rideshare_preferences")
@Vetoed
@Access(AccessType.FIELD)
public class RidesharePreferences implements Serializable {
	private static final long serialVersionUID = 7052181227403511232L;
	private static final int DEFAULT_MAX_DETOUR = 10;
	private static final int DEFAULT_MAX_PASSENGERS = 1;

	@Id
    private Long id;
	
	@JoinColumn(name= "id", foreignKey = @ForeignKey(name = "rideshare_preferences_profile_fk"))
	@OneToOne(fetch = FetchType.LAZY)
	@MapsId
	private Profile profile;

	/**
	 * Maximum  detour to pick up a passenger in minutes. 
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
	
    @ElementCollection()
    @CollectionTable(name = "ridehare_luggage", joinColumns = { 
    	@JoinColumn(name = "profile", foreignKey = @ForeignKey(foreignKeyDefinition = "rideshare_luggage_profile_fk")) 
    })
    @Column(name = "luggage", length = 2)
    @OrderBy("ASC")
	private Set<LuggageOption> luggageOptions;
    
    public RidesharePreferences() {
    	super();
    }
    
    public RidesharePreferences(Profile profile) {
    	this();
    	this.profile = profile;
    	this.luggageOptions = new HashSet<>(Set.of(LuggageOption.GROCERIES, LuggageOption.HANDLUGGAGE));
    	
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
    

}
