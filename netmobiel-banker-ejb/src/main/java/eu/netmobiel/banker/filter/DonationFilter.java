package eu.netmobiel.banker.filter;

import java.time.OffsetDateTime;
import java.util.Arrays;

import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.Charity;
import eu.netmobiel.banker.model.DonationSortBy;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.filter.PeriodFilter;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.util.UrnHelper;

public class DonationFilter extends PeriodFilter {
	/** ============================== 
	 * Selection of charities
	 */
	private Long charityId;
	private Charity charity;
	/**
	 * Location of a circle to filter charities to include. Ignored if charity is set.
	 */
	private GeoLocation location;
	/**
	 * Radius of a circle to filter charities to include. Ignored if charity is set.
	 */
	private Integer radius;
	/**
	 * Omit inactive charities if true. Ignored if charity is set.
	 */
	private boolean omitInactiveCharities;

	/** ==============================
	 * Selection of a specific user.
	 */
	private Long userId;
	private BankerUser user;
	
	private DonationSortBy sortBy;
	/**
	 * Should we ignore the anonymous flag? Only when a user requests his own
	 * donations, or when an admin requests the overview In report queries the
	 * anonymous donations are *always* excluded from the reports.
	 */
	private boolean anonymousToo;

	public DonationFilter() {
	}

	public DonationFilter(String charityId, Long userId, OffsetDateTime since, OffsetDateTime until,
			String sortBy, String sortDir, boolean anonymousToo) {
		setCharityId(charityId);
		setUserId(userId);
		setSince(since);
		setUntil(until);
		setSortBy(sortBy);
		setSortDir(sortDir);
		this.anonymousToo = anonymousToo;
	}
	
	public DonationFilter(String location, Integer radius, boolean omitInactiveCharities, Long userId, OffsetDateTime since, OffsetDateTime until,
			String sortBy, String sortDir, boolean anonymousToo) {
		setLocation(location);
		this.radius = radius;
		this.omitInactiveCharities = omitInactiveCharities;
		setUserId(userId);
		setSince(since);
		setUntil(until);
		setSortBy(sortBy);
		setSortDir(sortDir);
		this.anonymousToo = anonymousToo;
	}

	public Long getCharityId() {
		return charityId;
	}

	public void setCharityId(Long charityId) {
		this.charityId = charityId;
	}

	public final void setCharityId(String charityId) {
		this.charityId = UrnHelper.getId(Charity.URN_PREFIX, charityId);
	}

	public Charity getCharity() {
		return charity;
	}

	public void setCharity(Charity charity) {
		this.charity = charity;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public BankerUser getUser() {
		return user;
	}

	public void setUser(BankerUser user) {
		this.user = user;
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

	public Integer getRadius() {
		return radius;
	}

	public void setRadius(Integer radius) {
		this.radius = radius;
	}

	public boolean isOmitInactiveCharities() {
		return omitInactiveCharities;
	}

	public void setOmitInactiveCharities(boolean omitInactiveCharities) {
		this.omitInactiveCharities = omitInactiveCharities;
	}

	public DonationSortBy getSortBy() {
		return sortBy;
	}

	public void setSortBy(DonationSortBy sortBy) {
		this.sortBy = sortBy;
	}

	public final void setSortBy(String sortBy) {
		if (sortBy != null) {
			this.sortBy = DonationSortBy.valueOf(sortBy);
		}
	}

	public void setSortBy(String sortBy, DonationSortBy defaultSortBy, DonationSortBy[] supportedSortBy) {
		if (sortBy == null) {
			this.sortBy = defaultSortBy;
		} else {
			setSortBy(sortBy);
		}
		if (Arrays.stream(supportedSortBy).noneMatch(p -> p == this.sortBy)) {
			throw new IllegalArgumentException("SortyBy is not supported: " + this.sortBy);
		}
	}

	public boolean isAnonymousToo() {
		return anonymousToo;
	}

	public void setAnonymousToo(boolean anonymousToo) {
		this.anonymousToo = anonymousToo;
	}

	@Override
	public void validate() throws BadRequestException {
		super.validate();
    	if (this.sortBy == null) {
    		this.sortBy = DonationSortBy.DATE;
    	}
	}
}
