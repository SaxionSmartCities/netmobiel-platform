package eu.netmobiel.banker.filter;

import java.time.Instant;

import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.filter.BaseFilter;
import eu.netmobiel.commons.model.SortDirection;

public class IncentiveFilter extends BaseFilter {
	/**
	 * Reference time to compare against.
	 */
	private Instant now;
	
	/* ========= Selection of incentives ===================== */ 

	/**
	 * If true then list disabled incentives too. Default false.
	 */
	private boolean disabledToo;
	
	/**
	 * If true then list inactive incentives too. Default false.
	 */
	private boolean inactiveToo;

	/** ==============================
	 * Selection of a specific user. Depends on query. 
	 */
	private BankerUser user;

	public IncentiveFilter() {
	}

	public IncentiveFilter(BankerUser user, boolean inactiveToo, boolean disabledToo, String sortDir) throws BadRequestException {
		setUser(user);
		this.inactiveToo = inactiveToo;
		this.disabledToo = disabledToo;
		setSortDir(sortDir, SortDirection.DESC);
	}

	public IncentiveFilter(boolean inactiveToo, boolean disabledToo, String sortDir) throws BadRequestException {
		this.inactiveToo = inactiveToo;
		this.disabledToo = disabledToo;
		setSortDir(sortDir, SortDirection.DESC);
	}

	public Instant getNow() {
		return now;
	}

	public void setNow(Instant now) {
		this.now = now;
	}

	public BankerUser getUser() {
		return user;
	}

	public void setUser(BankerUser user) {
		this.user = user;
	}

	public boolean isInactiveToo() {
		return inactiveToo;
	}

	public void setInactiveToo(boolean inactiveToo) {
		this.inactiveToo = inactiveToo;
	}

	public boolean isDisabledToo() {
		return disabledToo;
	}

	public void setDisabledToo(boolean disabledToo) {
		this.disabledToo = disabledToo;
	}

	@Override
	public void validate() throws BadRequestException {
		super.validate();
		if (this.now == null) {
			this.now = Instant.now();
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (user != null) {
			builder.append("us=");
			builder.append(user);
			builder.append(" ");
		}
		if (inactiveToo) {
			builder.append("ia=");
			builder.append(inactiveToo);
			builder.append(" ");
		}
		if (disabledToo) {
			builder.append("dt=");
			builder.append(disabledToo);
			builder.append(" ");
		}
		builder.append(super.toString());
		return builder.toString();
	}

}
