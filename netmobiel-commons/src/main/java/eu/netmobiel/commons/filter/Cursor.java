package eu.netmobiel.commons.filter;

import eu.netmobiel.commons.exception.BadRequestException;

/**
 * Generic cursor class for data retrieval in NetMobiel.
 * 
 * @author Jaap Reitsma
 *
 */
public class Cursor {
	private Integer maxResults;
	private Integer offset;
	public static final Cursor COUNTING_CURSOR = new Cursor(0, 0);
	public static final int MAX_RESULTS = 100;
	
	public Cursor() {
		// Constructor
	}
	
	public Cursor(Integer aMaxResult, Integer anOffset) {
		this.maxResults = aMaxResult;
		this.offset = anOffset;
	}
	
	public void validate(Integer defaultMaxResults, Integer defaultOffset) throws BadRequestException {
    	if (maxResults != null && maxResults > 100) {
    		throw new BadRequestException("Constraint violation: 'maxResults' <= 100.");
    	}
    	if (maxResults != null && maxResults < 0) {
    		throw new BadRequestException("Constraint violation: 'maxResults' >= 0.");
    	}
    	if (offset != null && offset < 0) {
    		throw new BadRequestException("Constraint violation: 'offset' >= 0.");
    	}
        if (maxResults == null) {
        	maxResults = defaultMaxResults;
        }
        if (offset == null) {
        	offset = defaultOffset;
        }
	}

	public Integer getMaxResults() {
		return maxResults;
	}

	public Integer getOffset() {
		return offset;
	}
	
	public boolean isCountingQuery() {
		return maxResults == 0;
	}

	public void next() {
		this.offset += this.maxResults;
	}
	
	@Override
	public String toString() {
		return String.format("Cursor [%s %s]", maxResults, offset);
	}
}
