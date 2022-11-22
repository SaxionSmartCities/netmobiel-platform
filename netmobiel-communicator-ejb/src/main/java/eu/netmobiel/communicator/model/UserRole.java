package eu.netmobiel.communicator.model;

public enum UserRole {
	GENERIC("GN"),
	PASSENGER("PG"),
	DRIVER("DR"),
	DELEGATE("DE"),
	DELEGATOR("DO");

	private String code;
	 
    private UserRole(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }

}
