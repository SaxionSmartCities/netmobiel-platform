package eu.netmobiel.planner.model;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
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
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedSubgraph;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.report.ModalityNumericReportValue;
import eu.netmobiel.commons.report.NumericReportValue;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.planner.util.PlannerUrnHelper;

/**
 * Model of a trip. A trip has an itinerary with legs and stops. A trip is executed by the traveller.
 * In the beginning, a trip may have a plan only, no itinerary. An itinerary is by definition calculated by the 
 * NetMobiel planner. In the very beginning there may be an idea of a trip (spatial displacement), but not yet 
 * how (modality) and when (temporal). This is why a trip has a departure and arrival location itself.
 * A leg is a precise definition of how, when and where. An itinerary is a collection of legs a summarises a few
 * characteristics like duration, distance, waiting time etc. 
 * 
 * Should a traveller be allowed to plan individual legs? It gets complicated when allowing so, a better design is to create 
 * partial trips, i.e., a trip consists then of one or more partial trips. Each partial trip is planned by the traveller.
 * With that model all model elements are reusable. For now we stick to a single trip in one piece.
 * 
 * TODO: The trip state 'Cancelled' is incorrect with regard to analysis. Better is to introduce a separate cancel flag.
 * With the flag the last trip state is kept, so that we can analyse in which state a trip is cancelled.
 * Alternative: Maintain a history of the trip state changes.
 * 
 * A trip is created with a reference to an itinerary as calculated earlier by the planner. When a traveller does not get 
 * satisfactory results, he or she can issue a shout-out. A shout-out creates a trip plan, sets the 
 * modality to rideshare and posts then all potential drivers with the request. A driver can respond by offering a ride. 
 * The ride will be added as a itinerary in the shout-out plan. At some point in time the traveller will decide which 
 * ride (that is: itinerary) to select. From that on the process continues as with a ordinary planning: In case of rideshare 
 * a booking process is started. The driver confirms and then the trip is scheduled.  
 *  
 *  Once it is time, the trip will become in transit. When the traveller arrives, the trip is completed.  
 * 
 */
