package eu.netmobiel.banker.model;

import java.time.Instant;

import javax.enterprise.inject.Vetoed;
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
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

import eu.netmobiel.banker.util.BankerUrnHelper;

/**
 * Class to capture a request to deposit credits into the NetMobiel banker system. The DepositRequest is connected to 
 * the PaymentLink defined by the payment Provider. 
 * 
 * @author Jaap Reitsma
 *
 */
@Entity
@Table(name = "deposit_request", uniqueConstraints = 
	@UniqueConstraint(name = "deposit_request_payment_link_id_uc", columnNames = { "payment_link_id" } )
)
@Vetoed
@SequenceGenerator(name = "deposit_request_sg", sequenceName = "deposit_request_seq", allocationSize = 1, initialValue = 50)
public class DepositRequest {
	public static final String URN_PREFIX = BankerUrnHelper.createUrnPrefix(DepositRequest.class);
	public static final int DESCRIPTION_MAX_LENGTH = 128;
	public static final int MERCHANT_ORDER_ID_MAX_LENGTH = 48;
	public static final int PAYMENT_LINK_ID_MAX_LENGTH = 48;
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "deposit_request_sg")
    private Long id;

    @Transient
    private String depositRequestRef;

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
	@Positive
	private int amountEurocents;
	
	/**
	 * The description that will appear on the payment page.
	 */
	@Size(max = DESCRIPTION_MAX_LENGTH)
	@NotNull
    @Column(name = "description", length = DESCRIPTION_MAX_LENGTH)
	private String description;

	/**
     * The request is related to an account
     */
    @ManyToOne
	@JoinColumn(name = "account", nullable = false, foreignKey = @ForeignKey(name = "deposit_request_account_fk"))
    private Account account;
    
    /**
     * Time of creation of the request.
     */
    @Column(name = "creation_time", nullable = false, updatable = false)
    private Instant creationTime;

    /**
     * Time of expiration of the request.
     */
    @Column(name = "expiration_time", nullable = false)
    private Instant exprationTime;

    /**
     * Time of completing the request
     * If null then the request is open.
     */
    @Column(name = "completed_time", nullable = true)
    private Instant completedTime;

	/**
	 * Our merchant order id issued to the payment provider
	 */
	@Size(max = MERCHANT_ORDER_ID_MAX_LENGTH)
    @Column(name = "merchant_order_id", length = MERCHANT_ORDER_ID_MAX_LENGTH)
	private String merchantOrderId;

	/**
	 * The payment link id at the payment provider
	 */
	@Size(max = PAYMENT_LINK_ID_MAX_LENGTH)
    @Column(name = "payment_link_id", length = PAYMENT_LINK_ID_MAX_LENGTH)
	private String paymentLinkId;

	/**
	 * The status of the request
	 */
	@Column(name = "status", length = 1, nullable = false)
	private PaymentStatus status;

	/**
	 * The url to return to after completing the payment. Only used to create a payment request, not stored in DB.
	 */
    @Transient
	private String returnUrl;
    
	public DepositRequest() {
    }

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	public Instant getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Instant creationTime) {
		this.creationTime = creationTime;
	}

	public Instant getExprationTime() {
		return exprationTime;
	}

	public void setExprationTime(Instant exprationTime) {
		this.exprationTime = exprationTime;
	}

	public Instant getCompletedTime() {
		return completedTime;
	}

	public void setCompletedTime(Instant completedTime) {
		this.completedTime = completedTime;
	}

	public String getMerchantOrderId() {
		return merchantOrderId;
	}

	public void setMerchantOrderId(String merchantOrderId) {
		this.merchantOrderId = merchantOrderId;
	}

	public String getPaymentLinkId() {
		return paymentLinkId;
	}

	public void setPaymentLinkId(String paymentLinkId) {
		this.paymentLinkId = paymentLinkId;
	}

	public PaymentStatus getStatus() {
		return status;
	}

	public void setStatus(PaymentStatus status) {
		this.status = status;
	}

	public String getReturnUrl() {
		return returnUrl;
	}

	public void setReturnUrl(String returnUrl) {
		this.returnUrl = returnUrl;
	}

	public String getDepositRequestRef() {
		if (depositRequestRef == null) {
			depositRequestRef = BankerUrnHelper.createUrn(DepositRequest.URN_PREFIX, getId());
		}
		return depositRequestRef;
	}

}
