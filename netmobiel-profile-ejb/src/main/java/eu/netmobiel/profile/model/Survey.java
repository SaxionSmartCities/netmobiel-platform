package eu.netmobiel.profile.model;

import java.io.Serializable;
import java.time.Instant;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
/**
 * Definition of a survey. A survey has (optionally) a limited period of time is which it can be taken. 
 * A survey is ready to be taken after some trigger, currently supported is some trigger date. A survey has a condition 
 * that defines whether it is eligable to be taken. Currently supported are a duration after the trigger date (a delay) 
 * followed by period during which the survay can be taken.  
 * 
 * @author Jaap Reitsma
 *
 */
@Entity
@Table(name = "survey", uniqueConstraints = 
	{   @UniqueConstraint(columnNames = { "survey_id" }, name = "uc_survey_id"),
		@UniqueConstraint(columnNames = { "provider_survey_ref" }, name = "uc_provider_survey_ref")
	})
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
	 * The external reference to the survey, this is the reference used by the survey provider.
	 */
	@Size(max = 32)
	@NotNull
	@Column(name = "provider_survey_ref")
	private String providerSurveyRef;

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
	 * The optional delay of the survey after the trigger before it is made available to a (specific) user.
	 * The absolute start time is the trigger time + takeDelayHours. 
	 * 
	 */
	@Column(name = "take_delay_hours")
	private Integer takeDelayHours;

	/**
	 * The optional length if the interval during which the survey can be taken.
	 * The absolute end time is the trigger time + takeDelayHours + takeintervalHours. 
	 */
	@Column(name = "take_interval_hours")
	private Integer takeIntervalHours;

	/**
	 * The url of the provider to take the survey. 
	 * Only present on request.
	 */
	@Transient
	private String providerUrl;

	/**
	 * The amount of credits to receive on completing the survey.
	 */
	@Column(name = "reward_credits")
	private Integer rewardCredits;

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

	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

	public String getProviderSurveyRef() {
		return providerSurveyRef;
	}

	public void setProviderSurveyRef(String providerSurveyRef) {
		this.providerSurveyRef = providerSurveyRef;
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

	public Integer getTakeDelayHours() {
		return takeDelayHours;
	}

	public void setTakeDelayHours(Integer takeDelayHours) {
		this.takeDelayHours = takeDelayHours;
	}

	public Integer getTakeIntervalHours() {
		return takeIntervalHours;
	}

	public void setTakeIntervalHours(Integer takeIntervalHours) {
		this.takeIntervalHours = takeIntervalHours;
	}

	public String getProviderUrl() {
		return providerUrl;
	}

	public void setProviderUrl(String providerUrl) {
		this.providerUrl = providerUrl;
	}
	
	
}
