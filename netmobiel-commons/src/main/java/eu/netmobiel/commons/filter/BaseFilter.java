package eu.netmobiel.commons.filter;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.model.SortDirection;

public class BaseFilter {
	private SortDirection sortDir;
	
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

	public void validate() throws BadRequestException {
    	if (this.sortDir == null) {
    		this.sortDir = SortDirection.ASC;
    	}
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (sortDir != null) {
			builder.append("sortDir=");
			builder.append(sortDir);
		}
		return builder.toString();
	}

}
