package eu.netmobiel.rideshare.model;

public enum Gender {
	MAN("M"),
	FEMALE("F"),
	WONTSAY("X");

	private String code;
	 
    private Gender(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }
}
