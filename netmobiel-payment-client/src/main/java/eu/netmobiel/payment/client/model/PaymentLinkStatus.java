package eu.netmobiel.payment.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Payment link status as defined by EMS Pay.
 *  
 * @author Jaap Reitsma
 *
 */
public enum PaymentLinkStatus {
	/**
	 * New link, no attempts have been made to pay yet.
	 */
	NEW, 
	/**
	 * A payment is in progress.
	 */
    PROCESSING, 
    /**
     * At least one attempt has been made to pay, without success up to now.
     */
    ALL_UNSUCCESSFUL, 
    /**
     * The payment was successfully completed.
     */
    COMPLETED, 
    /**
     * The payment link has expired, no payment was done.
     */
    EXPIRED;
	
	@JsonCreator
    public static PaymentLinkStatus forValue(String value) {
		if (value == null) {
			return null;
		}
		return PaymentLinkStatus.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toValue() {
		return name().toLowerCase();
    }
}
