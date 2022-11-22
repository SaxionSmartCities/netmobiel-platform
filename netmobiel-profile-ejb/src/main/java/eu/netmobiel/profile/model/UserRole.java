package eu.netmobiel.profile.model;

public enum UserRole {
	PASSENGER("PG"),
	DRIVER("DR"),
	BOTH("BT");

	private String code;
	 
    private UserRole(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }

}
