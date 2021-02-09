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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "compliment")
@Vetoed
@Access(AccessType.FIELD)
@SequenceGenerator(name = "compliment_sg", sequenceName = "compliment_id_seq", allocationSize = 1, initialValue = 50)
public class Compliment implements Serializable {
	private static final long serialVersionUID = 7052181227403511232L;

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "compliment_sg")
    private Long id;
	
	@NotNull
	@Column(name = "compliment", length = 2)
	private ComplimentType compliment;

	@NotNull
	@Column(name = "published")
	private Instant published;
	
	@ManyToOne
	@JoinColumn(name = "receiver", nullable = false, foreignKey = @ForeignKey(name = "compliment_receiver_profile_fk"))
	private Profile receiver;

	@ManyToOne
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
	
	
}
