package eu.netmobiel.banker.model;

import java.util.Map;

/**
 * Generalized model of accounting purpose types. The type is in particular
 * used to inform the user in the statement overview. 
 * 
 * @author Jaap Reitsma
 *
 */
public enum TransactionType {
	/**
	 * Credits are deposited, i.e. credits are added to the system.
	 */
	DEPOSIT("DP"),
	/**
	 * Credits are withdrawn, i.e., removed from the system.
	 */
	WITHDRAWAL("WD"),
	/**
	 * A user pays a counterparty for a service.  
	 */
	PAYMENT("PY"),
	/**
	 * A party refunds a payment. This occurs when a validation is reversed and an earlier 
	 * payment for a ride is refunded. This may also occur when a premium was given for a ride and 
	 * the subsequent payment was cancelled.  
	 */
	REFUND("RF"),
	/**
	 * Credits are reserved for a yet-to-be-delivered service.
	 */
	RESERVATION("RS"),
	/**
	 * Previously reserved credits are given back, in preparation for 
	 * an actual payment or due to a cancellation. 
	 */
	RELEASE("RL");

	private String code;
	private static Map<TransactionType, TransactionType> reversedPurposeMap = 
			Map.ofEntries(
					Map.entry(DEPOSIT, WITHDRAWAL),
					Map.entry(WITHDRAWAL, DEPOSIT),
					Map.entry(PAYMENT, REFUND),
					Map.entry(REFUND, PAYMENT),
					Map.entry(RESERVATION, RELEASE),
					Map.entry(RELEASE, RESERVATION)
			);
	 
    private TransactionType(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }

    public TransactionType reverse() {
    	return reversedPurposeMap.get(this);
    }
}
