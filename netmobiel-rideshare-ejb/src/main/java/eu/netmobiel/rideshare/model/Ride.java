package eu.netmobiel.rideshare.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import javax.persistence.OrderBy;
import javax.persistence.OrderColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Size;

import eu.netmobiel.rideshare.util.RideshareUrnHelper;

/**
 * A Ride is simple or recurrent. A recurrent Ride is copied from a template. A simple Ride has no template.
 *  
 * @author Jaap Reitsma
 *
 */
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
public class Ride extends RideBase implements Serializable {
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
     * A recurrent ride has a ride template, shared by multiple rides. The pattern is used to instantiate new rides from the template(s), but also 
     * the recognize the instances that were derived from the same template.
     * A new template is saved before the ride is saved. Existing templates are not touched.
     */
    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ride_template", nullable = true, foreignKey = @ForeignKey(name = "ride_ride_template_fk"))
    private RideTemplate rideTemplate;


    /**
     * The reason for cancelling a ride.
     */
    @Size(max = 256)
    @Column(name = "cancel_reason", length = 256)
    private String cancelReason;
    
    /**
     * If true the ride is soft-deleted. If a ride was only planned (no bookings), it will be hard deleted.
     */
    @Column(name = "deleted")
    private Boolean deleted;

    /**
     * The bookings on this ride. Currently at most one.
     */
    @OneToMany (mappedBy = "ride", fetch = FetchType.LAZY)
    private List<Booking> bookings;

    /**
     * The stops (vertices) in this ride. The stops are ordered.
     */
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@JoinColumn(name = "ride", foreignKey = @ForeignKey(name = "stop_ride_fk"), nullable = false)
	@OrderColumn(name = "stop_ix")
	private List<Stop> stops;

	/**
     * The legs (edges) in this ride. The legs are ordered.
     */
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@JoinColumn(name = "ride", foreignKey = @ForeignKey(name = "leg_ride_fk"), nullable = false)
	@OrderBy("legIx asc")
	private List<Leg> legs;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public RideTemplate getRideTemplate() {
		return rideTemplate;
	}

	public void setRideTemplate(RideTemplate rideTemplate) {
		this.rideTemplate = rideTemplate;
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

	public boolean isDeleted() {
		return Boolean.TRUE == getDeleted();
	}
	
	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}

	public List<Booking> getBookings() {
		if (bookings == null) {
			bookings = new ArrayList<>();
		}
		return bookings;
	}

	public void setBookings(List<Booking> bookings) {
		this.bookings = bookings;
	}

	public List<Stop> getStops() {
		if (stops == null) {
			stops = new ArrayList<>();
		}
		return stops;
	}

	public void setStops(List<Stop> stops) {
		this.stops = stops;
	}

	public List<Leg> getLegs() {
		if (legs == null) {
			legs = new ArrayList<>();
		}
		return legs;
	}

	public void setLegs(List<Leg> legs) {
		this.legs = legs;
	}

	public String getRideRef() {
		if (rideRef == null) {
    		rideRef = RideshareUrnHelper.createUrn(Ride.URN_PREFIX, getId());
		}
		return rideRef;
	}

	/**
	 * Returns true if the specified ride overlaps in time with this ride.
	 * @param r the ride to compare.
	 * @return true if there is an overlap in trip time
	 */
	public boolean hasTemporalOverlap(Ride r) {
		return r.getDepartureTime().isBefore(getArrivalTime()) && r.getArrivalTime().isAfter(getDepartureTime());
	}

    private String formatTime(Instant instant) {
    	return DateTimeFormatter.ISO_INSTANT.format(instant);
    }

    @Override
    public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Ride D ");
		builder.append(formatTime(getDepartureTime())).append(" A ");
		builder.append(formatTime(getArrivalTime())).append(" ");
		builder.append(getDuration()).append("s ");
		builder.append(getDistance()).append("m ");
		if (legs != null) {
			Stop previous = null;
			for (Leg leg : legs) {
				if (previous == null) {
					builder.append("\n\t\t").append(leg.getFrom());
				} else if (! previous.equals(leg.getFrom())) {
					builder.append("\n\t\t").append(leg.getFrom());
				}
				builder.append("\n\t\t\t").append(leg);
				builder.append("\n\t\t").append(leg.getTo());
				previous = leg.getTo();
			}
		}
		return builder.toString();
    }
}	
