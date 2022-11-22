package eu.netmobiel.rideshare.model;

public enum CarType {
	CONVERTIBLE("CON"),
	COUPE("COP"),
	ESTATE("EST"),
	HATCHBACK("HAT"),
	MINIVAN("MVN"),
	OTHER("OTH"),
	SALOON("SAL"),
	SUV("SUV"),
	VAN("VAN");
	
	private String code;
	 
    private CarType(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }

}
