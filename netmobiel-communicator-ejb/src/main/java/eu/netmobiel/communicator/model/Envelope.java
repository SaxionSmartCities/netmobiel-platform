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
import javax.persistence.NamedEntityGraph;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import eu.netmobiel.communicator.util.MessageServiceUrnHelper;

@NamedEntityGraph()
@Entity
@Table(name = "envelope", uniqueConstraints = {
	    @UniqueConstraint(name = "cs_unique_message_recipient", columnNames = { "recipient", "message" })
})
@Vetoed
@SequenceGenerator(name = "envelope_sg", sequenceName = "envelope_id_seq", allocationSize = 1, initialValue = 50)
public class Envelope implements Serializable {

	private static final long serialVersionUID = 1045941720040157428L;
	public static final String URN_PREFIX = MessageServiceUrnHelper.createUrnPrefix(Envelope.class);

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "envelope_sg")
    private Long id;

    @Transient
    private String envelopeRef;

    @ManyToOne(fetch = FetchType.EAGER, cascade = { CascadeType.PERSIST } )
	@JoinColumn(name = "message", nullable = false, foreignKey = @ForeignKey(name = "envelope_message_fk"))
    private Message message;

	/**
	 * The recipient of the message. Address format is keycloak managed identity guid or a system name.
	 */
    @NotNull
	@Column(name = "recipient", length = 36, nullable = false)
	private String recipient;

	/**
	 * The time the message was acknowledged (read) by the user.
	 */
	@Column(name = "ack_time")
	private Instant ackTime;
	
	public Envelope() {
		
	}
	
	public Envelope(Message m, String rcp) {
		this(m, rcp, null);
	}
	
	public Envelope(Message m, String rcp, Instant anAckTime) {
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
			envelopeRef = MessageServiceUrnHelper.createUrn(Envelope.URN_PREFIX, getId());
		}
		return envelopeRef;
	}


	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	public String getRecipient() {
		return recipient;
	}

	public void setRecipient(String recipient) {
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
