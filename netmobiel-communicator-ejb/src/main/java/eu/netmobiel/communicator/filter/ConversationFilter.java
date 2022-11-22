package eu.netmobiel.communicator.filter;

import java.time.OffsetDateTime;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.filter.PeriodFilter;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.communicator.model.CommunicatorUser;

public class ConversationFilter extends PeriodFilter {
	/**
	 * The owner of the conversation.
	 */
	private CommunicatorUser owner;

	/**
	 * A context that is member of the conversation's contexts.
	 */
	private String context;
	
	/**
	 * List only actual conversations
	 */
	boolean actualOnly; 
	/**
	 * List only archived conversations
	 */
	boolean archivedOnly;
	
	public ConversationFilter() {
	}

	public ConversationFilter(CommunicatorUser owner, boolean actualOnly, boolean archivedOnly, 
			OffsetDateTime since, OffsetDateTime until, String context, String sortDir) {
		this.owner = owner;
		this.context = context;
		this.actualOnly = actualOnly;
		this.archivedOnly = archivedOnly;
		setSince(since);
		setUntil(until);
		setSortDir(sortDir);
	}
	

	public CommunicatorUser getOwner() {
		return owner;
	}

	public void setOwner(CommunicatorUser owner) {
		this.owner = owner;
	}

	public boolean isActualOnly() {
		return actualOnly;
	}

	public void setActualOnly(boolean actualOnly) {
		this.actualOnly = actualOnly;
	}

	public boolean isArchivedOnly() {
		return archivedOnly;
	}

	public void setArchivedOnly(boolean archivedOnly) {
		this.archivedOnly = archivedOnly;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	@Override
	public void validate() throws BadRequestException {
    	super.validate();
    	if (getSortDir() == null) {
    		setSortDir(SortDirection.DESC);
    	}
        if (actualOnly && archivedOnly) {
        	throw new BadRequestException("You cannot have actualOnly AND archiveOnly at the same time");
        }
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (owner != null) {
			builder.append("pi=");
			builder.append(owner);
			builder.append(" ");
		}
		if (context != null) {
			builder.append("cx=");
			builder.append(context);
			builder.append(" ");
		}
		if (actualOnly) {
			builder.append("act=true ");
		}
		if (archivedOnly) {
			builder.append("arc=true ");
		}
		builder.append(super.toString());
		return builder.toString();
	}

}
