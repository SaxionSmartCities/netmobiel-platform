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
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import eu.netmobiel.commons.model.ReferableObject;
import eu.netmobiel.profile.util.ProfileUrnHelper;

/**
 * Tracks a interaction between a survey and a profile. 
 * @author Jaap Reitsma
 *
 */
@NamedEntityGraphs({
	@NamedEntityGraph(name = SurveyInteraction.SURVEY_ENTITY_GRAPH, 
			attributeNodes = { 
					@NamedAttributeNode(value = "survey"), 
			} 
	),
	@NamedEntityGraph(name = SurveyInteraction.SURVEY_PROFILE_ENTITY_GRAPH, 
	attributeNodes = { 
			@NamedAttributeNode(value = "survey"), 
			@NamedAttributeNode(value = "profile"), 
	} 
),
})
@Entity
@Table(name = "survey_interaction", uniqueConstraints = {
	    @UniqueConstraint(name = "cs_survey_interaction_unique", columnNames = { "profile", "survey" })
})
@Vetoed
@Access(AccessType.FIELD)
@SequenceGenerator(name = "survey_interaction_sg", sequenceName = "survey_interaction_seq", allocationSize = 1, initialValue = 50)
public class SurveyInteraction extends ReferableObject implements Serializable {

	private static final long serialVersionUID = 4640816402401486372L;
	public static final String SURVEY_ENTITY_GRAPH = "survey-entity-graph";
	public static final String SURVEY_PROFILE_ENTITY_GRAPH = "survey-profile-entity-graph";
	// Use a shorter word to stay with 32 characters
	public static final String URN_PREFIX = ProfileUrnHelper.createUrnPrefix("surveyint");

	/**
	 * Primary key.
	 */
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "survey_interaction_sg")
    private Long id;

	/**
	 * The trigger time, causing the interaction to occur (after an optional delay). This value is needed to calculate 
	 * the time left. The trigger time is stored, rather then the expiration time. When the survey interval or end date are extended, 
	 * current survey interactions are extended as well. The trigger time is never changing.
	 */
	@NotNull
	@Column(name = "trigger_time", updatable = false)
	private Instant triggerTime;

	/**
	 * The first time the user was invited to take part in the survey.
	 */
	@NotNull
	@Column(name = "invitation_time", updatable = false)
	private Instant invitationTime;

	/**
	 * The number of times the user was invited to take part in the survey (number of views of the home page)
	 */
	@NotNull
	@Column(name = "invitation_count")
	private int invitationCount;

	/**
	 * The first time the user was redirected to the survey.
	 */
	@Column(name = "redirect_time")
	private Instant redirectTime;

	/**
	 * The number of times the user was redirected to the survey, intended to track whether there are difficulties completing the survey. 
	 */
	@NotNull
	@Column(name = "redirect_count")
	private int redirectCount;

	/**
	 * The submit time. If omitted the survey is being taken, but not yet finished.
	 */
	@Column(name = "submit_time")
	private Instant submitTime;

	/**
	 * Association with the survey.
	 */
	@NotNull
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name= "survey", foreignKey = @ForeignKey(name = "survey_interaction_survey_fk"))
	private Survey survey;

	/**
	 * Association with the main profile.
	 */
	@NotNull
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name= "profile", foreignKey = @ForeignKey(name = "survey_interaction_profile_fk"))
	private Profile profile;

	public SurveyInteraction() {
		this(null, null, null, Instant.now());
	}
	
	public SurveyInteraction(Survey survey, Profile profile, Instant triggerTime) {
		this(survey, profile, triggerTime, Instant.now());
	}

	public SurveyInteraction(Survey survey, Profile profile, Instant triggerTime, Instant invitationTime) {
		this.survey = survey;
		this.profile = profile;
		this.triggerTime = triggerTime;
		this.invitationTime = invitationTime;
		this.invitationCount = 0;
		this.redirectCount = 0;
	}

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

	public Instant getTriggerTime() {
		return triggerTime;
	}

	public void setTriggerTime(Instant triggerTime) {
		this.triggerTime = triggerTime;
	}

	public Instant getInvitationTime() {
		return invitationTime;
	}

	public void setInvitationTime(Instant invitationTime) {
		this.invitationTime = invitationTime;
	}

	public int getInvitationCount() {
		return invitationCount;
	}

	public void setInvitationCount(int invitationCount) {
		this.invitationCount = invitationCount;
	}

	public void incrementInvitationCount() {
		this.invitationCount++;
	}
	
	public Instant getRedirectTime() {
		return redirectTime;
	}

	public void setRedirectTime(Instant redirectTime) {
		this.redirectTime = redirectTime;
	}

	public int getRedirectCount() {
		return redirectCount;
	}

	public void setRedirectCount(int redirectCount) {
		this.redirectCount = redirectCount;
	}

	public void incrementRedirectCount() {
		this.redirectCount++;
	}
	
	public Instant getSubmitTime() {
		return submitTime;
	}

	public void setSubmitTime(Instant submitTime) {
		this.submitTime = submitTime;
	}

	public Instant getExpirationTime() {
		Survey s = getSurvey();
		Instant expTime = s.getEndTime();
		if (s.getTakeIntervalHours() != null) {
			expTime = triggerTime.plusSeconds((s.getTakeDelayHours() + s.getTakeIntervalHours()) * 3600);
			if (s.getEndTime() != null && expTime.isAfter(s.getEndTime())) {
				expTime = s.getEndTime(); 
			}
		}
        return expTime;
	}

	public boolean isExpired(Instant reference) {
		Instant expTime = getExpirationTime();
		return expTime != null && expTime.isBefore(reference);
	}

	public boolean isExpired() {
		return isExpired(Instant.now());
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

}