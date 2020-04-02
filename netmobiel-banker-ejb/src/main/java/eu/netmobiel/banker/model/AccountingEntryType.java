package eu.netmobiel.banker.model;

/**
 * Type of accounting entry.
 *  
 * @author Jaap Reitsma
 *
 */
public enum AccountingEntryType {
	DEBIT("D"),
	CREDIT("C");
	
	private String code;
	 
    private AccountingEntryType(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }

}
