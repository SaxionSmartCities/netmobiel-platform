package eu.netmobiel.banker.model;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import eu.netmobiel.banker.util.BankerUrnHelper;
import eu.netmobiel.commons.model.ReferableObject;

/**
 * Class to capture a bundle of WithdrawalRequests in a single PaymentBatch for the treasurer to process. 
 * 
 * @author Jaap Reitsma
 *
 */
@Entity
@Table(name = "payment_batch")
@Vetoed
@SequenceGenerator(name = "payment_batch_sg", sequenceName = "payment_batch_seq", allocationSize = 1, initialValue = 50)
public class PaymentBatch extends ReferableObject {
	private static final long serialVersionUID = -7409373690677258054L;
	public static final String URN_PREFIX = BankerUrnHelper.createUrnPrefix(PaymentBatch.class);
	public static final int DESCRIPTION_MAX_LENGTH = 128;
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_batch_sg")
    private Long id;

    /**
     * The withdrawal requests in the batch.
     */
	@OneToMany(mappedBy = "paymentBatch", fetch = FetchType.LAZY)
	private Set<WithdrawalRequest> withdrawalRequests = new LinkedHashSet<>();

	/**
     * The batch is created by a specific user.
     */
	@NotNull
    @ManyToOne
	@JoinColumn(name = "requested_by", nullable = false, foreignKey = @ForeignKey(name = "payment_batch_requested_by_fk"))
    private BankerUser requestedBy;

	/**
     * The clearance of the batch is confirmed by a specific user.
     */
    @ManyToOne
	@JoinColumn(name = "cleared_by", nullable = true, foreignKey = @ForeignKey(name = "payment_batch_cleared_by_fk"))
    private BankerUser clearedBy;
    
    /**
     * Time of creation of the batch.
     */
    @Column(name = "creation_time", nullable = false, updatable = false)
    private Instant creationTime;

    /**
     * Time of settlement of the batch.
     * If null then not all payments are cleared yet by the treasurer. If set then all payments are settled.
     */
    @Column(name = "completion_time", nullable = true)
    private Instant completionTime;

	public PaymentBatch() {
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

	public Set<WithdrawalRequest> getWithdrawalRequests() {
		return withdrawalRequests;
	}

	public void setWithdrawalRequests(Set<WithdrawalRequest> withdrawalRequests) {
		this.withdrawalRequests = withdrawalRequests;
	}

	public BankerUser getRequestedBy() {
		return requestedBy;
	}

	public void setRequestedBy(BankerUser requestedBy) {
		this.requestedBy = requestedBy;
	}

	public BankerUser getClearedBy() {
		return clearedBy;
	}

	public void setClearedBy(BankerUser clearedBy) {
		this.clearedBy = clearedBy;
	}

	public Instant getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Instant creationTime) {
		this.creationTime = creationTime;
	}


	public Instant getCompletionTime() {
		return completionTime;
	}

	public void setCompletionTime(Instant completionTime) {
		this.completionTime = completionTime;
	}

	public void addWithdrawalRequest(WithdrawalRequest request) {
		request.setPaymentBatch(this);
        withdrawalRequests.add(request);
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
         if (!(o instanceof PaymentBatch)) {
            return false;
        }
         PaymentBatch other = (PaymentBatch) o;
        return id != null && id.equals(other.getId());
    }

	/**
	 * Using the database ID as equals test!
	 * @see https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/
	 */
    @Override
    public int hashCode() {
        return 31;
    }

	@Override
	public String toString() {
		return String.format("PaymentBatch [%s]", id);
	}

}
