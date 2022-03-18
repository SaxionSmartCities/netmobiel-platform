package eu.netmobiel.banker.filter;

import java.time.OffsetDateTime;

import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.RewardType;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.filter.PeriodFilter;
import eu.netmobiel.commons.model.SortDirection;

public class RewardFilter extends PeriodFilter {
	/* ========= Selection of rewards ===================== */ 

	/**
	 * If set then list rewards by their cancel status (cancel time set means cancelled).  
	 */
	private Boolean cancelled;
	
	/**
	 * Type of reward to list.
	 */
	private RewardType rewardType;

	/** ==============================
	 * Selection of a specific user.
	 */
	private BankerUser user;

	/**
	 * If set then list rewards by their paid status.  
	 */
	private Boolean paid;
	

	public RewardFilter() {
	}

	public RewardFilter(BankerUser user, Boolean cancelled, String sortDir) throws BadRequestException {
		setUser(user);
		this.cancelled = cancelled;
		setSortDir(sortDir, SortDirection.DESC);
	}

	public RewardFilter(BankerUser user, OffsetDateTime since, OffsetDateTime until,
			Boolean paid, String rewardType, Boolean cancelled, String sortDir) throws BadRequestException {
		setUser(user);
		setSince(since);
		setUntil(until);
		this.paid= paid;
		this.rewardType = rewardType == null ? null : RewardType.valueOf(rewardType);
		this.cancelled = cancelled;
		setSortDir(sortDir, SortDirection.DESC);
	}
	
	public BankerUser getUser() {
		return user;
	}

	public void setUser(BankerUser user) {
		this.user = user;
	}


	public Boolean getCancelled() {
		return cancelled;
	}

	public void setCancelled(Boolean cancelled) {
		this.cancelled = cancelled;
	}

	public RewardType getRewardType() {
		return rewardType;
	}

	public void setRewardType(RewardType rewardType) {
		this.rewardType = rewardType;
	}

	public Boolean getPaid() {
		return paid;
	}

	public void setPaid(Boolean paid) {
		this.paid = paid;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RewardFilter[");
		if (user != null) {
			builder.append("user=");
			builder.append(user);
			builder.append(", ");
		}
		if (rewardType != null) {
			builder.append("rewardType=");
			builder.append(rewardType);
			builder.append(", ");
		}
		if (cancelled != null) {
			builder.append("cancelled=");
			builder.append(cancelled);
			builder.append(", ");
		}
		if (paid != null) {
			builder.append("pending=");
			builder.append(paid);
			builder.append(", ");
		}
		builder.append(super.toString());
		builder.append("]");
		return builder.toString();
	}

}
