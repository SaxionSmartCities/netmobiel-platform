package eu.netmobiel.commons;

public enum NetMobielModule {
	BANKER("bn"),
	COMMUNICATOR("cm"),
	KEYCLOAK("kc"),
	PLANNER("pn"),
	RIDESHARE("rs");

	private String code;
	 
    private NetMobielModule(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }
}
