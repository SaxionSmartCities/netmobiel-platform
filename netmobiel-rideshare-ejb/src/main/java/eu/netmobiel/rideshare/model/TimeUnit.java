package eu.netmobiel.rideshare.model;

public enum TimeUnit {
	DAY("D"),
	WEEK("W");

	private String code;
	 
    private TimeUnit(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }
}
