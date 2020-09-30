package eu.netmobiel.banker.filter;

import java.time.Instant;
import java.time.OffsetDateTime;

import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.Charity;
import eu.netmobiel.banker.model.DonationSortBy;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.util.UrnHelper;

public class DonationFilter {
	private Instant now;
	private Long charityId;
	private Charity charity;
	private Long userId;
	private BankerUser user;
	private GeoLocation location;
	private Integer radius;
	private Instant since;
	private Instant until;
	private Boolean inactiveToo;
	private DonationSortBy sortBy;
	private SortDirection sortDir;
	/**
	 * Should we ignore the anonymous flag? Only when a user requests his own
	 * donations, or when an admin requests tyhe overview In report queries the
	 * anonymous donations are *always* excluded from the reports.
	 */
	private boolean anonymousToo;

	public DonationFilter() {
		this.sortBy = DonationSortBy.DATE;
		this.sortDir = SortDirection.DESC;
	}

	public DonationFilter(String charityId, Long userId, OffsetDateTime since, OffsetDateTime until,
			Boolean inactiveToo, String sortBy, String sortDir, boolean anonymousToo) {
		setCharityId(charityId);
		setUserId(userId);
		setSince(since);
		setUntil(until);
		setSince(since);
		setUntil(until);
		this.inactiveToo = inactiveToo;
		this.anonymousToo = anonymousToo;
	}
	
	public Instant getNow() {
		return now;
	}

	public void setNow(Instant now) {
		this.now = now;
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

	public Integer getRadius() {
		return radius;
	}

	public void setRadius(Integer radius) {
		this.radius = radius;
	}

	public Instant getSince() {
		return since;
	}

	public void setSince(Instant since) {
		this.since = since;
	}

	public final void setSince(OffsetDateTime since) {
		if (since != null) {
			this.since = since.toInstant();
		}
	}

	public Instant getUntil() {
		return until;
	}

	public void setUntil(Instant until) {
		this.until = until;
	}

	public final void setUntil(OffsetDateTime until) {
		if (until != null) {
			this.until = until.toInstant();
		}
	}

	public Boolean getInactiveToo() {
		return inactiveToo;
	}

	public void setInactiveToo(Boolean inactiveToo) {
		this.inactiveToo = inactiveToo;
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

	public SortDirection getSortDir() {
		return sortDir;
	}

	public void setSortDir(SortDirection sortDir) {
		this.sortDir = sortDir;
	}

	public final void setSortDir(String sortDir) {
		if (sortDir != null) {
			this.sortDir = SortDirection.valueOf(sortDir);
		}
	}

	public boolean isAnonymousToo() {
		return anonymousToo;
	}

	public void setAnonymousToo(boolean anonymousToo) {
		this.anonymousToo = anonymousToo;
	}

	public void validate() throws BadRequestException {
    	if (now == null) {
    		now = Instant.now();
    	}
    	if (until != null && since != null && !until.isAfter(since)) {
    		throw new BadRequestException("Constraint violation: 'until' must be later than 'since'.");
    	}
	}
}
