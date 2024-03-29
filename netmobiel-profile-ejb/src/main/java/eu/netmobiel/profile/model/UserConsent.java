package eu.netmobiel.profile.model;

import java.io.Serializable;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

@Embeddable
@Vetoed
@Access(AccessType.FIELD)
public class UserConsent implements Serializable {
	private static final long serialVersionUID = -1283423695552952095L;
	/**
	 * If set, the time the user accepted the terms.
	 */
	@NotNull
	@Column(name = "consent_accepted_terms")
	private boolean acceptedTerms = false;
	/**
	 * If set, the time the user stated to be older than 16.
	 */
	@NotNull
	@Column(name = "consent_older_than_sixteen")
	private boolean olderThanSixteen = false;
	
	/**
	 * If set the user agrees to follow the safety guidelines.
	 */
	@NotNull
	@Column(name = "consent_safety_guidelines")
	private boolean safetyGuidelines = false;

	public static UserConsent createDefault() {
		return new UserConsent();
	}

	public boolean isAcceptedTerms() {
		return acceptedTerms;
	}
	
	public void setAcceptedTerms(boolean acceptedTerms) {
		this.acceptedTerms = acceptedTerms;
	}
	
	public boolean isOlderThanSixteen() {
		return olderThanSixteen;
	}
	
	public void setOlderThanSixteen(boolean olderThanSixteen) {
		this.olderThanSixteen = olderThanSixteen;
	}

	public boolean isSafetyGuidelines() {
		return safetyGuidelines;
	}

	public void setSafetyGuidelines(boolean safetyGuidelines) {
		this.safetyGuidelines = safetyGuidelines;
	}
	
	public boolean isAllAccepted() {
		return isAcceptedTerms() && isOlderThanSixteen() && isSafetyGuidelines();
	}
}
