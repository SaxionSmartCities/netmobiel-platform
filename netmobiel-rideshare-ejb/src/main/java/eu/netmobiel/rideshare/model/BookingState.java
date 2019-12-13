package eu.netmobiel.rideshare.model;

public enum BookingState {
	NEW("NEW"),
	REQUESTED("REQ"),
	CONFIRMED("CFM"),
	CANCELLED("CNC");

	private String code;
	 
    private BookingState(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }
}
