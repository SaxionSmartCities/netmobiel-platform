package eu.netmobiel.profile.model;

public enum LuggageOption {
	GROCERIES("GR"),
	HANDLUGGAGE("HL"),
	PET("PT"),
	STROLLER("ST");

	private String code;
	 
    private LuggageOption(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }

}
