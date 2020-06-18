package eu.netmobiel.payment.client.model;

public class PaymentStatus {
    public PaymentStatus(
            String status,
            String created,
            String modified,
            String completed
    ) {
        currentStatus = status;
        creationTimestamp = created;
        modifiedTimestamp = modified;
        completedTimestamp = completed;
    }

    public final String currentStatus;
    public final String creationTimestamp;
    public final String modifiedTimestamp;
    public final String completedTimestamp;
    
	@Override
	public String toString() {
		return String.format(
				"PaymentStatus [%s, cre %s, mod %s, com %s]",
				currentStatus, creationTimestamp, modifiedTimestamp, completedTimestamp);
	}
}
