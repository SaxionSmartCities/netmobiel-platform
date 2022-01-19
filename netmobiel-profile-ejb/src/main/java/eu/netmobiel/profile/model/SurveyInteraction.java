package eu.netmobiel.profile.model;

import java.io.Serializable;
import java.time.Instant;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * Tracks a interaction between a survey and a profile. 
 * @author Jaap Reitsma
 *
 */
@NamedEntityGraphs({
	@NamedEntityGraph(name = SurveyInteraction.SURVEY_ENTITY_GRAPH, 
			attributeNodes = { @NamedAttributeNode(value = "id") } 
	),
})
@Entity
@Table(name = "survey_interaction")
@Vetoed
@Access(AccessType.FIELD)
public class SurveyInteraction implements Serializable {

	private static final long serialVersionUID = 4640816402401486372L;
	public static final String SURVEY_ENTITY_GRAPH = "survey-entity-graph";

	/**
	 * Primary key.
	 */
	@EmbeddedId
    private SurveyInteractionId id;

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

	public SurveyInteraction() {
		this(null, null, null, Instant.now());
	}
	
	public SurveyInteraction(Survey survey, Profile profile, Instant triggerTime) {
		this(survey, profile, triggerTime, Instant.now());
	}

	public SurveyInteraction(Survey survey, Profile profile, Instant triggerTime, Instant invitationTime) {
		this.id = new SurveyInteractionId(survey, profile);
		this.triggerTime = triggerTime;
		this.invitationTime = invitationTime;
		this.invitationCount = 0;
		this.redirectCount = 0;
	}

	public SurveyInteractionId getId() {
		return id;
	}

	public void setId(SurveyInteractionId id) {
		this.id = id;
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
		Survey s = id.getSurvey();
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
}