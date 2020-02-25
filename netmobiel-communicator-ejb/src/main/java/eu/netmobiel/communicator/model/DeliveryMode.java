package eu.netmobiel.communicator.model;

public enum DeliveryMode {
	MESSAGE("MS"),
	NOTIFICATION("NT"),
	ALL("AL");
	
	private String code;
	 
    private DeliveryMode(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }

}
