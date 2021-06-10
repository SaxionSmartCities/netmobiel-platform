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
import javax.validation.constraints.Size;

import eu.netmobiel.commons.model.ReferableObject;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.profile.util.ProfileUrnHelper;

/**
 *  The Delegation relation between two profiles during a specific period of time.
 * 
 * @author Jaap Reitsma
 *
 */
@NamedEntityGraphs({
	@NamedEntityGraph(name = Delegation.DEFAULT_ENTITY_GRAPH
	),
	@NamedEntityGraph(name = Delegation.PROFILES_ENTITY_GRAPH, 
		attributeNodes = {
				@NamedAttributeNode(value = "delegate"),
				@NamedAttributeNode(value = "delegator"),
		}
	),
	@NamedEntityGraph(name = Delegation.DELEGATE_PROFILE_ENTITY_GRAPH, 
	attributeNodes = {
			@NamedAttributeNode(value = "delegate"),
	}
),
	@NamedEntityGraph(name = Delegation.DELEGATOR_PROFILE_ENTITY_GRAPH, 
	attributeNodes = {
			@NamedAttributeNode(value = "delegator"),
	}
)
})
@Entity
@Table(name = "delegation")
@Vetoed
@SequenceGenerator(name = "delegation_sg", sequenceName = "delegation_id_seq", allocationSize = 1, initialValue = 50)
@Access(AccessType.FIELD)
public class Delegation extends ReferableObject implements Serializable {
	private static final long serialVersionUID = 781083126023747323L;

	public static final String URN_PREFIX = ProfileUrnHelper.createUrnPrefix("delegation");
	public static final String DEFAULT_ENTITY_GRAPH = "default-delegation-entity-graph";
	public static final String PROFILES_ENTITY_GRAPH = "profiles-delegation-entity-graph";
	public static final String DELEGATOR_PROFILE_ENTITY_GRAPH = "delegator-profile-delegation-entity-graph";
	public static final String DELEGATE_PROFILE_ENTITY_GRAPH = "delegate-profile-delegation-entity-graph";
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "delegation_sg")
	@Access(AccessType.PROPERTY)
    private Long id;

	/**
	 * The time of the submission request for the delegation. 
	 */
    @NotNull
    @Column(name = "submission_time")
    private Instant submissionTime;

	/**
	 * The activation time of the delegation. If null then the activation process is not yet finished.
	 */
    @Column(name = "activation_time")
    private Instant activationTime;

	/**
	 * The revocation time of the delegation. If null while activation time is set then the delegation is still active.
	 */
    @Column(name = "revocation_time")
    private Instant revocationTime;

    /**
     * The person who has accepted to act as a representative.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegate", foreignKey = @ForeignKey(name = "delegation_delegate_fk"))
    private Profile delegate;

    
    /**
     * The person being represented.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegator", foreignKey = @ForeignKey(name = "delegation_delegator_fk"))
    private Profile delegator;

    /**
     * The code sent to the delegator in order for the acceptance process to settle the relation between delegate and delegator.  
     */
    @Size(max = 32)
    @Column(name = "activation_code")
    private String activationCode;
    
	/**
	 * The time of the transmission of the activation code. If null then no code is sent (for example, when admin has activated the delegation).
	 */
    @Column(name = "activation_code_sent_time")
    private Instant activationCodeSentTime;

    /**
     * The id of the SMS sent by the SMS provider.
     */
    @Size(max = 32)
    @Column(name = "sms_id")
    private String smsId;

    /**
     * In case of a transfer of delegation: The previous delegation from which to transfer.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_from", foreignKey = @ForeignKey(name = "delegation_transfer_from_fk"))
    private Delegation transferFrom;

    public Delegation() {
    }
    
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

	public Instant getSubmissionTime() {
		return submissionTime;
	}

	public void setSubmissionTime(Instant submissionTime) {
		this.submissionTime = submissionTime;
	}

	public Instant getActivationTime() {
		return activationTime;
	}

	public void setActivationTime(Instant activationTime) {
		this.activationTime = activationTime;
	}

	public Instant getRevocationTime() {
		return revocationTime;
	}

	public void setRevocationTime(Instant revocationTime) {
		this.revocationTime = revocationTime;
	}

	public Profile getDelegate() {
		return delegate;
	}

	public void setDelegate(Profile delegate) {
		this.delegate = delegate;
	}

	public Profile getDelegator() {
		return delegator;
	}

	public void setDelegator(Profile delegator) {
		this.delegator = delegator;
	}

	public String getActivationCode() {
		return activationCode;
	}

	public void setActivationCode(String activationCode) {
		this.activationCode = activationCode;
	}

	public Instant getActivationCodeSentTime() {
		return activationCodeSentTime;
	}

	public void setActivationCodeSentTime(Instant activationCodeSentTime) {
		this.activationCodeSentTime = activationCodeSentTime;
	}

	public String getSmsId() {
		return smsId;
	}

	public void setSmsId(String smsId) {
		this.smsId = smsId;
	}

	public Delegation getTransferFrom() {
		return transferFrom;
	}

	public void setTransferFrom(Delegation transferFrom) {
		this.transferFrom = transferFrom;
	}

	public String getDelegateRef() {
		String ref = null;
		if (getDelegate() != null) {
			ref = UrnHelper.createUrn(Profile.URN_PREFIX, getDelegate().getId());
		}
		return ref;
	}

	public String getDelegatorRef() {
		String ref = null;
		if (getDelegator() != null) {
			ref = UrnHelper.createUrn(Profile.URN_PREFIX, getDelegator().getId());
		}
		return ref;
	}

	/**
	 * Using the database ID as equals test!
	 * @see https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/
	 */
	@Override
    public boolean equals(Object o) {
        if (this == o) {
        	return true;
        }
         if (!(o instanceof Delegation)) {
            return false;
        }
        Delegation other = (Delegation) o;
        return Objects.equals(getId(), other.getId());
    }

	/**
	 * Using the database ID as equals test!
	 * @see https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/
	 */
    @Override
    public int hashCode() {
        return 31;
    }
}
