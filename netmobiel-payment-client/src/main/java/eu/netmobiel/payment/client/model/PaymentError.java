package eu.netmobiel.payment.client.model;

public class PaymentError {
	public ErrorDetail error;
	
	public static class ErrorDetail {
		public int status;
		public String type;
		public String value;
	}
	
	public PaymentError() {
		error = new PaymentError.ErrorDetail();
	}
	
	public PaymentError(int aStatus, String aType, String aValue) {
		this();
		error.status = aStatus;
		error.type = aType;
		error.value = aValue;

	}
	@Override
	public String toString() {
		return String.format("%d (%s) %s", error.status, error.type, error.value);
	}
}
