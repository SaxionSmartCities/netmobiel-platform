package eu.netmobiel.commons.filter;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import eu.netmobiel.commons.exception.BadRequestException;

public class PeriodFilter extends BaseFilter {
	/**
	 * For development purposes. Used to validate the since and until parameters.
	 */
	private Instant now;

	/** ==============================
	 * Selection based on time
	 */
	private Instant since;
	private Instant until;

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

	@Override
	public void validate() throws BadRequestException {
		super.validate();
    	if (now == null) {
    		now = Instant.now();
    	}
    	if (until != null && since != null && !until.isAfter(since)) {
    		throw new BadRequestException("Constraint violation: 'until' must be later than 'since'.");
    	}
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (since != null) {
			builder.append("since=");
			builder.append(DateTimeFormatter.ISO_INSTANT.format(since));
			builder.append(" ");
		}
		if (until != null) {
			builder.append("until=");
			builder.append(DateTimeFormatter.ISO_INSTANT.format(until));
			builder.append(" ");
		}
		builder.append(super.toString());
		return builder.toString();
	}
}
