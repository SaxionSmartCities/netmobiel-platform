package eu.netmobiel.commons;

public enum NetMobielModule {
	RIDESHARE("rs"),
	PLANNER("pn"),
	COMMUNICATOR("cm");

	private String code;
	 
    private NetMobielModule(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }
}
