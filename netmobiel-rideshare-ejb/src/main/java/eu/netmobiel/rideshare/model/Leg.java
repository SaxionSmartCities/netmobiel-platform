package eu.netmobiel.rideshare.model;


import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.vividsolutions.jts.geom.MultiPoint;

import eu.netmobiel.commons.api.EncodedPolylineBean;
import eu.netmobiel.commons.util.GeometryHelper;
import eu.netmobiel.commons.util.PolylineEncoder;
import eu.netmobiel.rideshare.util.RideshareUrnHelper;

/**
 * One leg of a trip -- that is, a temporally continuous piece of the journey that takes place on a
 * particular vehicle (or on foot).
 */

@Entity
@Table(name = "leg")
@Vetoed
@SequenceGenerator(name = "leg_sg", sequenceName = "leg_id_seq", allocationSize = 1, initialValue = 50)
public class Leg implements Serializable {
	private static final long serialVersionUID = -3789784762166689720L;
	public static final String URN_PREFIX = RideshareUrnHelper.createUrnPrefix("leg");

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "leg_sg")
    private Long id;

    @Transient
    private String legRef;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ride", foreignKey = @ForeignKey(name = "leg_ride_fk"), nullable = false)
	private Ride ride;

	/**
	 * Ordering of the legs.
	 */
	@Column(name = "leg_ix")
	private Integer legIx;

	/**
     * The distance travelled while traversing the leg in meters.
     */
    @Basic
    private Integer distance;
    
    /**
     * The duration of the leg in seconds (in general arrivalTime - departureTime).
     */
    @Basic
    private Integer duration;

    /**
    * The Place where the leg originates. Note: 'from' is a reserved keyword in Postgres.
    */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "from_stop", foreignKey = @ForeignKey(name = "leg_from_stop_fk"), nullable = false)
    private Stop from;
   
   /**
    * The Place where the leg begins.
    */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "to_stop", foreignKey = @ForeignKey(name = "leg_to_stop_fk"), nullable = false)
    private Stop to;

    /**
     * The leg's geometry.  
     */
	@Basic			// Add this annotation, otherwise no JPA ModelGen attribute is generated.
    @Column(name = "leg_geometry", nullable = true)
    private MultiPoint legGeometry; 

    /**
     * The leg's geometry as encoded polyline bean.  
     */
    @Transient
    private EncodedPolylineBean legGeometryEncoded; 

    /**
     * The legs this booking is involved in.
     */
    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinTable(
    		name = "booked_legs",
    		joinColumns = @JoinColumn(name= "leg", foreignKey = @ForeignKey(name = "booked_legs_leg_fk")),
    		inverseJoinColumns = @JoinColumn(name = "booking", foreignKey = @ForeignKey(name = "booked_legs_booking_fk"))
    )    
    private List<Booking> bookings;

    public Leg() {
    }

    public Leg(Stop aFrom, Stop aTo) {
		this.from = aFrom;
		this.to = aTo;
    }

    public Leg(Leg other) {
		this.distance = other.distance;
		this.duration = other.duration;
		this.from = other.from.copy();
		this.to = other.to.copy();
		this.legGeometry = other.legGeometry;
	}

    public Leg copy() {
    	return new Leg(this);
    }
    
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getLegRef() {
		if (legRef == null) {
    		legRef = RideshareUrnHelper.createUrn(Leg.URN_PREFIX, getId());
		}
		return legRef;
	}

	public Ride getRide() {
		return ride;
	}

	public void setRide(Ride ride) {
		this.ride = ride;
	}

	public Integer getLegIx() {
		return legIx;
	}

	public void setLegIx(Integer legIx) {
		this.legIx = legIx;
	}

	public Integer getDistance() {
		return distance;
	}

	public void setDistance(Integer distance) {
		this.distance = distance;
	}

	public Integer getDuration() {
		return duration;
	}

	public void setDuration(Integer duration) {
		this.duration = duration;
	}

	public Stop getFrom() {
		return from;
	}

	public void setFrom(Stop from) {
		this.from = from;
	}

	public Stop getTo() {
		return to;
	}

	public void setTo(Stop to) {
		this.to = to;
	}

	public Instant getStartTime() {
		return getFrom().getDepartureTime();
	}

	public Instant getEndTime() {
		return getTo().getArrivalTime();
	}

	public MultiPoint getLegGeometry() {
		return legGeometry;
	}

	public void setLegGeometry(MultiPoint  legGeometry) {
		this.legGeometry = legGeometry;
		this.legGeometryEncoded = null;
	}

	public EncodedPolylineBean getLegGeometryEncoded() {
    	if (legGeometryEncoded == null && legGeometry != null) {
    		legGeometryEncoded = PolylineEncoder.createEncodings(legGeometry);
    	}
		return legGeometryEncoded;
	}

	public void setLegGeometryEncoded(EncodedPolylineBean legGeometryEncoded) {
		this.legGeometryEncoded = legGeometryEncoded;
		this.legGeometry = GeometryHelper.createLegGeometry(legGeometryEncoded);
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

	/**
	 * Using the database ID as equals test!
	 * @see https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/
	 */
	@Override
    public boolean equals(Object o) {
        if (this == o) {
        	return true;
        }
        if (!(o instanceof Leg)) {
            return false;
        }
        Leg other = (Leg) o;
        return id != null && id.equals(other.getId());
    }

	/**
	 * Using the database ID as equals test!
	 * @see https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/
	 */
    @Override
    public int hashCode() {
        return 31;
    }
    
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Leg ");
		builder.append(id).append(" ");
		builder.append(duration).append("s ");
		builder.append(distance).append("m ");
		return builder.toString();
	}
    
}