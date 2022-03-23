package eu.netmobiel.banker.filter;

import java.time.OffsetDateTime;
import java.util.Arrays;

import eu.netmobiel.banker.model.CharitySortBy;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.filter.PeriodFilter;
import eu.netmobiel.commons.model.GeoLocation;

public class CharityFilter extends PeriodFilter {
	/**
	 * Location of a circle to filter charities to include. Ignored if charity is set.
	 */
	private GeoLocation location;
	/**
	 * Radius of a circle to filter charities to include. Ignored if charity is set.
	 */
	private Integer radius;
	/**
	 * Include inactive charities if true.
	 */
	private boolean inactiveToo;

	/**
	 * Include deleted charities if true.
	 */
	private boolean deletedToo;

	private CharitySortBy sortBy;

	public CharityFilter() {
	}

	public CharityFilter(String location, Integer radius, OffsetDateTime since, OffsetDateTime until,
			boolean inactiveToo, boolean deletedToo, String sortBy, String sortDir) {
		setLocation(location);
		this.radius = radius;
		setSince(since);
		setUntil(until);
		this.inactiveToo = inactiveToo;
		this.deletedToo = deletedToo;
		setSortBy(sortBy);
		setSortDir(sortDir);
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

	public boolean isInactiveToo() {
		return inactiveToo;
	}

	public void setInactiveToo(boolean inactiveToo) {
		this.inactiveToo = inactiveToo;
	}

	public boolean isDeletedToo() {
		return deletedToo;
	}

	public void setDeletedToo(boolean deletedToo) {
		this.deletedToo = deletedToo;
	}

	public Integer getRadius() {
		return radius;
	}

	public void setRadius(Integer radius) {
		this.radius = radius;
	}


	public CharitySortBy getSortBy() {
		return sortBy;
	}

	public void setSortBy(CharitySortBy sortBy) {
		this.sortBy = sortBy;
	}

	public final void setSortBy(String sortBy) {
		if (sortBy != null) {
			this.sortBy = CharitySortBy.valueOf(sortBy);
		}
	}

	public void setSortBy(String sortBy, CharitySortBy defaultSortBy, CharitySortBy[] supportedSortBy) {
		if (sortBy == null) {
			this.sortBy = defaultSortBy;
		} else {
			setSortBy(sortBy);
		}
		if (Arrays.stream(supportedSortBy).noneMatch(p -> p == this.sortBy)) {
			throw new IllegalArgumentException("SortyBy is not supported: " + this.sortBy);
		}
	}


	@Override
	public void validate() throws BadRequestException {
		super.validate();
    	if (this.sortBy == null) {
    		this.sortBy = CharitySortBy.NAME;
    	}
	}
}
