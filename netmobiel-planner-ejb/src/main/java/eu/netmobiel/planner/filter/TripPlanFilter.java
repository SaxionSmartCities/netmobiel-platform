package eu.netmobiel.planner.filter;

import java.time.OffsetDateTime;

import eu.netmobiel.commons.filter.PeriodFilter;
import eu.netmobiel.planner.model.PlanType;
import eu.netmobiel.planner.model.PlannerUser;

public class TripPlanFilter extends PeriodFilter {
	/**
	 * The traveller owning the trip plans.
	 */
	private PlannerUser traveller;

	/**
	 * Include active plans if true (shout-outs without fulfillment). This parameter implies planType SHOUT_OUT.
	 * If false, all completed plans are returned, including fulfilled shout-outs. 
	 */
	private Boolean inProgress;

	/**
	 * The type of plan to retrieve
	 */
	private PlanType planType;
	
	public TripPlanFilter() {
	}

	public TripPlanFilter(PlannerUser traveller, OffsetDateTime since, OffsetDateTime until, 
			String planType, Boolean inProgress, String sortDir) {
		this.traveller = traveller;
		setSince(since);
		setUntil(until);
		this.planType = planType == null ? null : PlanType.valueOf(planType);
		// Default false
		this.inProgress = inProgress;
		setSortDir(sortDir);
	}

	public PlannerUser getTraveller() {
		return traveller;
	}

	public void setTraveller(PlannerUser traveller) {
		this.traveller = traveller;
	}

	public PlanType getPlanType() {
		return planType;
	}

	public void setPlanType(PlanType planType) {
		this.planType = planType;
	}


	public Boolean getInProgress() {
		return inProgress;
	}

	public void setInProgress(Boolean inProgress) {
		this.inProgress = inProgress;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (planType != null) {
			builder.append("pt=");
			builder.append(planType);
			builder.append(" ");
		}
		if (inProgress != null) {
			builder.append("pr=");
			builder.append(inProgress);
			builder.append(" ");
		}
		builder.append(super.toString());
		return builder.toString();
	}
}