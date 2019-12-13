package eu.netmobiel.rideshare.model;

public enum RideScope {
	THIS("this"),
	THIS_AND_FOLLOWING("this-and-following");

	private String code;
	 
    private RideScope(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }
}
