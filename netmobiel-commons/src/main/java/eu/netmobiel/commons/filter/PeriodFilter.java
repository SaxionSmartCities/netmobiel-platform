package eu.netmobiel.commons.filter;

import java.time.Instant;
import java.time.OffsetDateTime;

import eu.netmobiel.commons.exception.BadRequestException;

public class PeriodFilter {
	/**
	 * For development purposes. Used to validate the since and until parameters.
	 */
	private Instant now;

	/** ==============================
	 * Selection based on time
	 */
	private Instant since;
	private Instant until;

	public PeriodFilter() {
	}

	public Instant getNow() {
		return now;
	}

	public void setNow(Instant now) {
		this.now = now;
	}

	public Instant getSince() {
		return since;
	}

	public void setSince(Instant since) {
		this.since = since;
	}

	public final void setSince(OffsetDateTime since) {
		if (since != null) {
			this.since = since.toInstant();
		}
	}

	public Instant getUntil() {
		return until;
	}

	public void setUntil(Instant until) {
		this.until = until;
	}

	public final void setUntil(OffsetDateTime until) {
		if (until != null) {
			this.until = until.toInstant();
		}
	}

	public void validate() throws BadRequestException {
    	if (now == null) {
    		now = Instant.now();
    	}
    	if (until != null && since != null && !until.isAfter(since)) {
    		throw new BadRequestException("Constraint violation: 'until' must be later than 'since'.");
    	}
	}
}
