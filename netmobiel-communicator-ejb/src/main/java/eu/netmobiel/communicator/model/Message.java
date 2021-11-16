package eu.netmobiel.communicator.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import eu.netmobiel.commons.model.NetMobielMessage;
import eu.netmobiel.communicator.util.CommunicatorUrnHelper;

@NamedEntityGraph(
		name = Message.MESSAGE_ENVELOPES_ENTITY_GRAPH, 
		attributeNodes = { 
				@NamedAttributeNode(value = "oldSender"),		
				@NamedAttributeNode(value = "envelopes", subgraph = "envelope-details")		
		}, subgraphs = {
				@NamedSubgraph(
						name = "envelope-details",
						attributeNodes = {
								@NamedAttributeNode(value = "oldRecipient"),
								@NamedAttributeNode(value = "ackTime"),
								@NamedAttributeNode(value = "conversation", subgraph = "conversation-details"),
						}
					),
				@NamedSubgraph(
						name = "conversation-details",
						attributeNodes = {
								@NamedAttributeNode(value = "owner"),
//								@NamedAttributeNode(value = "contexts")
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
	public static final String MESSAGE_ENVELOPES_ENTITY_GRAPH = "message-envelopes-entity-graph";
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
	 * The receiver might have a different context, that context will be added to the envelope.
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
//    @NotNull
	@Column(name = "subject")
	private String subject;
	
	
    @NotNull
	@Column(name = "created_time")
	private Instant createdTime;
	
    /**
     * Deliver as notification (push), regular message in inbox, or both.
     */
    @NotNull
	@Column(name = "delivery_mode", length = 2)
	private DeliveryMode deliveryMode;

	/**
	 * Deprecated: The sender of the message.
	 */
    @Deprecated
//    @NotNull
    @ManyToOne
    @JoinColumn(name = "sender", foreignKey = @ForeignKey(name = "message_sender_fk"))
    private CommunicatorUser oldSender;

    /**
     * The recipients of the message.
     */
	@OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<Envelope> envelopes;

	/**
	 * Convenience method. Used to het the sender from an envelope, or as input from, to create a sender envelope for.
	 */
	@Transient
    private CommunicatorUser sender;
	
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
	public Instant getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(Instant createdTime) {
		this.createdTime = createdTime;
	}

	public DeliveryMode getDeliveryMode() {
		return deliveryMode;
	}

	public void setDeliveryMode(DeliveryMode deliveryMode) {
		this.deliveryMode = deliveryMode;
	}

	@Deprecated
	public CommunicatorUser getOldSender() {
		return oldSender;
	}

	@Deprecated
	public void setOldSender(CommunicatorUser sender) {
		this.oldSender = sender;
	}

	@Override
	public CommunicatorUser getSender() {
		if (sender == null) {
			findSenderEnvelope().ifPresent(env -> sender = env.getConversation().getOwner());
		}
		return sender;
	}

	/**
	 * Use this method only to prepare the sending of a message. The field is transient is used only when creating a message.
	 * 
	 * @param sender
	 */
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

	public void addRecipient(Conversation conv, String rcpContext) {
		addRecipient(new Envelope(rcpContext, conv));
	}

	public void addRecipient(Envelope env) {
		env.setMessage(this);
		getEnvelopes().add(env);
	}

	public void addSender(Conversation conv, String sndContext) {
		addSender(new Envelope(sndContext, conv));
	}

	public void addSender(Envelope env) {
		env.setMessage(this);
		env.setAckTime(this.getCreatedTime());
		env.setSender(true);
		getEnvelopes().add(env);
	}

	public Optional<Envelope> findSenderEnvelope() {
		return this.getEnvelopes().stream().filter(env -> env.isSender()).findFirst();
	}

	public String getSenderConversationRef() {
		String convUrn = null;
		Optional<Envelope> optSenderEnv = findSenderEnvelope();
		if (optSenderEnv.isPresent()) {
			convUrn = optSenderEnv.get().getConversation().getUrn();
		}
		return convUrn;
	}
	
	@Override
	public String toString() {
		return String.format("Message [%d %s '%s' %s %s '%s']", id, 
				context, subject, deliveryMode, DateTimeFormatter.ISO_INSTANT.format(createdTime), body != null ? body : "");
	}

}
