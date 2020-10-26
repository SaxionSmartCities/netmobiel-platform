package eu.netmobiel.banker.model;

/**
 * Status of the payment deposit status.
 *  
 * @author Jaap Reitsma
 *
 */
public enum PaymentStatus {
	/**
	 * A request, not yet picked up. Deposit requests are immediately active, withdrawal request are picked up in a batch.
	 */
	REQUESTED("R"),
	/**
	 * An active request, work in progress.
	 */
	ACTIVE("A"),
	/**
	 * The request has expired.
	 */
	EXPIRED("E"),
	/**
	 * The payment was successful, the credits are transferred to or from the account.
	 */
	COMPLETED("C");
	
	private String code;
	 
    private PaymentStatus(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }

}
