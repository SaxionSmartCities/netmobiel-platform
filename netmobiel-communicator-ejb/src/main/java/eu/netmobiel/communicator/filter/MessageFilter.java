package eu.netmobiel.communicator.filter;

import java.time.OffsetDateTime;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.filter.PeriodFilter;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.communicator.model.DeliveryMode;

public class MessageFilter extends PeriodFilter {
	/**
	 * The managed identity of the sender or recipient of the message.
	 */
	private String participantId;

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
	
	public MessageFilter(String participantId, OffsetDateTime since, OffsetDateTime until, String context, String sortDir) {
		this.participantId = participantId;
		this.context = context;
		setSince(since);
		setUntil(until);
		setSortDir(sortDir);
	}
	
	public String getParticipantId() {
		return participantId;
	}

	public void setParticipantId(String participantId) {
		this.participantId = participantId;
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
		builder.append("MessageFilter [");
		if (conversationId != null) {
			builder.append("conversationId=");
			builder.append(conversationId);
			builder.append(", ");
		}
		if (participantId != null) {
			builder.append("participantId=");
			builder.append(participantId);
			builder.append(", ");
		}
		if (deliveryMode != null) {
			builder.append("deliveryMode=");
			builder.append(deliveryMode);
			builder.append(", ");
		}
		if (context != null) {
			builder.append("context=");
			builder.append(context);
			builder.append(", ");
		}
		builder.append(super.toString());
		builder.append("]");
		return builder.toString();
	}

}
