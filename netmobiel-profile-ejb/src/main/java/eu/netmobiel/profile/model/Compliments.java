package eu.netmobiel.profile.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
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
import javax.persistence.NamedEntityGraphs;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@NamedEntityGraphs({
	@NamedEntityGraph(name = Compliments.LIST_COMPLIMENTS_ENTITY_GRAPH,
		attributeNodes = {
			@NamedAttributeNode(value = "compliments"),
			@NamedAttributeNode(value = "receiver"),
			@NamedAttributeNode(value = "sender"),
	}),
})
@Entity
@Table(name = "compliment_set", uniqueConstraints = {
	    @UniqueConstraint(name = "cs_compliment_set_unique", columnNames = { "receiver", "context" })
})
@Vetoed
@Access(AccessType.FIELD)
@SequenceGenerator(name = "compliment_set_sg", sequenceName = "compliment_set_id_seq", allocationSize = 1, initialValue = 50)
public class Compliments implements Serializable {
	private static final long serialVersionUID = 7052181227403511232L;
	public static final String LIST_COMPLIMENTS_ENTITY_GRAPH = "list-compliments-entity-graph";

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "compliment_set_sg")
    private Long id;
	
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "compliment", joinColumns = { 
    	@JoinColumn(name = "compliment_set", foreignKey = @ForeignKey(name = "compliment_compliment_set_fk")) 
    })
    @Column(name = "compliment", length = 2)
    @OrderBy("ASC")
    @JoinColumn(name = "compliment_set")	// This definition is required by OnDelete, just a copy of the same column in @CollectionTable 
    @OnDelete(action = OnDeleteAction.CASCADE)
	private Set<ComplimentType> compliments;

	@NotNull
	@Column(name = "published")
	private Instant published;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "receiver", nullable = false, foreignKey = @ForeignKey(name = "compliment_receiver_profile_fk"))
	private Profile receiver;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sender", nullable = false, foreignKey = @ForeignKey(name = "compliment_sender_profile_fk"))
	private Profile sender;

	/**
	 * The context of the compliment. The context is a urn, referring to an object in the system.
	 * The context concerns the trip (passenger) or ride (driver) that is owned by the receiver.
	 */
	@Size(max = 32)
    @NotNull
	@Column(name = "context")
	private String context;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Instant getPublished() {
		return published;
	}

	public void setPublished(Instant published) {
		this.published = published;
	}

	public Profile getReceiver() {
		return receiver;
	}

	public void setReceiver(Profile receiver) {
		this.receiver = receiver;
	}

	public Profile getSender() {
		return sender;
	}

	public void setSender(Profile sender) {
		this.sender = sender;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public Set<ComplimentType> getCompliments() {
		return compliments;
	}

	public void setCompliments(Set<ComplimentType> compliments) {
		this.compliments = compliments;
	}

	@Override
	public int hashCode() {
		return Objects.hash(context, receiver);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Compliments)) {
			return false;
		}
		Compliments other = (Compliments) obj;
		return Objects.equals(context, other.context)
				&& Objects.equals(receiver, other.receiver);
	}
	
	
}
