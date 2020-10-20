package eu.netmobiel.banker.model;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PostPersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

import eu.netmobiel.banker.util.BankerUrnHelper;
import eu.netmobiel.commons.model.ReferableObject;

/**
 * Class to capture a request to withdraw credits from the NetMobiel banker system. The WithDrawalRequest is ultimately connected to 
 * a PaymentBatch for the treasurer to process. 
 * 
 * @author Jaap Reitsma
 *
 */
@Entity
@Table(name = "withdrawal_request")
@Vetoed
@SequenceGenerator(name = "withdrawal_request_sg", sequenceName = "withdrawal_request_seq", allocationSize = 1, initialValue = 50)
public class WithdrawalRequest extends ReferableObject {
	private static final long serialVersionUID = -2350588981525248444L;
	public static final String URN_PREFIX = BankerUrnHelper.createUrnPrefix(WithdrawalRequest.class);
	public static final int DESCRIPTION_MAX_LENGTH = 128;
	public static final int ORDER_REFERENCE_MAX_LENGTH = 32;
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "withdrawal_request_sg")
    private Long id;

	/**
	 * The amount of credits to deposit
	 */
	@Column(name = "amount_credits")
	@NotNull
	@Positive
	private int amountCredits;

	/**
	 * The amount of eurocents to pay for the desired amount of credits.
	 */
	@Column(name = "amount_eurocents")
	@NotNull
	@Positive
	private int amountEurocents;
	
	/**
	 * The description that will appear on the payment page.
	 */
	@Size(max = DESCRIPTION_MAX_LENGTH)
	@NotNull
    @Column(name = "description")
	private String description;

	/**
	 * The order reference to be used by treasurer.
	 */
	@Size(max = ORDER_REFERENCE_MAX_LENGTH)
    @Column(name = "order_reference")
	private String orderReference;

	/**
	 * The withdrawal requests are ultimately  bundled in a payment batch for processing. If not part of a batch yet, then 
	 * the withdrawal processing has not yet started. 
	 */
    @ManyToOne
	@JoinColumn(name = "payment_batch", nullable = true, foreignKey = @ForeignKey(name = "withdrawal_payment_batch_fk"))
    private PaymentBatch paymentBatch;

	/**
     * The request is related to an account
     */
	@NotNull
    @ManyToOne
	@JoinColumn(name = "account", nullable = false, foreignKey = @ForeignKey(name = "withdrawal_account_fk"))
    private Account account;
    
	/**
     * The request is made by a specific user.
     */
	@NotNull
    @ManyToOne
	@JoinColumn(name = "requested_by", nullable = false, foreignKey = @ForeignKey(name = "withdrawal_requested_by_fk"))
    private BankerUser requestedBy;

	/**
     * The clearance of the request is confirmed by a specific user.
     */
    @ManyToOne
	@JoinColumn(name = "cleared_by", nullable = true, foreignKey = @ForeignKey(name = "withdrawal_cleared_by_fk"))
    private BankerUser clearedBy;
    
    /**
     * Time of creation of the request.
     */
    @Column(name = "creation_time", nullable = false, updatable = false)
    private Instant creationTime;

    /**
     * Time of completing the request
     * If null then the request is open.
     */
    @Column(name = "completed_time", nullable = true)
    private Instant completedTime;

	/**
	 * The status of the request
	 */
	@Column(name = "status", length = 1, nullable = false)
	private PaymentStatus status;

    
	public WithdrawalRequest() {
    }

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Generate an order reference based on date (year and day-of-year) and ID. Example: NBWR-20294-50  
	 */
	@PostPersist
	public void defineOrderReference() {
		this.orderReference = String.format("NMWR-%s-%d", DateTimeFormatter.ofPattern("yyD").format(creationTime.atOffset(ZoneOffset.UTC)), id);
	}
	
	@Override
	public String getUrnPrefix() {
		return URN_PREFIX;
	}

	public int getAmountCredits() {
		return amountCredits;
	}

	public void setAmountCredits(int amountCredits) {
		this.amountCredits = amountCredits;
	}

	public int getAmountEurocents() {
		return amountEurocents;
	}

	public void setAmountEurocents(int amountEurocents) {
		this.amountEurocents = amountEurocents;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public PaymentBatch getPaymentBatch() {
		return paymentBatch;
	}

	public void setPaymentBatch(PaymentBatch paymentBatch) {
		this.paymentBatch = paymentBatch;
	}

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
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

	public Instant getCompletedTime() {
		return completedTime;
	}

	public void setCompletedTime(Instant completedTime) {
		this.completedTime = completedTime;
	}

	public PaymentStatus getStatus() {
		return status;
	}

	public void setStatus(PaymentStatus status) {
		this.status = status;
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
         if (!(o instanceof WithdrawalRequest)) {
            return false;
        }
         WithdrawalRequest other = (WithdrawalRequest) o;
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
		return String.format("WithdrawalRequest [%s %s %s]", id, orderReference, status);
	}

}
