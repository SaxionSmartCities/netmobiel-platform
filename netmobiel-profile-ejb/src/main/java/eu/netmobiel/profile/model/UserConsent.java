package eu.netmobiel.profile.model;

import java.io.Serializable;
import java.time.Instant;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
@Vetoed
@Access(AccessType.FIELD)
public class UserConsent implements Serializable {
	private static final long serialVersionUID = -1283423695552952095L;
	/**
	 * If set, the time the user accepted the terms.
	 */
	@Column(name = "accepted_terms")
	private Instant acceptedTerms;
	/**
	 * If set, the time the user stated to be older than 16.
	 */
	@Column(name = "older_than_sixteen")
	private Instant olderThanSixteen;
	
	public Instant getAcceptedTerms() {
		return acceptedTerms;
	}
	public void setAcceptedTerms(Instant acceptedTerms) {
		this.acceptedTerms = acceptedTerms;
	}
	public Instant getOlderThanSixteen() {
		return olderThanSixteen;
	}
	public void setOlderThanSixteen(Instant olderThanSixteen) {
		this.olderThanSixteen = olderThanSixteen;
	}
	
	
}
