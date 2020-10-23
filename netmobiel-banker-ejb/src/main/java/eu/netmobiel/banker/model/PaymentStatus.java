package eu.netmobiel.banker.model;

/**
 * Status of the payment deposit status.
 *  
 * @author Jaap Reitsma
 *
 */
public enum PaymentStatus {
	/**
	 * An active request, not expired and not completed yet.
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
