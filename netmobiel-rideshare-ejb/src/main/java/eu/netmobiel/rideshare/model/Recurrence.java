package eu.netmobiel.rideshare.model;

import java.io.Serializable;
import java.time.LocalDate;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
@Vetoed
@Access(AccessType.FIELD)	// Add this annotation, otherwise no JPA ModelGen attribute is generated for Point.
public class Recurrence implements Serializable {

	private static final long serialVersionUID = -9081655274003595412L;

	/**
	 * As long as the recurrence is active, new rides will be inserted into the database, with a moving horizon of two months.
	 * This attribute specified an absolute horizon after which the recurrence stops.
	 * 
	 */
    @Column(name = "recurrence_horizon")
	private LocalDate horizon;
	
	/**
	 * The repetition interval. One week means every week. Two weeks means every two weeks.
	 * Interval is a reserved word in Postgresql.
	 */
    
    @Column(name = "recurrence_interval")
	private Integer interval;
	
	/**
	 * The unit of the interval.
	 */
    @Column(name = "recurrence_unit", length = 1)
	private TimeUnit unit;

	/**
	 * In case of unit equals WEEK: The applicable days of the week as a bit mask. Bit 0 is week day 1 according
	 * to ISO 8601 (monday is day 1).  
	 */
    @Column(name = "recurrence_dsow", length = 1)
	private Byte daysOfWeekMask; 

    public Recurrence() {
    }

    public Recurrence(Integer anInterval) {
    	this(anInterval, TimeUnit.DAY, null, null);
    }
    public Recurrence(Integer anInterval, LocalDate aHorizon) {
    	this(anInterval, TimeUnit.DAY, null, aHorizon);
    }
    public Recurrence(Integer anInterval, Byte someDaysOfWeekMask) {
    	this(anInterval, TimeUnit.WEEK, someDaysOfWeekMask, null);
    }
    public Recurrence(Integer anInterval, Byte someDaysOfWeekMask, LocalDate aHorizon) {
    	this(anInterval, TimeUnit.WEEK, someDaysOfWeekMask, aHorizon);
    }

    public Recurrence(Integer anInterval, TimeUnit aUnit, Byte someDaysOfWeekMask, LocalDate aHorizon) {
    	this.interval = anInterval;
    	this.unit = aUnit;
    	this.daysOfWeekMask = someDaysOfWeekMask;
    	this.horizon = aHorizon;
    }

	public Integer getInterval() {
		return interval;
	}
	public void setInterval(Integer interval) {
		this.interval = interval;
	}
	public TimeUnit getUnit() {
		return unit;
	}
	public void setUnit(TimeUnit unit) {
		this.unit = unit;
	}
	public LocalDate getHorizon() {
		return horizon;
	}
	public void setHorizon(LocalDate horizon) {
		this.horizon = horizon;
	}
	public Byte getDaysOfWeekMask() {
		return daysOfWeekMask;
	}
	public void setDaysOfWeekMask(Byte daysOfWeekmask) {
		this.daysOfWeekMask = daysOfWeekmask;
	}
	
}
