package eu.netmobiel.profile.model;

import java.io.Serializable;
import java.time.Instant;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "survey_response", uniqueConstraints = @UniqueConstraint(name = "uc_survey_response", columnNames = { "survey", "profile" }))
@Vetoed
@Access(AccessType.FIELD)
@SequenceGenerator(name = "survey_response_sg", sequenceName = "survey_response_id_seq", allocationSize = 1, initialValue = 50)
public class SurveyResponse implements Serializable {

	private static final long serialVersionUID = 4640816402401486372L;

	/**
	 * Primary key.
	 */
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "survey_response_sg")
    private Long id;

	/**
	 * Association with the survey.
	 */
	@NotNull
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name= "survey", foreignKey = @ForeignKey(name = "survey_response_survey_fk"))
	private Survey survey;

	/**
	 * Association with the main profile.
	 */
	@NotNull
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name= "profile", foreignKey = @ForeignKey(name = "survey_response_profile_fk"))
	private Profile profile;

	/**
	 * The request time, i.e., the time the user was redirected to the survey.
	 */
	@NotNull
	@Column(name = "request_time")
	private Instant requestTime;

	/**
	 * The submit time. If omitted the survey is being taken, but not yet finished.
	 */
	@Column(name = "submit_time")
	private Instant submitTime;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public Instant getSubmitTime() {
		return submitTime;
	}

	public void setSubmitTime(Instant submitTime) {
		this.submitTime = submitTime;
	}

}
