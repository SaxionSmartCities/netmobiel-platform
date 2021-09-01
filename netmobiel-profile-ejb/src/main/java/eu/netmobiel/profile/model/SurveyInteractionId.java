package eu.netmobiel.profile.model;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

/**
 * Composite primary key for the relation between a survey and a profile. 
 * @author Jaap Reitsma
 *
 */
@Embeddable
@Access(AccessType.FIELD)
public class SurveyInteractionId implements Serializable {

	private static final long serialVersionUID = 5311740772839232575L;

	/**
	 * Association with the survey.
	 */
	@NotNull
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name= "survey", foreignKey = @ForeignKey(name = "survey_interaction_survey_fk"), updatable = false)
	private Survey survey;

	/**
	 * Association with the main profile.
	 */
	@NotNull
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name= "profile", foreignKey = @ForeignKey(name = "survey_interaction_profile_fk"), updatable = false)
	private Profile profile;

	public SurveyInteractionId() {
		
	}
	
	public SurveyInteractionId(Survey survey, Profile profile) {
		this.survey = survey;
		this.profile = profile;
	}

	public Survey getSurvey() {
		return survey;
	}

	public void setSurvey(Survey survey) {
		this.survey = survey;
	}

	public Profile getProfile() {
		return profile;
	}

	public void setProfile(Profile profile) {
		this.profile = profile;
	}

	@Override
	public int hashCode() {
		return Objects.hash(profile, survey);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SurveyInteractionId)) {
			return false;
		}
		SurveyInteractionId other = (SurveyInteractionId) obj;
		return Objects.equals(profile, other.profile) && Objects.equals(survey, other.survey);
	}


}