@NamedNativeQueries({
	@NamedNativeQuery(
		name = Trip.RGP_1_TRIPS_CREATED_COUNT,
		query = "select u.managed_identity as managed_identity, "
        		+ "date_part('year', it.departure_time) as year, " 
        		+ "date_part('month', it.departure_time) as month, "
        		+ "count(*) as count "
        		+ "from trip t "
        		+ "join pl_user u on u.id = t.traveller "
        		+ "join itinerary it on it.id = t.itinerary "
        		+ "where it.departure_time >= ? and it.departure_time < ? "
        		+ "group by u.managed_identity, year, month "
        		+ "order by u.managed_identity, year, month",
        resultSetMapping = Trip.PN_TRIP_USER_YEAR_MONTH_COUNT_MAPPING),
	@NamedNativeQuery(
			name = Trip.RGP_2_TRIPS_CANCELLED_COUNT,
			query = "select u.managed_identity as managed_identity, "
	        		+ "date_part('year', it.departure_time) as year, " 
	        		+ "date_part('month', it.departure_time) as month, "
	        		+ "count(*) as count "
	        		+ "from trip t "
	        		+ "join pl_user u on u.id = t.traveller "
	        		+ "join itinerary it on it.id = t.itinerary "
	        		+ "where it.departure_time >= ? and it.departure_time < ? and t.state = 'CNC' "
	        		+ "group by u.managed_identity, year, month "
	        		+ "order by u.managed_identity, year, month",
	        resultSetMapping = Trip.PN_TRIP_USER_YEAR_MONTH_COUNT_MAPPING),
	@NamedNativeQuery(
			name = Trip.RGP_3_TRIPS_CANCELLED_BY_PASSENGER_COUNT,
			query = "select u.managed_identity as managed_identity, "
	        		+ "date_part('year', it.departure_time) as year, " 
	        		+ "date_part('month', it.departure_time) as month, "
	        		+ "count(*) as count "
	        		+ "from trip t "
	        		+ "join pl_user u on u.id = t.traveller "
	        		+ "join itinerary it on it.id = t.itinerary "
	        		+ "where it.departure_time >= ? and it.departure_time < ? and t.state = 'CNC' and (t.cancelled_by_provider = false or t.cancelled_by_provider is null)"
	        		+ "group by u.managed_identity, year, month "
	        		+ "order by u.managed_identity, year, month",
	        resultSetMapping = Trip.PN_TRIP_USER_YEAR_MONTH_COUNT_MAPPING),
	@NamedNativeQuery(
			name = Trip.RGP_4_TRIPS_CANCELLED_BY_PROVIDER_COUNT,
			query = "select u.managed_identity as managed_identity, "
	        		+ "date_part('year', it.departure_time) as year, " 
	        		+ "date_part('month', it.departure_time) as month, "
	        		+ "count(*) as count "
	        		+ "from trip t "
	        		+ "join pl_user u on u.id = t.traveller "
	        		+ "join itinerary it on it.id = t.itinerary "
	        		+ "where it.departure_time >= ? and it.departure_time < ? and t.state = 'CNC' and t.cancelled_by_provider = true "
	        		+ "group by u.managed_identity, year, month "
	        		+ "order by u.managed_identity, year, month",
	        resultSetMapping = Trip.PN_TRIP_USER_YEAR_MONTH_COUNT_MAPPING),
	@NamedNativeQuery(
			name = Trip.RGP_5_TRIPS_WITH_CONFIRMED_RIDESHARE_COUNT,
					// --> Count the number of trips with a rideshare leg with payment state 'Paid'
			query = "select u.managed_identity as managed_identity, "
	        		+ "date_part('year', it.departure_time) as year, " 
	        		+ "date_part('month', it.departure_time) as month, "
	        		+ "count(distinct t.id) as count "
	        		+ "from trip t "
	        		+ "join pl_user u on u.id = t.traveller "
	        		+ "join itinerary it on it.id = t.itinerary "
	        		+ "where it.departure_time >= ? and it.departure_time < ? and "
	        		+ " exists (select 1 from leg lg where lg.itinerary = it.id and lg.traverse_mode = 'RS' and lg.payment_state = 'P') " 
	        		+ "group by u.managed_identity, year, month "
	        		+ "order by u.managed_identity, year, month",
	        resultSetMapping = Trip.PN_TRIP_USER_YEAR_MONTH_COUNT_MAPPING),
	@NamedNativeQuery(
			name = Trip.RGP_6_TRIPS_WITH_CANCELLED_RIDESHARE_PAYMENT_COUNT,
					// --> Count the number of trips with a rideshare leg with payment state 'Cancelled'
			query = "select u.managed_identity as managed_identity, "
	        		+ "date_part('year', it.departure_time) as year, " 
	        		+ "date_part('month', it.departure_time) as month, "
	        		+ "count(distinct t.id) as count "
	        		+ "from trip t "
	        		+ "join pl_user u on u.id = t.traveller "
	        		+ "join itinerary it on it.id = t.itinerary "
	        		+ "where it.departure_time >= ? and it.departure_time < ? and "
	        		+ " exists (select 1 from leg lg where lg.itinerary = it.id and lg.traverse_mode = 'RS' and lg.payment_state = 'C') " 
	        		+ "group by u.managed_identity, year, month "
	        		+ "order by u.managed_identity, year, month",
	        resultSetMapping = Trip.PN_TRIP_USER_YEAR_MONTH_COUNT_MAPPING),
	@NamedNativeQuery(
			name = Trip.RGP_7_MONO_MODAL_TRIPS_COUNT,
					// --> Count the number of completed trips with a just one non-walking leg
			query = "select u.managed_identity as managed_identity, "
	        		+ "date_part('year', it.departure_time) as year, " 
	        		+ "date_part('month', it.departure_time) as month, "
	        		+ "count(distinct t.id) as count "
	        		+ "from trip t "
	        		+ "join pl_user u on u.id = t.traveller "
	        		+ "join itinerary it on it.id = t.itinerary "
	        		+ "where it.departure_time >= ? and it.departure_time < ? and t.state = 'CMP' and "
	        		+ " (select count(distinct lg.traverse_mode) from leg lg where lg.itinerary = it.id and lg.traverse_mode <> 'WK') = 1 " 
	        		+ "group by u.managed_identity, year, month "
	        		+ "order by u.managed_identity, year, month",
	        resultSetMapping = Trip.PN_TRIP_USER_YEAR_MONTH_COUNT_MAPPING),
	@NamedNativeQuery(
			name = Trip.RGP_8_MONO_MODAL_TRIPS_BY_MODALITY_COUNT,
					// --> Count the number of competed trips for each modality separately
			query = "select u.managed_identity as managed_identity, "
	        		+ "date_part('year', it.departure_time) as year, " 
	        		+ "date_part('month', it.departure_time) as month, "
	        		+ "count(distinct t.id) as count, "
	        		+ "lg.traverse_mode as modality " 
	        		+ "from trip t "
	        		+ "join pl_user u on u.id = t.traveller "
	        		+ "join itinerary it on it.id = t.itinerary "
	        		+ "join leg lg on it.id = lg.itinerary "
	        		+ "where it.departure_time >= ? and it.departure_time < ? and t.state = 'CMP' and lg.traverse_mode <> 'WK' and t.state = 'CMP' and "
	        		+ " (select count(distinct lg.traverse_mode) from leg lg where lg.itinerary = it.id and lg.traverse_mode <> 'WK') = 1 " 
	        		+ "group by u.managed_identity, year, month, modality "
	        		+ "order by u.managed_identity, year, month, modality",
	        resultSetMapping = Trip.PN_TRIP_USER_YEAR_MONTH_COUNT_MODALITY_MAPPING),
	@NamedNativeQuery(
			name = Trip.RGP_9_MULTI_MODAL_TRIPS_COUNT,
					// --> Count the number of completed trips with more than one non-walking leg
			query = "select u.managed_identity as managed_identity, "
	        		+ "date_part('year', it.departure_time) as year, " 
	        		+ "date_part('month', it.departure_time) as month, "
	        		+ "count(distinct t.id) as count "
	        		+ "from trip t "
	        		+ "join pl_user u on u.id = t.traveller "
	        		+ "join itinerary it on it.id = t.itinerary "
	        		+ "where it.departure_time >= ? and it.departure_time < ? and t.state = 'CMP' and "
	        		+ " (select count(distinct lg.traverse_mode) from leg lg where lg.itinerary = it.id and lg.traverse_mode <> 'WK') > 1 " 
	        		+ "group by u.managed_identity, year, month "
	        		+ "order by u.managed_identity, year, month",
	        resultSetMapping = Trip.PN_TRIP_USER_YEAR_MONTH_COUNT_MAPPING),
	@NamedNativeQuery(
			name = Trip.RGP_10_MULTI_MODAL_TRIPS_BY_MODALITY_COUNT,
					// --> Count the number of completed trips for each modality separately (ignoring walking)
			query = "select u.managed_identity as managed_identity, "
	        		+ "date_part('year', it.departure_time) as year, " 
	        		+ "date_part('month', it.departure_time) as month, "
	        		+ "count(distinct t.id) as count, "
	        		+ "lg.traverse_mode as modality " 
	        		+ "from trip t "
	        		+ "join pl_user u on u.id = t.traveller "
	        		+ "join itinerary it on it.id = t.itinerary "
	        		+ "join leg lg on it.id = lg.itinerary "
	        		+ "where it.departure_time >= ? and it.departure_time < ? and t.state = 'CMP' and lg.traverse_mode <> 'WK' and t.state = 'CMP' and "
	        		+ " (select count(distinct lg.traverse_mode) from leg lg where lg.itinerary = it.id and lg.traverse_mode <> 'WK') > 1 " 
	        		+ "group by u.managed_identity, year, month, modality "
	        		+ "order by u.managed_identity, year, month, modality",
	        resultSetMapping = Trip.PN_TRIP_USER_YEAR_MONTH_COUNT_MODALITY_MAPPING),
})
@SqlResultSetMappings({
	@SqlResultSetMapping(
			name = Trip.PN_TRIP_USER_YEAR_MONTH_COUNT_MAPPING, 
			classes = @ConstructorResult(
				targetClass = NumericReportValue.class, 
				columns = {
						@ColumnResult(name = "managed_identity", type = String.class),
						@ColumnResult(name = "year", type = int.class),
						@ColumnResult(name = "month", type = int.class),
						@ColumnResult(name = "count", type = int.class)
				}
			)
		),
	@SqlResultSetMapping(
			name = Trip.PN_TRIP_USER_YEAR_MONTH_COUNT_MODALITY_MAPPING, 
			classes = @ConstructorResult(
				targetClass = ModalityNumericReportValue.class, 
				columns = {
						@ColumnResult(name = "managed_identity", type = String.class),
						@ColumnResult(name = "year", type = int.class),
						@ColumnResult(name = "month", type = int.class),
						@ColumnResult(name = "count", type = int.class),
						@ColumnResult(name = "modality", type = String.class)
				}
			)
		)
})
@NamedEntityGraphs({
	@NamedEntityGraph(
			name = Trip.DETAILED_ENTITY_GRAPH, 
			attributeNodes = { 
					@NamedAttributeNode(value = "itinerary", subgraph = "subgraph.itinerary"),		
					@NamedAttributeNode(value = "traveller")		
			}, subgraphs = {
					@NamedSubgraph(
							name = "subgraph.itinerary",
							attributeNodes = {
									@NamedAttributeNode(value = "legs", subgraph = "subgraph.leg")
							}
					),
					@NamedSubgraph(
							name = "subgraph.leg",
							attributeNodes = {
									@NamedAttributeNode(value = "from"),
									@NamedAttributeNode(value = "to"),
//									@NamedAttributeNode(value = "guideSteps")
							}
					)
			}
	),
	@NamedEntityGraph(
			name = Trip.MY_LEGS_ENTITY_GRAPH, 
			attributeNodes = { 
					@NamedAttributeNode(value = "itinerary", subgraph = "subgraph.itinerary"),		
			}, subgraphs = {
					@NamedSubgraph(
							name = "subgraph.itinerary",
							attributeNodes = {
									@NamedAttributeNode(value = "legs", subgraph = "subgraph.leg")
							}
					),
					@NamedSubgraph(
							name = "subgraph.leg",
							attributeNodes = {
									@NamedAttributeNode(value = "from"),
									@NamedAttributeNode(value = "to")
							}
					)
			}
	)

})
@Entity
@Table(name = "trip", uniqueConstraints = @UniqueConstraint(name = "trip_itinerary_uc", columnNames= { "itinerary" } ))
@Vetoed
@Access(AccessType.FIELD)
@SequenceGenerator(name = "trip_sg", sequenceName = "trip_id_seq", allocationSize = 1, initialValue = 50)
public class Trip implements Serializable {

