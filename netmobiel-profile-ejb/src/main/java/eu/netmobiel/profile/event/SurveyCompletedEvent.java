package eu.netmobiel.profile.event;

import java.io.Serializable;

import eu.netmobiel.profile.model.SurveyInteraction;

/**
 * Event issued on successful completion of a survey. The provided survey interaction must be complete, 
 * including the profile and the survey objects.  
 * 
 * @author Jaap Reitsma
 *
 */
public class SurveyCompletedEvent implements Serializable {
	private static final long serialVersionUID = -7901427588482490251L;
	private SurveyInteraction surveyInteraction;
	
	public SurveyCompletedEvent(SurveyInteraction surveyInteraction) {
		this.surveyInteraction = surveyInteraction;
	}

	public SurveyInteraction getSurveyInteraction() {
		return surveyInteraction;
	}

}
