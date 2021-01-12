package eu.netmobiel.rideshare.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.vividsolutions.jts.geom.MultiPoint;

@Entity
@Vetoed
@Table(name = "ride_template")
@SequenceGenerator(name = "ride_template_sg", sequenceName = "ride_template_id_seq", allocationSize = 1, initialValue = 50)
public class RideTemplate extends RideBase implements Serializable {
	private static final long serialVersionUID = -6389728915295371839L;
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ride_template_sg")
    private Long id;

    /**
     * The recurrence pattern of the template.
     */
    @Embedded
    private Recurrence recurrence;

    /**
     * The leg's geometry. The template has no leg, put it here instead. 
     */
	@Basic			// Add this annotation, otherwise no JPA ModelGen attribute is generated.
    @Column(name = "leg_geometry", nullable = true)
    private MultiPoint legGeometry; 

    public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}

	public Recurrence getRecurrence() {
		return recurrence;
	}
	public void setRecurrence(Recurrence recurrence) {
		this.recurrence = recurrence;
	}
	public MultiPoint getLegGeometry() {
		return legGeometry;
	}
	public void setLegGeometry(MultiPoint legGeometry) {
		this.legGeometry = legGeometry;
	}
	
	/**
     * Instantiates a new ride from an existing template. 
     * @return The new ride. 
     */
    public Ride createRide() {
    	Ride r = new Ride();
    	RideBase.copy(this, r);
		r.setRideTemplate(this);
		r.setState(RideState.SCHEDULED);

		// Create the simple single leg structure for a driver.
		// Add stops and leg and connect them
		Stop from = new Stop(getFrom(), getDepartureTime(), null);
		r.addStop(from);
		Stop to = new Stop(getTo(), null, getArrivalTime());
		r.addStop(to);
		Leg leg = new Leg();
		leg.setFrom(from);
		leg.setTo(to);
		leg.setDistance(getDistance());
		leg.setDuration(getDuration());
		leg.setLegGeometry(getLegGeometry());
		leg.setLegIx(0);
		r.addLeg(leg);
		
		return r;
	}

	protected void advanceTemplateState(LocalDate theNextDate, ZoneId myZone, LocalTime theTime) {
    	int duration = getDuration();
		Instant newTravelTime = LocalDateTime
				.of(theNextDate, theTime)
				.atZone(myZone)
				.toInstant();
		if (isArrivalTimePinned()) {
			setDepartureTime(newTravelTime.minusSeconds(duration));
			setArrivalTime(newTravelTime);
		} else {
			setDepartureTime(newTravelTime);
			setArrivalTime(newTravelTime.plusSeconds(duration));
		}
	}

	/**
	 * Generate a set of rides. The template maintains the last reference departure (and arrival) date. Recurrence will
	 * start from there. The actual generation will start after the start date.
	 * No generation will take place if start date is after template or system horizon. 
	 * @param horizon the time instant (converted to local date) beyond which no rides 
	 * 			are generated (horizon date is first date to exclude).
	 * @return A list of rides according the recurrence pattern.
	 */
    public List<Ride> generateRides(Instant systemHorizon) {
    	ZoneId myZone = ZoneId.of(recurrence.getTimeZone());
    	Instant travelTime = isArrivalTimePinned() ? getArrivalTime() : getDepartureTime();
		LocalDateTime reference = travelTime.atZone(myZone).toLocalDateTime();
    	List<Ride> rides = new ArrayList<>();
    	RecurrenceIterator rix = new RecurrenceIterator(recurrence, reference.toLocalDate(), null, systemHorizon.atZone(myZone).toLocalDate());
    	while (rix.hasNext()) {
			advanceTemplateState(rix.next(), myZone, reference.toLocalTime());
			Ride r = createRide();
			rides.add(r);
		}
		advanceTemplateState(rix.next(), myZone, reference.toLocalTime());
    	return rides;
    }

    /**
     * Takes the recurrence pattern and calculates the first valid travel time.
     * @param travelTime the initial travel time.
     * @return the first possible travel time matching the recurrence pattern.
     * 
     */
    public Instant snapTravelTimeToPattern(Instant travelTime) {
    	ZoneId myZone = ZoneId.of(recurrence.getTimeZone());
		LocalDateTime reference = travelTime.atZone(myZone).toLocalDateTime();
    	RecurrenceIterator rix = new RecurrenceIterator(recurrence, reference.toLocalDate(), null, null);
    	if (! rix.hasNext()) {
    		throw new IllegalStateException("Cannot snap the travel time to the pattern!?");
    	}
    	return LocalDateTime.of(rix.next(), reference.toLocalTime())
				.atZone(myZone)
				.toInstant();
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
		return String.format("Ride %s --> %s %s A %s %d s %d m", 
				getFrom(), getTo(), 
				formatTime(getDepartureTime()), formatTime(getArrivalTime()), 
				getDuration(), getDistance());
    }
}
