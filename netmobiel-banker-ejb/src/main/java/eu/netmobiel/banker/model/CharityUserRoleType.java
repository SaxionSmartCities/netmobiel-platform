package eu.netmobiel.banker.model;

/**
 * Generalized model of accounting purpose types. 
 * 
 * @author Jaap Reitsma
 *
 */
public enum CharityUserRoleType {
	/**
	 * The user may only view the charity: View attributes and 
	 */
	VIEWER("R"),
	/**
	 * The user can manage the charity, i.e. withdraw funds, close charity etc.
	 */
	MANAGER("M");
	
	private String code;
	 
    private CharityUserRoleType(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }

}
