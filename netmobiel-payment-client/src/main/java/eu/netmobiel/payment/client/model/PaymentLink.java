package eu.netmobiel.payment.client.model;

import java.time.Duration;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payment Link object modelled after the JSON representation of EMS Pay.
 * 
 * @author Jaap Reitsma
 *
 */
public class PaymentLink {
	/**
	 * Id of the payment link
	 */
	public String id;
	
	/**
	 * Our internal reference to the payment order
	 */
	@JsonProperty("merchant_order_id")
    public String merchantOrderId;
    
    /**
     * The description of the payment (will also be displayed on the payment page).
     */
    public String description;
    
	/**
	 * The amount to pay in cents
	 */
	public int amount;

	/**
	 * The currency code to pay in. We only support Euro now. Format ISO 4217.
	 */
    public String currency = "EUR";

    /**
     * The duration of the period before the link will be invalidated by the payment provider. Format is a duration in ISO8601.
     */
	@JsonProperty("expiration_period")
    public Duration expirationPeriod;

    /**
     * URL of page where payment can be carried out by the user.
     */
	@JsonProperty("payment_url")
    public String paymentUrl;

    /**
     * The supported payment methods. For now iDeal only.
     */
    @JsonProperty("payment_methods")
    public String[] paymentMethods = new String[] { "ideal" };
	
	/**
	 * The URL to return to after completing the payment. The return url gets two query parameters:
	 * project_id=d21aebea-a7e9-4a06-94bc-15f1605147f1
	 * order_id=4bf9f6fa-eb1d-4737-aebb-836328f91c7f 
	 */
	@JsonProperty("return_url")
    public String returnUrl;
	
    /**
     * Timestamp when link was created.
     */
    public OffsetDateTime created;
    /**
     * Timestamp when link was modified.
     */
    public OffsetDateTime modified;
    
    /**
     * The identifier of the completed order.
     */
    @JsonProperty("completed_order_id")
    public String completedOrderId;

    /**
     * The status of the payment link (an aggregation of the underlying order status values).
     */
    public PaymentLinkStatus status;
    
    /**
     * The reason why the payment link has the assigned status.
     */
    public String reason;
    
    /**
     * Timestamp when link was completed.
     */
    public OffsetDateTime completed;
    
	@Override
	public String toString() {
		return String.format("PaymentLink [%s,  %s]", id, paymentUrl);
	}
	
}
