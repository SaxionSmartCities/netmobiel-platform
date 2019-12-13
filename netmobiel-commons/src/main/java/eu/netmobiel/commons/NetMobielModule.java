package eu.netmobiel.commons;

public enum NetMobielModule {
	RIDESHARE("rs"),
	PLANNER("pn"),
	MESSAGE_SERVICE("ms");

	private String code;
	 
    private NetMobielModule(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }
}