	private static final long serialVersionUID = -3789784762166689720L;
	public static final String DETAILED_ENTITY_GRAPH = "detailed-trip-entity-graph";
	public static final String MY_LEGS_ENTITY_GRAPH = "my-legs-trip-entity-graph";
	public static final String URN_PREFIX = PlannerUrnHelper.createUrnPrefix(Trip.class);
	
	public static final String PN_TRIP_USER_YEAR_MONTH_COUNT_MAPPING = "PNTripUserYearMonthCountMapping";
	public static final String PN_TRIP_USER_YEAR_MONTH_COUNT_MODALITY_MAPPING = "PNTripUserYearMonthModalityCountMapping";
	public static final String RGP_1_TRIPS_CREATED_COUNT = "ListTripsCreatedCount";
	public static final String RGP_2_TRIPS_CANCELLED_COUNT = "ListTripsCancelledCount";
	public static final String RGP_3_TRIPS_CANCELLED_BY_PASSENGER_COUNT = "ListTripsCancelledByPassengerCount";
	public static final String RGP_4_TRIPS_CANCELLED_BY_PROVIDER_COUNT = "ListTripsCancelledByProviderCount";
	public static final String RGP_5_TRIPS_WITH_CONFIRMED_RIDESHARE_COUNT = "ListTripsWithConfirmedRideshareCount";
	public static final String RGP_6_TRIPS_WITH_CANCELLED_RIDESHARE_PAYMENT_COUNT = "ListTripsWithCancelledRidesharePaymentCount";
	public static final String RGP_7_MONO_MODAL_TRIPS_COUNT = "ListMonoModalTripsCount";
	public static final String RGP_8_MONO_MODAL_TRIPS_BY_MODALITY_COUNT = "ListMonoModalTripsByModalityCount";
	public static final String RGP_9_MULTI_MODAL_TRIPS_COUNT = "ListMultiModalTripsCount";
	public static final String RGP_10_MULTI_MODAL_TRIPS_BY_MODALITY_COUNT = "ListMultiModalTripsByModalityCount";

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trip_sg")
    private Long id;

