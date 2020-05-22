package eu.netmobiel.rideshare.model;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

@Embeddable
@Vetoed
@Access(AccessType.FIELD) // Add this annotation, otherwise no JPA ModelGen attribute is generated for
							// Point.
public class Recurrence implements Serializable {

	private static final long serialVersionUID = -9081655274003595412L;

	public static final String DEFAULT_TIME_ZONE = "Europe/Amsterdam";

	/**
	 * As long as the recurrence is active, new rides will be inserted into the
	 * database, with a moving horizon of two months. This attribute specifies an
	 * absolute horizon after which the recurrence stops.
	 * 
	 */
	@Column(name = "recurrence_horizon")
	private Instant horizon;

	/**
	 * The horizon as local date. This attribute specifies the local horizon after
	 * which the recurrence stops.
	 * 
	 */
	@Transient
	private LocalDate localHorizon;

	/**
	 * The repetition interval. One week means every week. Two weeks means every two
	 * weeks. Interval is a reserved word in Postgresql.
	 */

	@Column(name = "recurrence_interval")
	private Integer interval;

	/**
	 * The unit of the interval.
	 */
	@Column(name = "recurrence_unit", length = 1)
	private TimeUnit unit;

	/**
	 * In case of unit equals WEEK: The applicable days of the week as a bit mask.
	 * Bit 0 is week day 1 according to ISO 8601 (monday is day 1).
	 */
	@Column(name = "recurrence_dsow", length = 1)
	private Byte daysOfWeekMask;

	/**
	 * The zone to calculate the local date. If not set Europe/Amsterdam time zone
	 * is used.
	 */
	@Column(name = "recurrence_time_zone", length = 32)
	private String timeZone;

	public Recurrence() {
	}

	public Recurrence(Integer anInterval) {
		this(anInterval, TimeUnit.DAY, null, (Instant) null);
	}

	public Recurrence(Integer anInterval, LocalDate aHorizon) {
		this(anInterval, TimeUnit.DAY, null, aHorizon);
	}

	public Recurrence(Integer anInterval, Instant aHorizon) {
		this(anInterval, TimeUnit.DAY, null, aHorizon);
	}

	public Recurrence(Integer anInterval, Byte someDaysOfWeekMask) {
		this(anInterval, TimeUnit.WEEK, someDaysOfWeekMask, (Instant) null);
	}

	public Recurrence(Integer anInterval, Byte someDaysOfWeekMask, LocalDate aHorizon) {
		this(anInterval, TimeUnit.WEEK, someDaysOfWeekMask, aHorizon);
	}

	public Recurrence(Integer anInterval, TimeUnit aUnit, Byte someDaysOfWeekMask, LocalDate aHorizon) {
		this.interval = anInterval;
		this.unit = aUnit;
		this.daysOfWeekMask = someDaysOfWeekMask;
		this.horizon = localToInstant(aHorizon);
	}

	public Recurrence(Integer anInterval, TimeUnit aUnit, Byte someDaysOfWeekMask, Instant aHorizon) {
		this.interval = anInterval;
		this.unit = aUnit;
		this.daysOfWeekMask = someDaysOfWeekMask;
		this.horizon = aHorizon;
	}

	public static byte dowMask(DayOfWeek... dows) {
		byte mask = 0x0;
		for (DayOfWeek dow : dows) {
			mask |= (byte) (1 << (dow.getValue() - 1));
		}
		return mask;
	}

	private LocalDate instantToLocal(Instant instant) {
		return instant == null ? null : instant.atZone(ZoneId.of(this.getTimeZone())).toLocalDate();
	}

	private Instant localToInstant(LocalDate localDate) {
		return localDate == null ? null : localDate.atStartOfDay().atZone(ZoneId.of(getTimeZone())).toInstant();
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


	public Byte getDaysOfWeekMask() {
		return daysOfWeekMask;
	}

	public void setDaysOfWeekMask(Byte daysOfWeekmask) {
		this.daysOfWeekMask = daysOfWeekmask;
	}

	public final String getTimeZone() {
		return timeZone != null ? timeZone : DEFAULT_TIME_ZONE;
	}

	public final void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	public Instant getHorizon() {
		return horizon;
	}

	public void setHorizon(Instant horizon) {
		this.horizon = horizon;
		this.localHorizon = null;
	}

	public LocalDate getLocalHorizon() {
		if (localHorizon == null && horizon != null) {
			localHorizon = instantToLocal(horizon);
		}
		return localHorizon;
	}

	public void setLocalHorizon(LocalDate localHorizon) {
		this.localHorizon = localHorizon;
		this.horizon = localToInstant(localHorizon);
	}

	public LocalDateTime getLocalDateTime(Instant time) {
		return time.atZone(ZoneId.of(this.getTimeZone())).toLocalDateTime();
	}

	@Override
	public int hashCode() {
		return Objects.hash(daysOfWeekMask, horizon, interval, timeZone, unit);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Recurrence other = (Recurrence) obj;
		return Objects.equals(daysOfWeekMask, other.daysOfWeekMask) && Objects.equals(horizon, other.horizon)
				&& Objects.equals(interval, other.interval) && Objects.equals(timeZone, other.timeZone)
				&& unit == other.unit;
	}

}
