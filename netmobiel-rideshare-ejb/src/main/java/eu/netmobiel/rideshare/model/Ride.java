package eu.netmobiel.rideshare.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import javax.enterprise.inject.Vetoed;
import javax.persistence.CascadeType;
import javax.persistence.Column;
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
import javax.persistence.NamedSubgraph;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import eu.netmobiel.rideshare.util.RideshareUrnHelper;

@NamedEntityGraph(
	name = Ride.SEARCH_RIDES_ENTITY_GRAPH, 
	attributeNodes = { 
			@NamedAttributeNode(value = "rideTemplate", subgraph = "template-details")		
	}, subgraphs = {
			@NamedSubgraph(
					name = "template-details",
					attributeNodes = {
							@NamedAttributeNode(value = "car"),
							@NamedAttributeNode(value = "driver")
					}
				)
			}

)
@NamedEntityGraph(
	name = Ride.BOOKINGS_ENTITY_GRAPH, 
	attributeNodes = { 
		@NamedAttributeNode(value = "rideTemplate", subgraph = "template-details"),		
		@NamedAttributeNode(value = "bookings", subgraph = "booking-passenger-details")		
	}, subgraphs = {
			@NamedSubgraph(
					name = "template-details",
					attributeNodes = {
							@NamedAttributeNode(value = "car"),
							@NamedAttributeNode(value = "driver")
					}
				),
			@NamedSubgraph(
					name = "booking-passenger-details",
					attributeNodes = {
						@NamedAttributeNode(value = "passenger")
					}
				)
			
	}
)
@Entity
@Table(name = "ride")
@Vetoed
@SequenceGenerator(name = "ride_sg", sequenceName = "ride_id_seq", allocationSize = 1, initialValue = 50)
public class Ride implements Serializable {
	private static final long serialVersionUID = 4342765799358026502L;
	public static final String URN_PREFIX = RideshareUrnHelper.createUrnPrefix("ride");
	public static final String SEARCH_RIDES_ENTITY_GRAPH = "search-rides-graph";
	public static final String BOOKINGS_ENTITY_GRAPH = "bookings-graph";

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ride_sg")
    private Long id;

    @Transient
    private String rideRef;

    /**
     * A ride template is shared by multiple rides. The pattern is used to instantiate new rides from the template(s), but also 
     * the recognize the instances that were derived from the same template.
     */
    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
	@JoinColumn(name = "ride_template", nullable = true, foreignKey = @ForeignKey(name = "ride_ride_template_fk"))
    private RideTemplate rideTemplate;

    @NotNull
    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;
    
    @NotNull
    @Column(name = "estimated_arrival_time", nullable = false)
    private LocalDateTime estimatedArrivalTime;

    @Column(name = "deleted")
    private Boolean deleted;

    @OneToMany (mappedBy = "ride", fetch = FetchType.LAZY)
    private List<Booking> bookings;

    @OneToMany(mappedBy = "ride", fetch = FetchType.LAZY )
    @OrderColumn(name = "stops_order")
    private List<Stop> stops;
    
    public RideTemplate getRideTemplate() {
		return rideTemplate;
	}

	public void setRideTemplate(RideTemplate rideTemplate) {
		this.rideTemplate = rideTemplate;
	}

	public List<Stop> getStops() {
		return stops;
	}

	public void setStops(List<Stop> stops) {
		this.stops = stops;
	}

	public void setBookings(List<Booking> bookings) {
		this.bookings = bookings;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public List<Booking> getBookings() {
		return bookings;
	}

	public LocalDateTime getDepartureTime() {
		return departureTime;
	}

	public void setDepartureTime(LocalDateTime departure) {
		this.departureTime = departure;
	}

	public LocalDateTime getEstimatedArrivalTime() {
		return estimatedArrivalTime;
	}

	public void setEstimatedArrivalTime(LocalDateTime estimatedArrivalTime) {
		this.estimatedArrivalTime = estimatedArrivalTime;
	}

	public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}

	public String getRideRef() {
		if (rideRef == null) {
    		rideRef = RideshareUrnHelper.createUrn(Ride.URN_PREFIX, getId());
		}
		return rideRef;
	}

	public void updateEstimatedArrivalTime() {
		if (getDepartureTime() == null || getRideTemplate() == null ) {
			throw new IllegalStateException("Departure time and template must be set");
		}
		if (getRideTemplate().getEstimatedDrivingTime() != null) {
			setEstimatedArrivalTime(getDepartureTime().plusSeconds(getRideTemplate().getEstimatedDrivingTime()));
		}
	}
	
	/**
     * Instantiates a new ride from an existing template. 
     * @return The new ride. 
     */
    public static Ride createRide(RideTemplate template, LocalDateTime departure) {
    	Ride c = new Ride();
		c.departureTime = departure;
		c.rideTemplate = template;
		c.updateEstimatedArrivalTime();
		return c;
	}


}	