    @Transient
    private String tripRef;

    /**
     * The departure location from the original planning request.
     */
    @NotNull
    @Embedded
    @AttributeOverrides({ 
    	@AttributeOverride(name = "label", column = @Column(name = "from_label", length = GeoLocation.MAX_LABEL_LENGTH)), 
    	@AttributeOverride(name = "point", column = @Column(name = "from_point")), 
   	} )
    private GeoLocation from;
    
    /**
     * The arrival location from the original planning request.
     */
    @NotNull
    @Embedded
    @AttributeOverrides({ 
    	@AttributeOverride(name = "label", column = @Column(name = "to_label", length = GeoLocation.MAX_LABEL_LENGTH)), 
    	@AttributeOverride(name = "point", column = @Column(name = "to_point")), 
   	} )
    private GeoLocation to;

    /**
     * The postal code 6 of the departure location.
     */
    @Size(max = 6)
    @Column(name = "departure_postal_code")
    private String departurePostalCode;

    /**
     * The postal code 6 of the arrival location.
     */
    @Size(max = 6)
    @Column(name = "arrival_postal_code")
    private String arrivalPostalCode;

    @NotNull
    @ManyToOne (fetch = FetchType.LAZY)
	@JoinColumn(name = "traveller", nullable = false, foreignKey = @ForeignKey(name = "trip_traveller_fk"))
    private PlannerUser traveller;

