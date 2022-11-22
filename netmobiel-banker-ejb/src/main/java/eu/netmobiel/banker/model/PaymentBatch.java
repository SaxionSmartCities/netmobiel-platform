package eu.netmobiel.banker.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Basic;
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
import javax.persistence.NamedSubgraph;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PostPersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import eu.netmobiel.banker.util.BankerUrnHelper;
import eu.netmobiel.banker.validator.IBANBankAccount;
import eu.netmobiel.commons.model.ReferableObject;
import eu.netmobiel.commons.util.UrnHelper;

/**
 * Class to capture a bundle of WithdrawalRequests in a single PaymentBatch for the treasurer to process. 
 * 
 * @author Jaap Reitsma
 *
 */
@NamedEntityGraphs({
	@NamedEntityGraph(
			name = PaymentBatch.LIST_GRAPH, 
			attributeNodes = { 
					@NamedAttributeNode(value = "createdBy"),		
					@NamedAttributeNode(value = "modifiedBy"),		
			}
	),
	@NamedEntityGraph(
			name = PaymentBatch.WITHDRAWALS_GRAPH, 
			attributeNodes = { 
					@NamedAttributeNode(value = "createdBy"),		
					@NamedAttributeNode(value = "modifiedBy"),		
					@NamedAttributeNode(value = "withdrawalRequests", subgraph = "subgraph.withdrawal")		
			}, subgraphs = {
					@NamedSubgraph(
							name = "subgraph.withdrawal",
							attributeNodes = {
									@NamedAttributeNode(value = "account"),		
									@NamedAttributeNode(value = "createdBy"),
									@NamedAttributeNode(value = "modifiedBy")
							}
					)
			}
	)
})
@Entity
@Table(name = "payment_batch")
@Vetoed
@SequenceGenerator(name = "payment_batch_sg", sequenceName = "payment_batch_seq", allocationSize = 1, initialValue = 50)
public class PaymentBatch extends ReferableObject {
	private static final long serialVersionUID = -7409373690677258054L;
	public static final String URN_PREFIX = BankerUrnHelper.createUrnPrefix(PaymentBatch.class);
	public static final int ORDER_REFERENCE_MAX_LENGTH = 32;
	public static final String LIST_GRAPH = "payment-batch-list-graph";
	public static final String WITHDRAWALS_GRAPH = "payment-batch-withdrawals-graph";

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_batch_sg")
    private Long id;

	/**
	 * Record version for optimistic locking.
	 */
	@Version
	@Column(name = "version")
	private int version;
	
