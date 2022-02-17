package eu.netmobiel.profile.event;

import java.io.Serializable;

import eu.netmobiel.profile.model.SurveyInteraction;

/**
 * Base event event for  a survey. The provided survey interaction must be complete, 
 * including the profile and the survey objects.  
 * 
 * @author Jaap Reitsma
 *
 */
public class SurveyEvent implements Serializable {
	private static final long serialVersionUID = -7901427588482490251L;
	private SurveyInteraction surveyInteraction;
	
	protected SurveyEvent(SurveyInteraction surveyInteraction) {
		this.surveyInteraction = surveyInteraction;
	}

	public SurveyInteraction getSurveyInteraction() {
		return surveyInteraction;
	}

}
