package eu.netmobiel.payment.client.model;

import java.time.OffsetDateTime;

public class PaymentStatus {
    public PaymentStatus(
            Current current,
            OffsetDateTime created,
            OffsetDateTime modified,
            OffsetDateTime completed
    ) {
        currentStatus = current;
        createdTimestamp = created;
        modifiedTimestamp = modified;
        completedTimestamp = completed;
    }

    public final Current currentStatus;
    public final OffsetDateTime createdTimestamp;
    public final OffsetDateTime modifiedTimestamp;
    public final OffsetDateTime completedTimestamp;

	@Override
	public String toString() {
		return String.format(
				"PaymentStatus [%s, cre %s, mod %s, com %s]",
				currentStatus, createdTimestamp, modifiedTimestamp, completedTimestamp);
	}

	public static enum Current {
        NEW, PROCESSING, ALL_UNSUCCESSFUL, COMPLETED, EXPIRED;
    }

}