    /**
     * A URN reference to the traveller. 
     */
    @Transient
    private String travellerRef;
    

    @Column(name = "state", length = 3)
    private TripState state;

    @OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "itinerary", nullable = false, foreignKey = @ForeignKey(name = "trip_itinerary_fk"))
    private Itinerary itinerary;

    @Transient
    private String itineraryRef;
    
    @Size(max = 256)
    @Column(name = "cancel_reason", length = 256)
    private String cancelReason;
    
    @Column(name = "deleted")
    private Boolean deleted;

    /**
     * In case of rideshare: The number of seats requested.
     */
    @Positive
    @Column(name = "nr_seats")
    private int nrSeats = 1;

    /**
     * If true then the arrival time is more important in this trip.
     * Otherwise departure time is more important.
     */
    @Column(name = "arrival_time_is_pinned")
    private boolean arrivalTimeIsPinned;

    /**
     * If true then the trip is being monitored by the trip monitor.
     */
    @Column(name = "monitored", nullable = false)
    private boolean monitored;

    /**
     * If true then the trip (in fact the booking of one of the legs) was cancelled by the mobility provider.
     */
    @Column(name = "cancelled_by_provider")
    private Boolean cancelledByProvider;


    public Trip() {
    	
    }
    
    public Trip(String itineraryRef) {
    	this.itineraryRef = itineraryRef;
    }
    
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTripRef() {
    	if (tripRef == null) {
    		tripRef = PlannerUrnHelper.createUrn(Trip.URN_PREFIX, getId());
    	}
		return tripRef;
	}

	public PlannerUser getTraveller() {
		return traveller;
	}

	public void setTraveller(PlannerUser traveller) {
		this.traveller = traveller;
	}

	public String getTravellerRef() {
		if (travellerRef == null) {
			travellerRef = UrnHelper.createUrn(PlannerUser.URN_PREFIX, getTraveller().getId());
		}
		return travellerRef;
	}

	public TripState getState() {
		return state;
	}

	public void setState(TripState state) {
		this.state = state;
	}


	public String getCancelReason() {
		return cancelReason;
	}

	public void setCancelReason(String cancelReason) {
		this.cancelReason = cancelReason;
	}

	public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}

	public boolean isDeleted() {
		return Boolean.TRUE.equals(getDeleted());
	}
	public GeoLocation getFrom() {
		return from;
	}

	public void setFrom(GeoLocation from) {
		this.from = from;
	}

	public GeoLocation getTo() {
		return to;
	}

	public void setTo(GeoLocation to) {
		this.to = to;
	}

    public int getNrSeats() {
		return nrSeats;
	}

	public void setNrSeats(int nrSeats) {
		this.nrSeats = nrSeats;
	}

	public Itinerary getItinerary() {
		return itinerary;
	}

	public void setItinerary(Itinerary itinerary) {
		this.itinerary = itinerary;
	}

	public String getItineraryRef() {
		if (itineraryRef == null && itinerary != null) {
			itineraryRef = itinerary.getItineraryRef();
		}
		return itineraryRef;
	}

	public void setItineraryRef(String itineraryRef) {
		this.itineraryRef = itineraryRef;
		this.itinerary = null;
	}

	public boolean isArrivalTimeIsPinned() {
		return arrivalTimeIsPinned;
	}

	public void setArrivalTimeIsPinned(boolean arrivalTimeIsPinned) {
		this.arrivalTimeIsPinned = arrivalTimeIsPinned;
	}

	public boolean isMonitored() {
		return monitored;
	}

	public void setMonitored(boolean monitored) {
		this.monitored = monitored;
	}

	public Boolean getCancelledByProvider() {
		return cancelledByProvider;
	}

	public void setCancelledByProvider(Boolean cancelledByProvider) {
		this.cancelledByProvider = cancelledByProvider;
	}

	public String getDeparturePostalCode() {
		return departurePostalCode;
	}

	public void setDeparturePostalCode(String departurePostalCode) {
		this.departurePostalCode = departurePostalCode;
	}

	public String getArrivalPostalCode() {
		return arrivalPostalCode;
	}

	public void setArrivalPostalCode(String arrivalPostalCode) {
		this.arrivalPostalCode = arrivalPostalCode;
	}

	private String formatTime(Instant instant) {
    	return DateTimeFormatter.ISO_TIME.format(instant.atZone(ZoneId.systemDefault()).toLocalDateTime());
    }

	public String toStringCompact() {
		return String.format("Trip %d %s %s D %s A %s %s from %s to %s",
				getId(), traveller.getEmail(), state.name(), 
				formatTime(itinerary.getDepartureTime()), formatTime(itinerary.getArrivalTime()),
				itinerary.getDuration() == null ? "" : Duration.ofSeconds(itinerary.getDuration()).toString(),
				getFrom().toString(), getTo().toString());
	}

	@Override
	public String toString() {
		return String.format("%s\n\t%s", toStringCompact(), itinerary.toStringCompact());
	}

    /**
     * Assigns the lowest leg state (in ordinal terms) to the overall trip state.
     * If there are no legs then the state remains as is.
     * The new state cannot be lower than the current trip state
     */
   	public void updateTripState() {
   		if (getItinerary() == null || getItinerary().getLegs() == null) {
   			return;
   		}
   		Optional<Leg> minleg = getItinerary().getLegs().stream().min(Comparator.comparingInt(leg -> leg.getState().ordinal()));
   		if (minleg.isPresent() && minleg.get().getState().ordinal() > getState().ordinal()) {
			setState(minleg.get().getState());
   		}
   	}

    /**
     * Assigns the current trip state to all legs, if any, but only if the leg state is 
     * lower in terms of the ordinal. A trip with a cancelled leg can never be set to completed.  
     */
   	public void forceTripStateDown() {
   		if (getItinerary() == null || getItinerary().getLegs() == null) {
   			return;
   		}
   		for (Leg leg: getItinerary().getLegs()) {
   			if (leg.getState().ordinal() < getState().ordinal()) {
   				leg.setState(getState());
   			}
   		}
   	}

   	public Set<String> getAgencies() {
    	Set<String> ags = new LinkedHashSet<>();
		for (Leg leg: getItinerary().getLegs()) {
			if (leg.getTraverseMode() == TraverseMode.WALK) {
				continue;
			} else if (leg.getTraverseMode() == TraverseMode.RIDESHARE) {
				ags.add(leg.getDriverName());
			} else {
				ags.add(leg.getAgencyName());
			}
		}
    	return ags;
    }


}
