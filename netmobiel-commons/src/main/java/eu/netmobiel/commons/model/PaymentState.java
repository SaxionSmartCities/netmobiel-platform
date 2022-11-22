package eu.netmobiel.commons.model;

/**
 * The state of a payment for a leg to the transport provider.
 * 
 * @author Jaap Reitsma
 *
 */
public enum PaymentState {
	/**
	 * The fare for the leg has been reserved. The transaction refers to the reservation 
	 * (in case of benefactor).
	 */
	RESERVED("R"),
	/**
	 * The fare has been paid to the transport provider. The transaction refers to the final payment 
	 * (in case of beneficiary).  
	 */
	PAID("P"),
	/**
	 * The trip has been cancelled. The transaction refers to the release (in case of benefactor).
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
