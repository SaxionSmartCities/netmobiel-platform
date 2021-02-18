package eu.netmobiel.profile.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
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
import javax.persistence.NamedEntityGraphs;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@NamedEntityGraphs({
	@NamedEntityGraph(name = Compliment.LIST_COMPLIMENTS_ENTITY_GRAPH,
		attributeNodes = {
			@NamedAttributeNode(value = "receiver"),
			@NamedAttributeNode(value = "sender"),
	}),
})
@Entity
@Table(name = "compliment")
@Vetoed
@Access(AccessType.FIELD)
@SequenceGenerator(name = "compliment_sg", sequenceName = "compliment_id_seq", allocationSize = 1, initialValue = 50)
public class Compliment implements Serializable {
	private static final long serialVersionUID = 7052181227403511232L;
	public static final String LIST_COMPLIMENTS_ENTITY_GRAPH = "list-compliments-entity-graph";

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "compliment_sg")
    private Long id;
	
	@NotNull
	@Column(name = "compliment", length = 2)
	private ComplimentType compliment;

	@NotNull
	@Column(name = "published")
	private Instant published;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "receiver", nullable = false, foreignKey = @ForeignKey(name = "compliment_receiver_profile_fk"))
	private Profile receiver;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sender", nullable = false, foreignKey = @ForeignKey(name = "compliment_sender_profile_fk"))
	private Profile sender;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public ComplimentType getCompliment() {
		return compliment;
	}

	public void setCompliment(ComplimentType compliment) {
		this.compliment = compliment;
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

	@Override
	public int hashCode() {
		return Objects.hash(compliment, published, receiver, sender);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Compliment)) {
			return false;
		}
		Compliment other = (Compliment) obj;
		return compliment == other.compliment && Objects.equals(published, other.published)
				&& Objects.equals(receiver, other.receiver) && Objects.equals(sender, other.sender);
	}
	
	
}
