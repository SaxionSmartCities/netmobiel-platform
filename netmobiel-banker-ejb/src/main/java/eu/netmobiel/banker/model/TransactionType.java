package eu.netmobiel.banker.model;

/**
 * Generalized model of accounting purpose types. 
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
	 * A user pays a counter party for a service 
	 */
	PAYMENT("PY"),
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
	 
    private TransactionType(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }

}
