package eu.netmobiel.communicator.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import eu.netmobiel.commons.model.NetMobielMessage;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.communicator.util.CommunicatorUrnHelper;

@NamedEntityGraph(
		name = Message.LIST_MY_MESSAGES_ENTITY_GRAPH, 
		attributeNodes = { 
				@NamedAttributeNode(value = "sender"),		
				@NamedAttributeNode(value = "envelopes", subgraph = "envelope-details")		
		}, subgraphs = {
				@NamedSubgraph(
						name = "envelope-details",
						attributeNodes = {
								@NamedAttributeNode(value = "recipient"),
								@NamedAttributeNode(value = "ackTime")
						}
					)
				}
	)
@Entity
@Table(name = "message")
@Vetoed
@SequenceGenerator(name = "message_sg", sequenceName = "message_id_seq", allocationSize = 1, initialValue = 50)
public class Message implements NetMobielMessage, Serializable {

	private static final long serialVersionUID = 5034396677188994964L;
	public static final String URN_PREFIX = CommunicatorUrnHelper.createUrnPrefix(Message.class);
	public static final String LIST_MY_MESSAGES_ENTITY_GRAPH = "list-my-messages-graph";
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
    private CommunicatorUser sender;

    /**
     * The recipients of the message.
     */
	@OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<Envelope> envelopes;

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}


	@Override
	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	@Override
	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	@Override
	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	@Override
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

	@Override
	public CommunicatorUser getSender() {
		return sender;
	}

	public void setSender(CommunicatorUser sender) {
		this.sender = sender;
	}

	public List<Envelope> getEnvelopes() {
		if (envelopes == null) {
			envelopes = new ArrayList<>();
		}
		return envelopes;
	}

	public void setEnvelopes(List<Envelope> envelopes) {
		this.envelopes = envelopes;
	}

	public void addRecipient(NetMobielUser nmu) {
		CommunicatorUser rcp = new CommunicatorUser(nmu); 
		getEnvelopes().add(new Envelope(this, rcp));
	}

	@Override
	public String toString() {
		return String.format("Message [%d %s %s '%s' %s %s '%s']", id, sender.toString(), 
				context, subject, deliveryMode, DateTimeFormatter.ISO_INSTANT.format(creationTime), body != null ? body : "");
	}

}
