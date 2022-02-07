package eu.netmobiel.banker.model;

/**
 * Type of account usage.
 *  
 * @author Jaap Reitsma
 *
 */
public enum AccountPurposeType {
	SYSTEM("S"),
	CURRENT("C"),
	PREMIUM("P");
	
	private String code;
	 
    private AccountPurposeType(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }

}
