package eu.netmobiel.banker.exception;

import eu.netmobiel.commons.exception.PaymentException;

/**
 * Exception for signalling an invalid charge, e.g. when charging more than the previous reservation.
 *
 * @author Jaap.Reitsma
 *
 */
public class OverdrawnException extends PaymentException {
	private static final long serialVersionUID = -7820928113267312637L;

	public OverdrawnException() {
		super();
	}

	public OverdrawnException(String message, Throwable cause) {
		super(message, cause);
	}

	public OverdrawnException(String message, String vendorCode, Throwable cause) {
		super(message, vendorCode, cause);
	}

	public OverdrawnException(String message) {
		super(message);
	}

	public OverdrawnException(String message, String vendorCode) {
		super(message, vendorCode);
	}

	public OverdrawnException(Throwable cause) {
		super(cause);
	}
}