	/**
	 * The order reference to be used by treasurer. Can be null because the record ID is in the order refrence.
	 */
	@Size(max = ORDER_REFERENCE_MAX_LENGTH)
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "order_reference")
	private String orderReference;

    /**
     * The withdrawal requests in the batch.
     */
	@OneToMany(mappedBy = "paymentBatch", fetch = FetchType.LAZY)
	@OrderBy("id ASC")
	private Set<WithdrawalRequest> withdrawalRequests = new LinkedHashSet<>();

	/**
     * The batch is created by a specific user.
     */
	@NotNull
    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by", foreignKey = @ForeignKey(name = "payment_batch_created_by_fk"))
    private BankerUser createdBy;

    /**
     * Time of creation of the batch.
     */
    @NotNull
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "creation_time", updatable = false)
    private Instant creationTime;

	/**
     * The modification of the batch is done by a specific user.
     */
	@NotNull
    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "modified_by", foreignKey = @ForeignKey(name = "payment_batch_modified_by_fk"))
    private BankerUser modifiedBy;
    
    /**
     * Time of modification of the batch (i.e. updating the status).
     */
    @NotNull
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "modification_time")
    private Instant modificationTime;

	/**
	 * The status of the batch.
	 */
	@Column(name = "status", length = 1, nullable = false)
	private PaymentStatus status;

	/**
	 * The reason for the current status.
	 */
	@Size(max = 256)
	@Column(name = "reason")
	private String reason;
	
	@NotNull
    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "originator_account", nullable = false, foreignKey = @ForeignKey(name = "payment_batch_originator_account_fk"))
    private Account originatorAccount;

	/**
	 * The originator IBAN number. This is copied at the time of the creation of the payment batch to prevent change of bank 
	 * account during processing of the batch or afterwards (i.e. don't change the history). 
	 */
	@NotNull
    @IBANBankAccount
	@Size(max = 48)
    @Column(name = "originator_iban")
    private String originatorIban;

	/**
	 * The holder of the originator's IBAN account. A copy, see IBAN comment.
	 */
	@NotNull
	@Size(max = 96)
    @Column(name = "originator_iban_holder")
    private String originatorIbanHolder;

    /**
     * The number of withdrawal requests in this batch. 
     */
	@PositiveOrZero
	@NotNull
	@Column(name = "nr_requests")
    private int nrRequests;
    
	/**
	 * The requested amount of eurocents in this batch.
	 */
	@Column(name = "amount_requested_eurocents")
	@NotNull
	@Positive
	private int amountRequestedEurocents;

	/**
	 * The settled amount of eurocents in this batch.
	 */
	@Column(name = "amount_settled_eurocents")
	@NotNull
	@PositiveOrZero
	private int amountSettledEurocents;

	@Override
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Generates an order reference based on date (year and day-of-year) and ID. Example: P20294-50  
	 */
	@PostPersist
	public void defineOrderReference() {
		OffsetDateTime date = creationTime.atOffset(ZoneOffset.UTC);
		int dayOfYear = date.getDayOfYear();
		int year = date.getYear() % 100;
		this.orderReference = String.format("P%02d%03d-%d", year, dayOfYear, id);
	}

	public String getOrderReference() {
		return orderReference;
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

	public BankerUser getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(BankerUser createdBy) {
		this.createdBy = createdBy;
	}

	public Instant getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Instant creationTime) {
		this.creationTime = creationTime;
	}

	public BankerUser getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(BankerUser modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public Instant getModificationTime() {
		return modificationTime;
	}

	public void setModificationTime(Instant modificationTime) {
		this.modificationTime = modificationTime;
	}

	public void addWithdrawalRequest(WithdrawalRequest request) {
		request.setPaymentBatch(this);
		request.setStatus(PaymentStatus.ACTIVE);
        withdrawalRequests.add(request);
    }
 

	public PaymentStatus getStatus() {
		return status;
	}

	public void setStatus(PaymentStatus status) {
		this.status = status;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public Account getOriginatorAccount() {
		return originatorAccount;
	}

	public String getOriginatorAccountRef() {
		return getOriginatorAccount() != null ? UrnHelper.createUrn(Account.URN_PREFIX, getOriginatorAccount().getId()) : null;
	}

	public void setOriginatorAccount(Account originatorAccount) {
		this.originatorAccount = originatorAccount;
	}

	public String getOriginatorIban() {
		return originatorIban;
	}

	public void setOriginatorIban(String originatorIban) {
		this.originatorIban = originatorIban;
	}

	public String getOriginatorIbanHolder() {
		return originatorIbanHolder;
	}

	public void setOriginatorIbanHolder(String originatorIbanHolder) {
		this.originatorIbanHolder = originatorIbanHolder;
	}

	public int getNrRequests() {
		return nrRequests;
	}

	public void setNrRequests(int nrRequests) {
		this.nrRequests = nrRequests;
	}

	public int getAmountRequestedEurocents() {
		return amountRequestedEurocents;
	}

	public void setAmountRequestedEurocents(int amountRequestedEurocents) {
		this.amountRequestedEurocents = amountRequestedEurocents;
	}

	public int getAmountSettledEurocents() {
		return amountSettledEurocents;
	}

	public void setAmountSettledEurocents(int amountSettledEurocents) {
		this.amountSettledEurocents = amountSettledEurocents;
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
