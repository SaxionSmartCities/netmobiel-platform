package eu.netmobiel.communicator.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedEntityGraph;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import eu.netmobiel.communicator.util.CommunicatorUrnHelper;

@NamedEntityGraph()
@Entity
@Table(name = "message")
@Vetoed
@SequenceGenerator(name = "message_sg", sequenceName = "message_id_seq", allocationSize = 1, initialValue = 50)
public class Message implements Serializable {

	private static final long serialVersionUID = 1045941720040157428L;
	public static final String URN_PREFIX = CommunicatorUrnHelper.createUrnPrefix(Message.class);
	public static final int MAX_MESSAGE_SIZE = 1024;
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "message_sg")
    private Long id;

	/**
	 * The text of the message. 
	 */
	@Column(name = "body", length = MAX_MESSAGE_SIZE)
	private String body;
	
	/**
	 * The context of the message. The context is a urn , referring to an object in the system.
	 */
    @NotNull
	@Column(name = "context", length = 32, nullable = false)
	private String context;
	
	/**
	 * The subject of the context, formatted by the client. For the backend this is an opaque string.
	 * The subject should be fixed for a given context. The reason for a subject is to prevent a 1+N query
	 * by the client for looking up the context of each message after retrieving the list of messages.   
	 */
    @NotNull
	@Column(name = "subject", length = 128, nullable = false)
	private String subject;
	
	
    @NotNull
	@Column(name = "created_time", nullable = false)
	private Instant creationTime;
	
	@Column(name = "delivery_mode", length = 2, nullable = false)
	private DeliveryMode deliveryMode;

	/**
	 * The sender of the message.
	 */
    @NotNull
    @ManyToOne
    @JoinColumn(name = "sender", nullable = false, foreignKey = @ForeignKey(name = "message_sender_fk"))
    private User sender;

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}


	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public Instant getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Instant creationTime) {
		this.creationTime = creationTime;
	}

	public DeliveryMode getDeliveryMode() {
		return deliveryMode;
	}

	public void setDeliveryMode(DeliveryMode deliveryMode) {
		this.deliveryMode = deliveryMode;
	}

	public User getSender() {
		return sender;
	}

	public void setSender(User sender) {
		this.sender = sender;
	}

	@Override
	public String toString() {
		return String.format("Message [%d %s %s '%s' %s '%s']", id, sender.toString(), context, subject, DateTimeFormatter.ISO_INSTANT.format(creationTime), body != null ? body : "");
	}

   
}
