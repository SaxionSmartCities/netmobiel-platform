package eu.netmobiel.banker.filter;

import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.filter.BaseFilter;
import eu.netmobiel.commons.model.SortDirection;

public class IncentiveFilter extends BaseFilter {
	/* ========= Selection of incentives ===================== */ 

	/**
	 * If true then list disabled incentives too. Default false.
	 */
	private boolean disabledToo;
	
	/** ==============================
	 * Selection of a specific user.
	 */
	private BankerUser user;

	public IncentiveFilter() {
	}

	public IncentiveFilter(BankerUser user, boolean disabledToo, String sortDir) throws BadRequestException {
		setUser(user);
		setSortDir(sortDir, SortDirection.DESC);
	}

	public BankerUser getUser() {
		return user;
	}

	public void setUser(BankerUser user) {
		this.user = user;
	}

	public boolean isDisabledToo() {
		return disabledToo;
	}

	public void setDisabledToo(boolean disabledToo) {
		this.disabledToo = disabledToo;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (user != null) {
			builder.append("us=");
			builder.append(user);
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
