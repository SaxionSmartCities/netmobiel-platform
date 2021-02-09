package eu.netmobiel.profile.model;

public enum ComplimentType {
	SAME_INTERESTS("SI"),
	ON_TIME("TM"),
	TALKS_EASILY("TK"),
	SOCIABLE("SC"),
	NEATLY("NT"),
	NICE_CAR("CR");

	private String code;
	 
    private ComplimentType(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }

}
