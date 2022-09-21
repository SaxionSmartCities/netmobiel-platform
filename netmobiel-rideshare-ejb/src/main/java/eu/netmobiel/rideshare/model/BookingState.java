package eu.netmobiel.rideshare.model;

public enum BookingState {
	/**
	 * Initial state. This state is used to issues Booking ID's for the TOMP.
	 */
	NEW("NEW"),
	/**
	 * Expired state. A booking expires when not committed quick enough.   
	 */
	EXPIRED("EXP"),
	/**
	 * Released state. A booking is released when cancelled before it was confirmed.   
	 */
	RELEASED("RLS"),
	/**
	 * The driver has proposed a booking as part of a shout-out solution. 
	 */
	PROPOSED("PRO"),
	/**
	 * The traveller has requested a booking, the driver has not confirmed yet. 
	 * In TOMP API this is comparable to 'PENDING'.
	 */
	REQUESTED("REQ"),
	/**
	 * The booking has been confirmed by both parties.
	 */
	CONFIRMED("CFM"),
	/**
	 * The (confirmed) booking is cancelled by the driver or the traveller.
	 */
	CANCELLED("CNC");

	private String code;
	 
    private BookingState(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }
    
    public boolean isFinal() {
    	return this == EXPIRED || this == RELEASED || this == CANCELLED; 
    }
}
