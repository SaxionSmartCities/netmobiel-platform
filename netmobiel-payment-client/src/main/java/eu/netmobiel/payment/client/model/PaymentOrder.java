package eu.netmobiel.payment.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payment order object modelled after the JSON representation of EMS Pay. Only the relevant fields are added.
 * @author Jaap Reitsma
 *
 */
public class PaymentOrder {
	/**
	 * Id of the payment order
	 */
	public String id;
	
	/**
	 * Our internal reference to the payment order
	 */
	@JsonProperty("merchant_order_id")
    public String merchantOrderId;
    
    /**
     * The related payment link id
     */
	@JsonProperty("related_payment_link_id")
    public String relatedPaymentLinkId;
    
	@Override
	public String toString() {
		return String.format("PaymentOrder [%s -->  %s]", id, relatedPaymentLinkId);
	}
	
}
