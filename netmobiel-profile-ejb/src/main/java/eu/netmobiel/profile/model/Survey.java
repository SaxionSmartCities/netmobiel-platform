package eu.netmobiel.profile.model;

import java.io.Serializable;
import java.time.Instant;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(name = "survey", uniqueConstraints = @UniqueConstraint(columnNames = { "survey_id" }, name = "uc_survey_id"))
@Vetoed
@Access(AccessType.FIELD)

public class Survey implements Serializable {
	private static final long serialVersionUID = -9153483037541781268L;

	/**
	 * Primary key.
	 * Our reference to the survey.
	 * Not generated, because the survey id is known in advance.
	 */
	@Id
	@NotNull
	@Size(max = 8)
	@Column(name = "survey_id")
	private String surveyId;


	/**
	 * The display name of the survey, intended for the end user.
	 */
	@NotNull
	@Size(max = 64)
	@Column(name = "display_name")
	private String displayName;

	/**
	 * The optional remarks about the survey, intended for the developer/maintainer.
	 */
	@Size(max = 256)
	@Column(name = "remarks")
	private String remarks;

	/**
	 * The provider of the survey.
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "survey_provider", length = 16, nullable = false)
	private SurveyProvider surveyProvider;

	/**
	 * The trigger of the survey to present to the user. 
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "survey_trigger", length = 16, nullable = false)
	private SurveyTrigger surveyTrigger;
	
	/**
	 * The external reference to the survey, this is the reference used by the survey provider.
	 */
	@Size(max = 32)
	@NotNull
	@Column(name = "provider_survey_ref")
	private String providerSurveyRef;

	/**
	 * Our survey group reference. Used to group consecutive surveys.
	 */
	@Size(max = 32)
	@Column(name = "group_ref")
	private String groupRef;

	/**
	 * Our survey group reference. Used to group consecutive surveys.
	 */
	@Column(name = "sequenceNr")
	private Integer sequenceNr;

	/**
	 * The eligable start time of the survey (optional).
	 */
	@Column(name = "start_time")
	private Instant startTime;

	/**
	 * The end time of the survey (optional). After this time the survey will not be presented anymore.
	 */
	@Column(name = "end_time")
	private Instant endTime;

	/**
	 * The optional (minimum) delay of the survey after the trigger.
	 */
	@Column(name = "delay_hours")
	private Integer delayHours;

	public String getSurveyId() {
		return surveyId;
	}

	public void setSurveyId(String surveyId) {
		this.surveyId = surveyId;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public SurveyProvider getSurveyProvider() {
		return surveyProvider;
	}

	public void setSurveyProvider(SurveyProvider surveyProvider) {
		this.surveyProvider = surveyProvider;
	}

	public SurveyTrigger getSurveyTrigger() {
		return surveyTrigger;
	}

	public void setSurveyTrigger(SurveyTrigger surveyTrigger) {
		this.surveyTrigger = surveyTrigger;
	}

	public String getProviderSurveyRef() {
		return providerSurveyRef;
	}

	public void setProviderSurveyRef(String providerSurveyRef) {
		this.providerSurveyRef = providerSurveyRef;
	}

	public String getGroupRef() {
		return groupRef;
	}

	public void setGroupRef(String groupRef) {
		this.groupRef = groupRef;
	}

	public Integer getSequenceNr() {
		return sequenceNr;
	}

	public void setSequenceNr(Integer sequenceNr) {
		this.sequenceNr = sequenceNr;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}

	public Instant getEndTime() {
		return endTime;
	}

	public void setEndTime(Instant endTime) {
		this.endTime = endTime;
	}

	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

	public Integer getDelayHours() {
		return delayHours;
	}

	public void setDelayHours(Integer delayHours) {
		this.delayHours = delayHours;
	}

}
