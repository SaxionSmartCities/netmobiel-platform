package eu.netmobiel.planner.filter;

import java.time.OffsetDateTime;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.filter.PeriodFilter;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.planner.model.PlannerUser;

public class ShoutOutFilter extends PeriodFilter {
	public static final Integer DEFAULT_DEP_ARR_RADIUS = 10000;
	
	/**
	 * The driver asking for the shout-outs. Used to filter shout-outs issued by the driver (when he/she is a passenger too).
	 */
	private PlannerUser caller;
	/**
	 * The reference location of the driver asking for the shout-outs.
	 */
	private GeoLocation location;
	/**
	 * The radius of the small circle containing at least departure or arrival location of the traveller.
	 */
	private Integer depArrRadius;
	/**
	 * The radius of the larger circle containing both departure and arrival location of the traveller.
	 */
	private Integer travelRadius;
	/**
	 * Include active shout-outs only if true (shout-outs without fulfillment).
	 */
	private boolean inProgressOnly;

	public ShoutOutFilter() {
	}

	public ShoutOutFilter(PlannerUser caller, String location, Integer depArrRadius, Integer travelRadius, 
			OffsetDateTime since, OffsetDateTime until, Boolean inProgressOnly, String sortDir) {
		this.caller = caller;
		setLocation(location);
		this.depArrRadius = depArrRadius;
		this.travelRadius = travelRadius;
		setSince(since);
		setUntil(until);
		// Default true
		this.inProgressOnly = !Boolean.FALSE.equals(inProgressOnly);
		setSortDir(sortDir);
	}

	public PlannerUser getCaller() {
		return caller;
	}

	public void setCaller(PlannerUser caller) {
		this.caller = caller;
	}

	public GeoLocation getLocation() {
		return location;
	}

	public void setLocation(GeoLocation location) {
		this.location = location;
	}

	public final void setLocation(String location) {
		if (location != null) {
			this.location = GeoLocation.fromString(location);
		}
	}

	public Integer getDepArrRadius() {
		return depArrRadius;
	}

	public void setDepArrRadius(Integer depArrRadius) {
		this.depArrRadius = depArrRadius;
	}

	public Integer getTravelRadius() {
		return travelRadius;
	}

	public void setTravelRadius(Integer travelRadius) {
		this.travelRadius = travelRadius;
	}

	public boolean isInProgressOnly() {
		return inProgressOnly;
	}

	public void setInProgressOnly(boolean inProgressOnly) {
		this.inProgressOnly = inProgressOnly;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(location.toString());
		builder.append(", ");
		builder.append("dar=");
		builder.append(depArrRadius);
		builder.append(", ");
		builder.append("tr=");
		builder.append(travelRadius);
		builder.append(", ");
		builder.append("po=");
		builder.append(inProgressOnly);
		builder.append(", ");
		builder.append(super.toString());
		return builder.toString();
	}

	@Override
	public void validate() throws BadRequestException {
		super.validate();
		if (depArrRadius == null) {
			depArrRadius = DEFAULT_DEP_ARR_RADIUS;
		}
		if (travelRadius == null) {
			travelRadius = DEFAULT_DEP_ARR_RADIUS;
		}
		if (depArrRadius > travelRadius) {
			throw new BadRequestException("DepArrRadius must be smaller than or equal to travelRadius");
		}
	}
}
