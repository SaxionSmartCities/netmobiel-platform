package eu.netmobiel.planner.filter;

import java.time.OffsetDateTime;

import eu.netmobiel.commons.filter.PeriodFilter;
import eu.netmobiel.planner.model.PlannerUser;
import eu.netmobiel.planner.model.TripState;

public class TripFilter extends PeriodFilter {
	/**
	 * The traveller owning the trip plans.
	 */
	private PlannerUser traveller;

	/**
	 * The trip state to filter on.
	 */
	private TripState tripState;

	/**
	 * Included delete trips too. 
	 */
	private Boolean deletedToo;
	/**
	 * Do not include cancelled trips. 
	 */
	private Boolean skipCancelled;

	public TripFilter() {
	}

	public TripFilter(PlannerUser traveller, OffsetDateTime since, OffsetDateTime until, 
			String state, Boolean deletedToo, Boolean skipCancelled,  String sortDir) {
		this.traveller = traveller;
		setSince(since);
		setUntil(until);
		this.tripState = state == null ? null : TripState.valueOf(state);
		// Default false
		this.deletedToo = deletedToo;
		this.skipCancelled = skipCancelled;
		setSortDir(sortDir);
	}

	public PlannerUser getTraveller() {
		return traveller;
	}

	public void setTraveller(PlannerUser traveller) {
		this.traveller = traveller;
	}


	public TripState getTripState() {
		return tripState;
	}

	public void setTripState(TripState tripState) {
		this.tripState = tripState;
	}

	public Boolean getDeletedToo() {
		return deletedToo;
	}

	public void setDeletedToo(Boolean deletedToo) {
		this.deletedToo = deletedToo;
	}

	public Boolean getSkipCancelled() {
		return skipCancelled;
	}

	public void setSkipCancelled(Boolean skipCancelled) {
		this.skipCancelled = skipCancelled;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (tripState != null) {
			builder.append("ts=");
			builder.append(tripState);
			builder.append(" ");
		}
		if (deletedToo != null) {
			builder.append("dt=");
			builder.append(deletedToo);
			builder.append(" ");
		}
		if (skipCancelled != null) {
			builder.append("sc=");
			builder.append(skipCancelled);
			builder.append(" ");
		}
		builder.append(super.toString());
		return builder.toString();
	}
}