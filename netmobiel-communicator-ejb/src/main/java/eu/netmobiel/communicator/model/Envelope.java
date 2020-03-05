package eu.netmobiel.communicator.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import javax.enterprise.inject.Vetoed;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import eu.netmobiel.communicator.util.CommunicatorUrnHelper;

@NamedEntityGraph(
		name = Envelope.LIST_MY_ENVELOPES_ENTITY_GRAPH, 
		attributeNodes = { 
				@NamedAttributeNode(value = "message", subgraph = "message-details")		
		}, subgraphs = {
				// Without this subgraph no leg details are retrieved
				@NamedSubgraph(
						name = "message-details",
						attributeNodes = {
								@NamedAttributeNode(value = "sender")
						}
					)
				}

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
	public static final String LIST_MY_ENVELOPES_ENTITY_GRAPH = "list-my-envelopes-graph";

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "envelope_sg")
    private Long id;

    @Transient
    private String envelopeRef;

    @ManyToOne(cascade = { CascadeType.PERSIST } )
	@JoinColumn(name = "message", nullable = false, foreignKey = @ForeignKey(name = "envelope_message_fk"))
    private Message message;

	/**
	 * The recipient of the message. 
	 */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recipient", nullable = false, foreignKey = @ForeignKey(name = "message_recipient_fk"))
    private User recipient;

	/**
	 * The time the message was acknowledged (read) by the user.
	 */
	@Column(name = "ack_time")
	private Instant ackTime;
	
	public Envelope() {
		
	}
	
	public Envelope(Message m, User rcp) {
		this(m, rcp, null);
	}
	
	public Envelope(Message m, User rcp, Instant anAckTime) {
		this.message = m;
		this.recipient = rcp;
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

	public User getRecipient() {
		return recipient;
	}

	public void setRecipient(User recipient) {
		this.recipient = recipient;
	}


	public Instant getAckTime() {
		return ackTime;
	}

	public void setAckTime(Instant ackTime) {
		this.ackTime = ackTime;
	}

	@Override
	public String toString() {
		return String.format("Envelope [%s %s %s]", 
				ackTime != null ? DateTimeFormatter.ISO_DATE_TIME.format(ackTime.atZone(ZoneOffset.UTC)) : "<no ack>",
				recipient,
				message.toString());
	}

   
}
