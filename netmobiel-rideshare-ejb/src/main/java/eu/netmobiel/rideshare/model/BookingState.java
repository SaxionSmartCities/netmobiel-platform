package eu.netmobiel.rideshare.model;

public enum BookingState {
	/**
	 * Initial state. 
	 */
	NEW("NEW"),
	/**
	 * The driver has proposed a booking as part of a shout-out solution. 
	 */
	PROPOSED("PRO"),
	/**
	 * The traveller has requested a booking, the driver has not confirmed yet.
	 */
	REQUESTED("REQ"),
	/**
	 * The driver (previous state is requested) or traveller (previous state is proposed) has confirmed the booking.
	 */
	CONFIRMED("CFM"),
	/**
	 * The booking is cancelled by the driver or the traveller.
	 */
	CANCELLED("CNC");

	private String code;
	 
    private BookingState(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }
}
