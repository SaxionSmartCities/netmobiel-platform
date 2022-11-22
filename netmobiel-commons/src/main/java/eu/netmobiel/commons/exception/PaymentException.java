package eu.netmobiel.commons.exception;

/**
 * Exception for signaling an issues with payments, like not enough credits for a payment, 
 * or a withdrawal that exceeds the balance..
 *
 * @author Jaap.Reitsma
 *
 */
public class PaymentException extends BusinessException {

	private static final long serialVersionUID = 660880157443491022L;

	public PaymentException() {
		super();
	}

	public PaymentException(String message, Throwable cause) {
		super(message, cause);
	}

	public PaymentException(String message, String vendorCode, Throwable cause) {
		super(message, vendorCode, cause);
	}

	public PaymentException(String message) {
		super(message);
	}

	public PaymentException(String message, String vendorCode) {
		super(message, vendorCode);
	}

	public PaymentException(Throwable cause) {
		super(cause);
	}
}
