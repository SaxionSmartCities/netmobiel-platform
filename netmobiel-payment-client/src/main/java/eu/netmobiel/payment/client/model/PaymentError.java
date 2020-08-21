package eu.netmobiel.payment.client.model;

public class PaymentError {
	public ErrorDetail error;
	
	public static class ErrorDetail {
		public int status;
		public String type;
		public String value;
	}
	
	public String toString() {
		return String.format("%d (%s) %s", error.status, error.type, error.value);
	}
}
