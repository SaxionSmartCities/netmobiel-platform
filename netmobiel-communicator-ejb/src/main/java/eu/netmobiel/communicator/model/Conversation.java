package eu.netmobiel.communicator.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.Vetoed;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
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
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import eu.netmobiel.commons.model.ReferableObject;
import eu.netmobiel.communicator.util.CommunicatorUrnHelper;

/**
 * Class for maintaining a message thread (a conversation). Sender and each recipient have their own thread.
 * 
 * A message is categorized at creation time by the context as defined in the conversation for the sender and recipient.
 * As a consequence, it is an error to post a message with a context that is not registered in the conversations for the sender and recipient.
 * The System (as a sender) has no sender defined and thus no conversation.
 * 
 * The design choice has made to define an entity relation between Message, Envelope and Conversation. The conversation is queries at 
 * each message and then the relation is fixed. This relation is never altered. Retrieval of conversations is based on entity relations
 * and therefore as fast as possible.
 * 
 * A design alternative is to postpone the bundling of messages in conversations at query time, using a query based on
 * the contexts in the message and envelope. This is potential slower and the database must compare contexts, context should 
 * probably be an indexed property. The registration of contexts is however done by the system (at certain events), not by the user. The 
 * advantage for the user is hard to see, only database repairs are possibly quicker. Conversation queries will be slower with this design,
 * but creation of messages is faster.
 * 
 * Because the retrieval of the inbox must be as fast as possible, we choose the relation design. The creation of messages is asynchronous anyway.
 * 
 * @author Jaap Reitsma
 *
 */
@NamedEntityGraph(name = Conversation.DEFAULT_ENTITY_GRAPH,
	attributeNodes = {
		@NamedAttributeNode(value = "contexts")
})
@NamedEntityGraph(name = Conversation.FULL_ENTITY_GRAPH,
	attributeNodes = {
		@NamedAttributeNode(value = "contexts"),
		@NamedAttributeNode(value = "owner")
})

@Entity
@Table(name = "conversation")
@Vetoed
@SequenceGenerator(name = "conversation_sg", sequenceName = "conversation_id_seq", allocationSize = 1, initialValue = 50)
public class Conversation extends ReferableObject implements Serializable {

	private static final long serialVersionUID = 5034396677188994967L;
	public static final String URN_PREFIX = CommunicatorUrnHelper.createUrnPrefix(Conversation.class);
	public static final String FULL_ENTITY_GRAPH = "full-conversation-entity-graph";
	public static final String DEFAULT_ENTITY_GRAPH = "default-conversation-entity-graph";

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "conversation_sg")
    private Long id;

	/**
	 * The subject of the thread, formatted by the backend.    
	 */
    @NotNull
    @Size(max = 128)
	@Column(name = "topic")
	private String topic;
	
    /**
     * Creation time of the thread.
     */
    @NotNull
	@Column(name = "created_time")
	private Instant createdTime;
	
    /**
     * Archive timestamp of the thread.
     */
	@Column(name = "archived_time")
	private Instant archivedTime;

    /**
	 * The owner of the thread.
	 */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner", foreignKey = @ForeignKey(name = "conversation_owner_fk"))
    private CommunicatorUser owner;

    /**
     * The message contexts (of sender or recipient) of messages to bundle in this conversation.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "conversation_context", joinColumns = { 
    	@JoinColumn(name = "conversation", foreignKey = @ForeignKey(name = "conversation_context_conversation_fk")) 
    })
    @Column(name = "context", length = 32)
    @OrderBy("ASC")
    @JoinColumn(name = "conversation")	// This definition is required by OnDelete, just a copy of the same column in @CollectionTable 
    @OnDelete(action = OnDeleteAction.CASCADE)
	private Set<String> contexts;

    /**
     * The most recent message in this conversation. Only present when listing conversations. 
     */
    @Transient
    private Message recentMessage;
    
    public Conversation() {
    	this(null, null);
    }

    public Conversation(String initialContext, String aTopic) {
    	this(null, initialContext, aTopic, Instant.now());
    }
    
    public Conversation(CommunicatorUser anOwner) {
    	this(anOwner, null, null);
    }

    public Conversation(CommunicatorUser anOwner, String initialContext, String aTopic) {
    	this(anOwner, initialContext, aTopic, Instant.now());
    }

    public Conversation(CommunicatorUser anOwner, String initialContext, String aTopic, Instant aCcreationTime) {
    	this.owner = anOwner;
    	this.topic = aTopic;
    	this.createdTime = aCcreationTime;
    	this.contexts = new HashSet<>();
    	if (initialContext != null) {
    		this.contexts.add(initialContext);
    	}
    }

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

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public Instant getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(Instant createdTime) {
		this.createdTime = createdTime;
	}

	public Instant getArchivedTime() {
		return archivedTime;
	}

	public void setArchivedTime(Instant archivedTime) {
		this.archivedTime = archivedTime;
	}

	public CommunicatorUser getOwner() {
		return owner;
	}

	public void setOwner(CommunicatorUser owner) {
		this.owner = owner;
	}

	public Set<String> getContexts() {
		return contexts;
	}

	public void setContexts(Set<String> contexts) {
		this.contexts = contexts;
	}

	public Message getRecentMessage() {
		return recentMessage;
	}

	public void setRecentMessage(Message recentMessage) {
		this.recentMessage = recentMessage;
	}

	@Override
	public String toString() {
		return String.format("%s %s", StringUtils.abbreviate(getTopic(), 32), getOwner());
	}
}
