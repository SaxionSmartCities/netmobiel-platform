package eu.netmobiel.commons.model;

import java.util.Collections;
import java.util.List;

import eu.netmobiel.commons.filter.Cursor;

public class PagedResult<T> {
	/**
	 * The payload data. Can be null or empty.
	 */
	private List<T> data;
	/**
	 * The size of the total result set, if known.
	 */
	private Long totalCount;
	/**
	 * The maximum number of items in a page. If 0 then no items are returned.
	 */
	private int resultsPerPage;
	/**
	 * The zero-based offset in the result set. 
	 */
	private int offset;
	
	public PagedResult() {
		this(null, 0, 0, 0L);
	}
	
	public PagedResult(List<T> someData, Cursor cursor, Long aTotalCount) {
		this(someData, cursor.getMaxResults(), cursor.getOffset(), aTotalCount);
	}

	public PagedResult(List<T> someData, int aResultsPerPage, int anOffset, Long aTotalCount) {
		this.data = someData == null ? Collections.emptyList() : someData;
		this.resultsPerPage = aResultsPerPage;
		this.offset = anOffset;
		this.totalCount = aTotalCount;
	}

	public static <T> PagedResult<T> empty() {
		return new PagedResult<>(null, 0, 0, 0L);
	}
	
	public List<T> getData() {
		return data;
	}

	public Long getTotalCount() {
		return totalCount;
	}

	public int getResultsPerPage() {
		return resultsPerPage;
	}

	public int getCount() {
		return data != null ? data.size() : 0; 
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}
}
