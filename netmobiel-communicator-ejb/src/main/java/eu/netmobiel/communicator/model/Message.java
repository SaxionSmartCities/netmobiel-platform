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
import javax.validation.constraints.Size;

import eu.netmobiel.commons.model.NetMobielMessage;
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
	@Size(max = MAX_MESSAGE_SIZE)
	@Column(name = "body")
	private String body;
	
	/**
	 * The context of the message. The context is a urn, referring to an object in the system.
	 * This is the context of the message as conceived by the sender.
	 * If the receiver needs a different context, that context will be added to the envelope.
	 */
	@Size(max = 32)
    @NotNull
	@Column(name = "context")
	private String context;
	
	/**
	 * The subject of the context, formatted by the client. For the backend this is an opaque string.
	 * The subject should be set for a given context. The reason for a subject is to prevent a 1+N query
	 * by the client for looking up the context of each message after retrieving the list of messages.
	 * The context will be used to lookup details of a message (i.e. clicking-through).     
	 */
	@Size(max = 128)
    @NotNull
	@Column(name = "subject")
	private String subject;
	
	
    @NotNull
	@Column(name = "created_time")
	private Instant creationTime;
	
    /**
     * Deliver as notification (push), regular message in inbox, or both.
     */
    @NotNull
	@Column(name = "delivery_mode", length = 2)
	private DeliveryMode deliveryMode;

	/**
	 * The sender of the message.
	 */
//    @NotNull
    @ManyToOne
    @JoinColumn(name = "sender", foreignKey = @ForeignKey(name = "message_sender_fk"))
    private CommunicatorUser sender;

    /**
     * The recipients of the message.
     */
	@OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<Envelope> envelopes;

	/** 
	 * The thread of the sender of the message. Optional when the system is sending a message. 
	 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_conversation", foreignKey = @ForeignKey(name = "message_sender_conversation_fk"))
    private Conversation senderConversation;

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

	public Conversation getSenderConversation() {
		return senderConversation;
	}

	public void setSenderConversation(Conversation senderConversation) {
		this.senderConversation = senderConversation;
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

	public void addRecipient(Conversation conv, String rcpContext) {
		addRecipient(new Envelope(rcpContext, conv));
	}

	public void addRecipient(Envelope env) {
		env.setMessage(this);
		getEnvelopes().add(env);
	}

	@Override
	public String toString() {
		return String.format("Message [%d %s %s '%s' %s %s '%s']", id, sender.toString(), 
				context, subject, deliveryMode, DateTimeFormatter.ISO_INSTANT.format(creationTime), body != null ? body : "");
	}

}
