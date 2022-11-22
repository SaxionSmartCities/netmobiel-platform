package eu.netmobiel.profile.model;

public enum UserEventType {
	/**
	 * The user sees at least one Call-To-Action.
	 */
	CTA_IN_VIEW("CV"),
	/**
	 * The user selects a Call-To-Action.
	 */
	CTA_SELECTED("CS"),
	/**
	 * The user is viewing a page in the GUI
	 */
	PAGE_VISIT("PV");

	private String code;
	 
    private UserEventType(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }

}
