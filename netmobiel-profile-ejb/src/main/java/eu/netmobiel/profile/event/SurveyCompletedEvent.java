package eu.netmobiel.profile.event;

import java.io.Serializable;

import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.model.SurveyInteraction;

/**
 * Event issued on successful completion of a survey. 
 * 
 * @author Jaap Reitsma
 *
 */
public class SurveyCompletedEvent implements Serializable {
	private static final long serialVersionUID = -7901427588482490251L;
	private Profile profile;
	private SurveyInteraction surveyInteraction;
	
	public SurveyCompletedEvent(Profile user, SurveyInteraction surveyInteraction) {
		this.profile = user;
		this.surveyInteraction = surveyInteraction;
	}

	public Profile getProfile() {
		return profile;
	}

	public SurveyInteraction getSurveyInteraction() {
		return surveyInteraction;
	}

}
