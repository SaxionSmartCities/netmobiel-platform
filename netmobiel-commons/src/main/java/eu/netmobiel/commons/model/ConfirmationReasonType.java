package eu.netmobiel.commons.model;

/**
 * The type of the reason for a (negative) confirmation of a trip of the passenger.
 * The status is used as explanation why the passenger or the transport provider 
 * denied a trip (made by the passenger). The reason must be read as: 'The trip of the passenger did
 * not take place, because ....'
 * 
 * @author Jaap Reitsma
 *
 */
public enum ConfirmationReasonType {
	/**
	 * The trip was not needed anymore by the passenger.  
	 */
	TRIP_NOT_NEEDED("TNN"),
	/**
	 * The passenger used different means of transport.  
	 */
	FOUND_OTHER_TRANSPORT("FOT"),
	/**
	 * The passenger or driver did not show up.   
	 */
	NO_SHOW("NOS"),
	/**
	 * The trip did not take place at all. 
	 */
	NO_TRIP("NOT"),
	/**
	 * The passenger and driver could not agree, the matter was settled by the mediator. 
	 */
	DISPUTED("DSP"),
	/**
	 * The reason is unknown. 
	 */
	UNKNOWN("UNK");


	private String code;
	 
    private ConfirmationReasonType(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }
}
