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
import javax.validation.constraints.NotNull;

import eu.netmobiel.communicator.util.CommunicatorUrnHelper;

@NamedNativeQueries({
	@NamedNativeQuery(
		name = "ListMessagesReceivedCount",
		query = "select u.managed_identity as managed_identity, "
        		+ "date_part('year', m.created_time) as year, " 
        		+ "date_part('month', m.created_time) as month, "
        		+ "count(*) as count "
        		+ "from envelope e "
        		+ "join message m on m.id = e.message "
        		+ "join cm_user u on u.id = e.recipient "
        		+ "where m.created_time >= ? and m.created_time < ? and (m.delivery_mode = 'AL' or m.delivery_mode = 'MS') "
        		+ "group by u.managed_identity, year, month "
        		+ "order by u.managed_identity, year, month",
        resultSetMapping = "ListMessageCountMapping"),
	@NamedNativeQuery(
			name = "ListNotificationsReceivedCount",
			query = "select u.managed_identity as managed_identity, "
	        		+ "date_part('year', e.push_time) as year, " 
	        		+ "date_part('month', e.push_time) as month, "
	        		+ "count(*) as count "
	        		+ "from envelope e "
	        		+ "join cm_user u on u.id = e.recipient "
	        		+ "where e.push_time >= ? and e.push_time < ? "
	        		+ "group by u.managed_identity, year, month "
	        		+ "order by u.managed_identity, year, month",
	        resultSetMapping = "ListMessageCountMapping"),
	@NamedNativeQuery(
			name = "ListMessagesReadCount",
			query = "select u.managed_identity as managed_identity, "
	        		+ "date_part('year', e.ack_time) as year, " 
	        		+ "date_part('month', e.ack_time) as month, "
	        		+ "count(*) as count "
	        		+ "from envelope e "
	        		+ "join message m on m.id = e.message "
	        		+ "join cm_user u on u.id = e.recipient "
	        		+ "where e.ack_time >= ? and e.ack_time < ? and (m.delivery_mode = 'AL' or m.delivery_mode = 'MS') "
	        		+ "group by u.managed_identity, year, month "
	        		+ "order by u.managed_identity, year, month",
	        resultSetMapping = "ListMessageCountMapping"),
	@NamedNativeQuery(
			name = "ListNotificationsReadCount",
			query = "select u.managed_identity as managed_identity, "
	        		+ "date_part('year', e.ack_time) as year, " 
	        		+ "date_part('month', e.ack_time) as month, "
	        		+ "count(*) as count "
	        		+ "from envelope e "
	        		+ "join cm_user u on u.id = e.recipient "
	        		+ "where e.ack_time >= ? and e.ack_time < ? and e.push_time is not null "
	        		+ "group by u.managed_identity, year, month "
	        		+ "order by u.managed_identity, year, month",
	        resultSetMapping = "ListMessageCountMapping")
})
@SqlResultSetMapping(
		name = "ListMessageCountMapping", 
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
	    @UniqueConstraint(name = "cs_unique_message_recipient", columnNames = { "recipient", "message" })
})
@Vetoed
@SequenceGenerator(name = "envelope_sg", sequenceName = "envelope_id_seq", allocationSize = 1, initialValue = 50)
public class Envelope implements Serializable {

	private static final long serialVersionUID = 1045941720040157428L;
	public static final String URN_PREFIX = CommunicatorUrnHelper.createUrnPrefix(Envelope.class);

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "envelope_sg")
    private Long id;

    @Transient
    private String envelopeRef;

    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "message", nullable = false, foreignKey = @ForeignKey(name = "envelope_message_fk"))
    private Message message;

	/**
	 * The recipient of the message. 
	 */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recipient", nullable = false, foreignKey = @ForeignKey(name = "envelope_recipient_fk"))
    private CommunicatorUser recipient;

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
	
	public Envelope() {
		
	}
	
	public Envelope(Message m, CommunicatorUser rcp) {
		this(m, rcp, null, null);
	}
	
	public Envelope(Message m, CommunicatorUser rcp, Instant aPushTime, Instant anAckTime) {
		this.message = m;
		this.recipient = rcp;
		this.pushTime = aPushTime;
		this.ackTime = anAckTime;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEnvelopeRef() {
		if (envelopeRef == null) {
			envelopeRef = CommunicatorUrnHelper.createUrn(Envelope.URN_PREFIX, getId());
		}
		return envelopeRef;
	}


	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	public CommunicatorUser getRecipient() {
		return recipient;
	}

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

	@Override
	public String toString() {
		return String.format("Envelope [%s %s %s %s]", 
				ackTime != null ? DateTimeFormatter.ISO_INSTANT.format(ackTime) : "<no ack>",
				pushTime != null ? DateTimeFormatter.ISO_INSTANT.format(pushTime) : "<no push>",
				recipient,
				message.toString());
	}

   
}
