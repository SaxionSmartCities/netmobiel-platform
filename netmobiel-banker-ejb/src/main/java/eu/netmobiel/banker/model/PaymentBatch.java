package eu.netmobiel.banker.model;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.NamedSubgraph;
import javax.persistence.OneToMany;
import javax.persistence.PostPersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import eu.netmobiel.banker.util.BankerUrnHelper;
import eu.netmobiel.commons.model.ReferableObject;

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
					@NamedAttributeNode(value = "settledBy"),		
			}
	),
	@NamedEntityGraph(
			name = PaymentBatch.WITHDRAWALS_GRAPH, 
			attributeNodes = { 
					@NamedAttributeNode(value = "createdBy"),		
					@NamedAttributeNode(value = "settledBy"),		
					@NamedAttributeNode(value = "withdrawalRequests", subgraph = "subgraph.withdrawal")		
			}, subgraphs = {
					@NamedSubgraph(
							name = "subgraph.withdrawal",
							attributeNodes = {
									@NamedAttributeNode(value = "account"),		
									@NamedAttributeNode(value = "createdBy"),
									@NamedAttributeNode(value = "settledBy")
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
	 * The order reference to be used by treasurer.
	 */
	@Size(max = ORDER_REFERENCE_MAX_LENGTH)
    @Column(name = "order_reference")
	private String orderReference;

    /**
     * The withdrawal requests in the batch.
     */
	@OneToMany(mappedBy = "paymentBatch", fetch = FetchType.LAZY)
	private Set<WithdrawalRequest> withdrawalRequests = new LinkedHashSet<>();

	/**
     * The batch is created by a specific user.
     */
	@NotNull
    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by", nullable = false, foreignKey = @ForeignKey(name = "payment_batch_created_by_fk"))
    private BankerUser createdBy;

	/**
     * The clearance of the batch is confirmed by a specific user.
     */
    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "settled_by", nullable = true, foreignKey = @ForeignKey(name = "payment_batch_settled_by_fk"))
    private BankerUser settledBy;
    
    /**
     * Time of creation of the batch.
     */
    @Column(name = "creation_time", nullable = false, updatable = false)
    private Instant creationTime;

    /**
     * Time of settlement of the batch.
     * If null then not all payments are cleared yet by the treasurer. If set then all payments are settled.
     */
    @Column(name = "settlement_time", nullable = true)
    private Instant settlementTime;

	public PaymentBatch() {
    }

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Generates an order reference based on date (year and day-of-year) and ID. Example: NMPB-20294-50  
	 */
	@PostPersist
	public void defineOrderReference() {
		this.orderReference = String.format("NMPB-%s-%d", DateTimeFormatter.ofPattern("yyD").format(creationTime.atOffset(ZoneOffset.UTC)), id);
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

	public BankerUser getSettledBy() {
		return settledBy;
	}

	public void setSettledBy(BankerUser settledBy) {
		this.settledBy = settledBy;
	}

	public Instant getSettlementTime() {
		return settlementTime;
	}

	public void setSettlementTime(Instant settlementTime) {
		this.settlementTime = settlementTime;
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
