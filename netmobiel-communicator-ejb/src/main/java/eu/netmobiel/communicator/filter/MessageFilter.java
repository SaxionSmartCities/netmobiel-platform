package eu.netmobiel.communicator.filter;

import java.time.OffsetDateTime;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.filter.PeriodFilter;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.communicator.model.CommunicatorUser;
import eu.netmobiel.communicator.model.DeliveryMode;

public class MessageFilter extends PeriodFilter {
	/**
	 * The sender or recipient of the message.
	 */
	private CommunicatorUser participant;

	/**
	 * The context of the message (or the envelope)
	 */
	private String context;
	
	/**
	 * The delivery mode to look for. 
	 */
	private DeliveryMode deliveryMode;

	/**
	 * The conversation id.
	 */
	private Long conversationId;

	public MessageFilter() {
	}

	public MessageFilter(Long conversationId, String sortDir) {
		this(sortDir);
		this.conversationId = conversationId;
	}

	public MessageFilter(String sortDir) {
		this(null, null, null, null, sortDir);
	}
	
	public MessageFilter(CommunicatorUser participant, OffsetDateTime since, OffsetDateTime until, String context, String sortDir) {
		this.participant = participant;
		this.context = context;
		setSince(since);
		setUntil(until);
		setSortDir(sortDir);
	}
	
	public CommunicatorUser getParticipant() {
		return participant;
	}

	public void setParticipant(CommunicatorUser participant) {
		this.participant = participant;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public DeliveryMode getDeliveryMode() {
		return deliveryMode;
	}

	public void setDeliveryMode(DeliveryMode deliveryMode) {
		this.deliveryMode = deliveryMode;
	}

	public Long getConversationId() {
		return conversationId;
	}

	public void setConversationId(Long conversationId) {
		this.conversationId = conversationId;
	}

	@Override
	public void validate() throws BadRequestException {
    	if (getSortDir() == null) {
    		setSortDir(SortDirection.DESC);
    	}
    	super.validate();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (conversationId != null) {
			builder.append("ci=");
			builder.append(conversationId);
			builder.append(" ");
		}
		if (participant != null) {
			builder.append("pi=");
			builder.append(participant);
			builder.append(" ");
		}
		if (deliveryMode != null) {
			builder.append("dm=");
			builder.append(deliveryMode);
			builder.append(" ");
		}
		if (context != null) {
			builder.append("cx=");
			builder.append(context);
			builder.append(" ");
		}
		builder.append(super.toString());
		return builder.toString();
	}

}
