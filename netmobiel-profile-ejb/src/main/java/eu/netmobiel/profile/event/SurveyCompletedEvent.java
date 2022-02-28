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
public class SurveyCompletedEvent extends SurveyEvent implements Serializable {
	private static final long serialVersionUID = -7420478775206484689L;

	public SurveyCompletedEvent(SurveyInteraction surveyInteraction) {
		super(surveyInteraction);
	}
	@Override
	public String toString() {
		return String.format("SurveyCompletedEvent [%s]", getSurveyInteraction().getUrn());
	}
}
