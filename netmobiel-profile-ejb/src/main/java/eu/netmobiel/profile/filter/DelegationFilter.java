package eu.netmobiel.profile.filter;

import java.time.OffsetDateTime;

import eu.netmobiel.commons.filter.PeriodFilter;
import eu.netmobiel.profile.model.Profile;

public class DelegationFilter extends PeriodFilter {
	private Profile delegate;
	private Profile delegator;
	private boolean inactiveToo;
	
	public DelegationFilter() {
	}

	public DelegationFilter(Profile delegate, Profile delegator, OffsetDateTime since, OffsetDateTime until, boolean inactiveToo) {
		this.delegate = delegate;
		this.delegator = delegator;
		setSince(since);
		setUntil(until);
		this.inactiveToo = inactiveToo;
	}

	public Profile getDelegate() {
		return delegate;
	}

	public void setDelegate(Profile delegate) {
		this.delegate = delegate;
	}

	public Profile getDelegator() {
		return delegator;
	}

	public void setDelegator(Profile delegator) {
		this.delegator = delegator;
	}

	public boolean isInactiveToo() {
		return inactiveToo;
	}

	public void setInactiveToo(boolean inactiveToo) {
		this.inactiveToo = inactiveToo;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DelegationFilter [");
		if (delegate != null) {
			builder.append("delegate=");
			builder.append(delegate);
			builder.append(", ");
		}
		if (delegator != null) {
			builder.append("delegator=");
			builder.append(delegator);
			builder.append(", ");
		}
		if (inactiveToo) {
			builder.append("inactiveToo=true");
			builder.append(", ");
		}
		builder.append(super.toString());
		builder.append("]");
		return builder.toString();
	}
}
