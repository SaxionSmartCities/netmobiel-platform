package eu.netmobiel.commons;

import java.util.stream.Stream;

public enum NetMobielModule {
	BANKER("bn"),
	COMMUNICATOR("cm"),
	KEYCLOAK("kc"),
	PLANNER("pn"),
	PROFILE("pf"),
	RIDESHARE("rs");

	private String code;
	 
    private NetMobielModule(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }
    
    public static NetMobielModule getEnum(String code) {
		if (code == null) {
            return null;
        }
		return Stream.of(NetMobielModule.values())
		          .filter(c -> c.getCode().equals(code))
		          .findFirst()
		          .orElseThrow(() -> new IllegalArgumentException("Don't understand '" + code + "'"));
	}
}
