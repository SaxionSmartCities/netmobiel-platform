package eu.netmobiel.banker.exception;

import eu.netmobiel.commons.exception.ApplicationException;

/**
 * Exception for signalling an invalid charge, e.g. when charging more than the previous reservation.
 *
 * @author Jaap.Reitsma
 *
 */
public class InvalidChargeException extends ApplicationException {
	private static final long serialVersionUID = -7820928113267312637L;

	public InvalidChargeException() {
		super();
	}

	public InvalidChargeException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidChargeException(String message, String vendorCode, Throwable cause) {
		super(message, vendorCode, cause);
	}

	public InvalidChargeException(String message) {
		super(message);
	}

	public InvalidChargeException(String message, String vendorCode) {
		super(message, vendorCode);
	}

	public InvalidChargeException(Throwable cause) {
		super(cause);
	}
}
