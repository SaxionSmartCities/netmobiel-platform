package eu.netmobiel.commons.model;

import java.util.List;

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
	
	public PagedResult(List<T> someData, int aResultsPerPage, int anOffset, Long aTotalCount) {
		this.data = someData;
		this.resultsPerPage = aResultsPerPage;
		this.offset = anOffset;
		this.totalCount = aTotalCount;
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
		return data == null ? 0 : data.size(); 
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}
}
