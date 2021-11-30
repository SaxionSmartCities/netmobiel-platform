package eu.netmobiel.communicator.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Size;

import com.google.firebase.database.annotations.NotNull;

import eu.netmobiel.commons.report.NumericReportValue;

@NamedNativeQueries({
	@NamedNativeQuery(
		name = Envelope.ACT_1_MESSAGES_RECEIVED_COUNT,
		query = "select u.managed_identity as managed_identity, "
        		+ "date_part('year', m.created_time) as year, " 
        		+ "date_part('month', m.created_time) as month, "
        		+ "count(*) as count "
        		+ "from envelope e "
        		+ "join conversation c on c.id = e.conversation "
        		+ "join message m on m.id = e.message "
        		+ "join cm_user u on u.id = c.owner "
        		+ "where e.sender = false and m.created_time >= ? and m.created_time < ? and (m.delivery_mode = 'AL' or m.delivery_mode = 'MS') "
        		+ "group by u.managed_identity, year, month "
        		+ "order by u.managed_identity, year, month",
        resultSetMapping = Envelope.USER_YEAR_MONTH_COUNT_MAPPING),
	@NamedNativeQuery(
			name = Envelope.ACT_2_NOTIFICATIONS_RECEIVED_COUNT,
			query = "select u.managed_identity as managed_identity, "
	        		+ "date_part('year', e.push_time) as year, " 
	        		+ "date_part('month', e.push_time) as month, "
	        		+ "count(*) as count "
	        		+ "from envelope e "
	        		+ "join conversation c on c.id = e.conversation "
	        		+ "join cm_user u on u.id = c.owner "
	        		+ "where e.sender = false and e.push_time >= ? and e.push_time < ? "
	        		+ "group by u.managed_identity, year, month "
	        		+ "order by u.managed_identity, year, month",
	        resultSetMapping = Envelope.USER_YEAR_MONTH_COUNT_MAPPING),
	@NamedNativeQuery(
			name = Envelope.ACT_3_MESSAGES_READ_COUNT,
			query = "select u.managed_identity as managed_identity, "
	        		+ "date_part('year', e.ack_time) as year, " 
	        		+ "date_part('month', e.ack_time) as month, "
	        		+ "count(*) as count "
	        		+ "from envelope e "
	        		+ "join conversation c on c.id = e.conversation "
	        		+ "join message m on m.id = e.message "
	        		+ "join cm_user u on u.id = c.owner "
	        		+ "where e.sender = false and e.ack_time >= ? and e.ack_time < ? and (m.delivery_mode = 'AL' or m.delivery_mode = 'MS') "
	        		+ "group by u.managed_identity, year, month "
	        		+ "order by u.managed_identity, year, month",
	        resultSetMapping = Envelope.USER_YEAR_MONTH_COUNT_MAPPING),
	@NamedNativeQuery(
			name = Envelope.ACT_4_NOTIFICATIONS_READ_COUNT,
			query = "select u.managed_identity as managed_identity, "
	        		+ "date_part('year', e.ack_time) as year, " 
	        		+ "date_part('month', e.ack_time) as month, "
	        		+ "count(*) as count "
	        		+ "from envelope e "
	        		+ "join conversation c on c.id = e.conversation "
	        		+ "join cm_user u on u.id = c.owner "
	        		+ "where e.sender = false and e.ack_time >= ? and e.ack_time < ? and e.push_time is not null "
	        		+ "group by u.managed_identity, year, month "
	        		+ "order by u.managed_identity, year, month",
	        resultSetMapping = Envelope.USER_YEAR_MONTH_COUNT_MAPPING),
	@NamedNativeQuery(
			name = Envelope.RGC_5_SHOUT_OUT_NOTIFICATIONS_RECEIVED_COUNT,
			query = "select u.managed_identity as managed_identity, "
	        		+ "date_part('year', e.push_time) as year, " 
	        		+ "date_part('month', e.push_time) as month, "
	        		+ "count(*) as count "
	        		+ "from envelope e "
	        		+ "join conversation c on c.id = e.conversation "
	        		+ "join message m on m.id = e.message "
	        		+ "join cm_user u on u.id = c.owner "
	        		+ "where e.sender = false and e.push_time >= ? and e.push_time < ? and e.context like 'urn:nb:pn:tripplan:' " 
	        		+ "group by u.managed_identity, year, month "
	        		+ "order by u.managed_identity, year, month",
	        resultSetMapping = Envelope.USER_YEAR_MONTH_COUNT_MAPPING),
	@NamedNativeQuery(
			name = Envelope.RGC_6_SHOUT_OUT_NOTIFICATIONS_READ_COUNT,
			query = "select u.managed_identity as managed_identity, "
	        		+ "date_part('year', e.ack_time) as year, " 
	        		+ "date_part('month', e.ack_time) as month, "
	        		+ "count(*) as count "
	        		+ "from envelope e "
	        		+ "join conversation c on c.id = e.conversation "
	        		+ "join message m on m.id = e.message "
	        		+ "join cm_user u on u.id = c.owner "
	        		+ "where e.sender = false and e.ack_time >= ? and e.ack_time < ? and e.push_time is not null and e.context like 'urn:nb:pn:tripplan:' "
	        		+ "group by u.managed_identity, year, month "
	        		+ "order by u.managed_identity, year, month",
	        resultSetMapping = Envelope.USER_YEAR_MONTH_COUNT_MAPPING),
})
@SqlResultSetMapping(
		name = Envelope.USER_YEAR_MONTH_COUNT_MAPPING, 
		classes = @ConstructorResult(
			targetClass = NumericReportValue.class, 
			columns = {
					@ColumnResult(name = "managed_identity", type = String.class),
					@ColumnResult(name = "year", type = int.class),
					@ColumnResult(name = "month", type = int.class),
					@ColumnResult(name = "count", type = int.class)
			})
	)

