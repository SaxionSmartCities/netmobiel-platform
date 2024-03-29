package eu.netmobiel.profile.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import eu.netmobiel.commons.model.ReferableObject;
import eu.netmobiel.profile.util.ProfileUrnHelper;
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
	{   @UniqueConstraint(columnNames = { "survey_id" }, name = "uc_survey_id")
	})
@Vetoed
@Access(AccessType.FIELD)
@SequenceGenerator(name = "survey_sg", sequenceName = "survey_id_seq", allocationSize = 1, initialValue = 50)
public class Survey extends ReferableObject implements Serializable {
	private static final long serialVersionUID = -9153483037541781268L;
	public static final String URN_PREFIX = ProfileUrnHelper.createUrnPrefix("survey");

	/**
	 * Primary key.
	 */
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "survey_sg")
    private Long id;

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
	@Column(name = "survey_id")
	private String surveyId;

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
	@NotNull
	@Column(name = "take_delay_hours")
	private Integer takeDelayHours;

	/**
	 * The optional length if the interval during which the survey can be taken.
	 * The absolute end time is the trigger time + takeDelayHours + takeintervalHours. 
	 */
	@Column(name = "take_interval_hours")
	private Integer takeIntervalHours;

	/**
	 * An survey has optionally an incentive attached. 
	 */
	@Size(max = 16)
	@Column(name = "incentive_code")
    private String incentiveCode;

	@Override
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public String getUrnPrefix() {
		return URN_PREFIX;
	}
	
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

	public String getIncentiveCode() {
		return incentiveCode;
	}

	public void setIncentiveCode(String incentiveCode) {
		this.incentiveCode = incentiveCode;
	}

	@Override
	public int hashCode() {
		return Objects.hash(surveyId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Survey)) {
			return false;
		}
		Survey other = (Survey) obj;
		return Objects.equals(surveyId, other.surveyId);
	}
	
	
}
