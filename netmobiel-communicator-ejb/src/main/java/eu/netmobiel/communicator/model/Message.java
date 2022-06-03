package eu.netmobiel.communicator.model;

import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.inject.Vetoed;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
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
import eu.netmobiel.commons.model.ReferableObject;
import eu.netmobiel.communicator.util.CommunicatorUrnHelper;

@NamedEntityGraph(
		name = Message.MESSAGE_ENVELOPES_ENTITY_GRAPH, 
		attributeNodes = { 
				@NamedAttributeNode(value = "envelopes", subgraph = "envelope-details")		
		}, subgraphs = {
				@NamedSubgraph(
						name = "envelope-details",
						attributeNodes = {
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
public class Message extends ReferableObject implements NetMobielMessage, Serializable {

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
    @NotNull
	@Size(max = MAX_MESSAGE_SIZE)
	@Column(name = "body")
	private String body;
	
	/**
	 * The context of the message. The context is a urn, referring to an object in the system.
	 * This is the shared context of the message as perceived by both the sender and the recipient.
	 * The system is a silent sender, i.e. it has no envelope.
	 */
	@Size(max = 32)
    @NotNull
	@Column(name = "context")
	private String context;
	
    @NotNull
	@Column(name = "created_time", updatable = false)
	private Instant createdTime;
	
    /**
     * Deliver as notification (push), regular message in inbox, or both.
     */
    @NotNull
	@Column(name = "delivery_mode", length = 2)
	private DeliveryMode deliveryMode;

    /**
     * The recipients of the message.
     */
	@OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<Envelope> envelopes;

	/**
	 * Convenience method. Used to set the sender from the envelope, or as input to create a sender envelope for.
	 */
	@Transient
    private CommunicatorUser sender;
	
	/**
	 * Convenience method. Used to set the sender context from the sender envelope, or as input to create a sender envelope for.
	 */
	@Transient
    private String senderContext;

	@Override
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public String getUrnPrefix() {
		return URN_PREFIX;
	}

	@Override
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

	public String getSenderContext() {
		if (senderContext == null) {
			findSenderEnvelope().ifPresent(env -> senderContext = env.getContext());
		}
		return senderContext;
	}

	/**
	 * Use this method only to prepare the sending of a message. The field is transient is used only when creating a message.
	 * 
	 * @param senderContext
	 */
	public void setSenderContext(String senderContext) {
		this.senderContext = senderContext;
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

	public void addSender(String aContext, Conversation conv) {
		// Sender context is the message context. 
		addSender(new Envelope(aContext, conv));
	}

	public void addSender(Envelope env) {
		env.setAckTime(this.getCreatedTime());
		env.setSender(true);
		env.setMessage(this);
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
	
	public boolean isUserParticipant(CommunicatorUser user) {
		return envelopes.stream()
				.filter(env -> env.getConversation().getOwner().equals(user))
				.findAny()
				.isPresent();
	}
	
	@Override
	public String toString() {
		return String.format("Message [%d %s %s %s '%s']", id, 
				context, deliveryMode, createdTime == null ? "": DateTimeFormatter.ISO_INSTANT.format(createdTime), body != null ? body : "");
	}

	public static MessageBuilder create() {
		return new MessageBuilder();
	}
	
	public static class MessageBuilder {
		private Message message;
		
		public MessageBuilder() {
			message = new Message();
		}
		
		public Message getMessage() {
			return message;
		}
		
		public MessageBuilder withContext(String aContext) {
			message.setContext(aContext);
			return this;
		}

		public MessageBuilder withBody(String aBody) {
			message.setBody(aBody);
			return this;
		}

		public MessageBuilder withCreatedTime(Instant anInstant) {
			message.setCreatedTime(anInstant);
			return this;
		}

		public MessageBuilder withDeliveryMode(DeliveryMode aMode) {
			message.setDeliveryMode(aMode);
			return this;
		}

		public MessageBuilder withSender(String context, String aManagedIdentity) {
			CommunicatorUser sender = new CommunicatorUser(aManagedIdentity);
			return withSender(context, new Conversation(sender, context));
		}

		public MessageBuilder withSender(String context, Conversation senderConversation) {
			message.addSender(context, senderConversation);
			return this;
		}

		public Conversation.ConversationBuilder addEnvelope() {
			return addEnvelope((String)null);
		}
		
		public Conversation.ConversationBuilder addEnvelope(String recipientContext) {
			Conversation.ConversationBuilder cb = new Conversation.ConversationBuilder(this);
			message.addRecipient(new Envelope(recipientContext, cb.getConversation()));
			return cb;
		}
		
		public MessageBuilder addEnvelope(Envelope env) {
			message.addRecipient(env);
			return this;
		}

		public Message buildMessage() {
			if (message.getCreatedTime() == null) {
				message.setCreatedTime(Instant.now());
			}
			if (message.getBody() == null || message.getBody().isBlank()) { 
				throw new IllegalStateException("Specify a message body");
			}
			if (message.getContext() == null || message.getContext().isBlank()) {
				throw new IllegalStateException("Specify a message context");
			}
			if (message.getEnvelopes().isEmpty()) { 
				throw new IllegalStateException("Specify at least one recipient");
			}
			if (message.getDeliveryMode() == null) { 
				message.setDeliveryMode(DeliveryMode.ALL);
			}
			message.getEnvelopes().stream()
					.filter(env -> env.getContext() == null)
					.forEach(env -> env.setContext(env.getMessage().getContext()));
			if (message.getEnvelopes().stream()
					.filter(env -> env.getContext().isBlank())
					.findAny().isPresent()) {
				throw new IllegalStateException("Envelope context cannot be blank");
			}
			List<Envelope> senders = message.getEnvelopes().stream()
				.filter(env -> env.isSender())
				.collect(Collectors.toList());
			// Sender cardinality is 0..1 
			if (senders.size() > 1) {
				throw new IllegalStateException("There can be only one sender");
			}
			// Sender envelopes are immediately acknowledged
			senders.forEach(env -> env.setAckTime(message.getCreatedTime()));
			return message;
		}
	}
}