@Entity
@Table(name = "envelope", uniqueConstraints = {
	    @UniqueConstraint(name = "cs_unique_conversation_message", columnNames = { "conversation", "message" })
})
@Vetoed
@SequenceGenerator(name = "envelope_sg", sequenceName = "envelope_id_seq", allocationSize = 1, initialValue = 50)
public class Envelope implements Serializable {

	private static final long serialVersionUID = 1045941720040157428L;

	public static final String USER_YEAR_MONTH_COUNT_MAPPING = "CMEnvelopeUserYearMonthCountMapping";
	public static final String ACT_1_MESSAGES_RECEIVED_COUNT = "ListMessagesReceivedCount";
	public static final String ACT_2_NOTIFICATIONS_RECEIVED_COUNT = "ListNotificationsReceivedCount";
	public static final String ACT_3_MESSAGES_READ_COUNT = "ListMessagesReadCount";
	public static final String ACT_4_NOTIFICATIONS_READ_COUNT = "ListNotificationsReadCount";

	public static final String RGC_5_SHOUT_OUT_NOTIFICATIONS_RECEIVED_COUNT = "ListShoutOutNotificationsReceivedCount";
	public static final String RGC_6_SHOUT_OUT_NOTIFICATIONS_READ_COUNT = "ListShoutOutNotificationsReadCount";

	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "envelope_sg")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "message", nullable = false, foreignKey = @ForeignKey(name = "envelope_message_fk"))
    private Message message;

	/**
	 * The time the message was acknowledged (read) by the user.
	 */
	@Column(name = "ack_time")
	private Instant ackTime;
	
	/**
	 * The time the message was sent as a notification (push message) to the user, i.e. sent to the device used by the user. 
	 */
	@Column(name = "push_time")
	private Instant pushTime;
	
	/**
	 * The context of the message for the participant. The context is a urn, referring to an object in the system.
	 * This is the (non-optional) context of the message as conceived by the recipient.
	 * If the receiver shares the context with the sender, refer to the context of the message.
	 */
	@Size(max = 32)
	@NotNull
	@Column(name = "context")
	private String context;
	
	/** 
	 * The thread of the recipient. 
	 */
	@NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation", foreignKey = @ForeignKey(name = "envelope_conversation_fk"))
    private Conversation conversation;

    /**
     * If true this participant is the sender of the message.
     */
	@Column(name = "sender", nullable = false)
    private boolean sender;

	/**
	 * Convenience. Uses the owner of the conversation.
	 */
	@Transient
    private CommunicatorUser recipient;

	public Envelope() {
		
	}
	
	public Envelope(Message m, String context, Conversation conversation) {
		this.message = m;
		this.context = context;
		this.conversation = conversation;
	}
	
	public Envelope(Conversation aParticipant) {
		this(null, aParticipant, null, null);
	}

	public Envelope(String aContext, Conversation aParticipant) {
		this(aContext, aParticipant, null, null);
	}
	
	public Envelope(String aContext, Conversation aParticipant, Instant aPushTime, Instant anAckTime) {
		this.context = aContext;
		this.conversation = aParticipant;
		this.pushTime = aPushTime;
		this.ackTime = anAckTime;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	public CommunicatorUser getRecipient() {
		if (recipient == null && conversation != null) {
			recipient = conversation.getOwner();
		}
		return recipient;
	}

	/**
	 * Use this method only when creating a new envelope.
	 * @param recipient
	 */
	public void setRecipient(CommunicatorUser recipient) {
		this.recipient = recipient;
	}

	public Instant getAckTime() {
		return ackTime;
	}

	public void setAckTime(Instant ackTime) {
		this.ackTime = ackTime;
	}

	public Instant getPushTime() {
		return pushTime;
	}

	public void setPushTime(Instant pushTime) {
		this.pushTime = pushTime;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public Conversation getConversation() {
		return conversation;
	}

	public void setConversation(Conversation conversation) {
		this.conversation = conversation;
	}

	public boolean isSender() {
		return sender;
	}

	public void setSender(boolean sender) {
		this.sender = sender;
	}

	@Override
	public String toString() {
		return String.format("Envelope [%s %s %s %s %s]",
				getId(),
				ackTime != null ? DateTimeFormatter.ISO_INSTANT.format(ackTime) : "<no ack>",
				pushTime != null ? DateTimeFormatter.ISO_INSTANT.format(pushTime) : "<no push>",
				conversation != null ? conversation.getId() : "?",
				message.toString());
	}

   
}
