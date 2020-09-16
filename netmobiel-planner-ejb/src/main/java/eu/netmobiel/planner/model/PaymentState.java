package eu.netmobiel.planner.model;

/**
 * The state of a payment for a leg to the transport provider.
 * 
 * @author Jaap Reitsma
 *
 */
public enum PaymentState {
	/**
	 * The fare for the leg has been reserved. The transaction refers to the reservation.
	 */
	RESERVED("R"),
	/**
	 * The fare has been paid to the transport provider. The transaction refers to the final payment.  
	 */
	PAID("P"),
	/**
	 * The trip has been cancelled. The transaction refers to the release.
	 */
	CANCELLED("C");

	private String code;
	 
    private PaymentState(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }
    
}
