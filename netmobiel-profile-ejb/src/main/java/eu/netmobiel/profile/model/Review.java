package eu.netmobiel.profile.model;

import java.io.Serializable;
import java.time.Instant;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
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
import javax.validation.constraints.Size;

@NamedEntityGraphs({
	@NamedEntityGraph(name = Review.LIST_REVIEWS_ENTITY_GRAPH,
		attributeNodes = {
			@NamedAttributeNode(value = "receiver"),
			@NamedAttributeNode(value = "sender"),
	}),
})
@Entity
@Table(name = "review")
@Vetoed
@Access(AccessType.FIELD)
@SequenceGenerator(name = "review_sg", sequenceName = "review_id_seq", allocationSize = 1, initialValue = 50)
public class Review implements Serializable {
	private static final long serialVersionUID = 7052181227403511232L;
	public static final String LIST_REVIEWS_ENTITY_GRAPH = "list-reviews-entity-graph";

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "review_sg")
    private Long id;
	
	@NotNull
	@Size(max = 256)
	@Column(name = "review")
	private String review;

	@NotNull
	@Column(name = "published")
	private Instant published;
	
	@ManyToOne
	@JoinColumn(name = "receiver", nullable = false, foreignKey = @ForeignKey(name = "review_receiver_profile_fk"))
	private Profile receiver;

	@ManyToOne
	@JoinColumn(name = "sender", nullable = false, foreignKey = @ForeignKey(name = "review_sender_profile_fk"))
	private Profile sender;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getReview() {
		return review;
	}

	public void setReview(String review) {
		this.review = review;
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
	
	
}
