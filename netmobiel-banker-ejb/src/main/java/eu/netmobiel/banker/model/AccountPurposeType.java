package eu.netmobiel.banker.model;

/**
 * Type of account. Used to distinguish between different types of accounts and allowed operations.
 *  
 * @author Jaap Reitsma
 *
 */
public enum AccountPurposeType {
	/**
	 * A system account.
	 */
	SYSTEM("S"),
	/**
	 * A user account holding the free credits.
	 */
	CURRENT("C"),
	/**
	 * A user account for holding the premium credits.
	 */
	PREMIUM("P");
	
	private String code;
	 
    private AccountPurposeType(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }

}
