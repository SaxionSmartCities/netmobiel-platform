package eu.netmobiel.banker.exception;

import eu.netmobiel.commons.exception.BusinessException;

/**
 * Exception for signalling an insufficient balance.
 *
 * @author Jaap.Reitsma
 *
 */
public class BalanceInsufficientException extends BusinessException {
	private static final long serialVersionUID = -7820928113267312637L;

	public BalanceInsufficientException() {
		super();
	}

	public BalanceInsufficientException(String message, Throwable cause) {
		super(message, cause);
	}

	public BalanceInsufficientException(String message, String vendorCode, Throwable cause) {
		super(message, vendorCode, cause);
	}

	public BalanceInsufficientException(String message) {
		super(message);
	}

	public BalanceInsufficientException(String message, String vendorCode) {
		super(message, vendorCode);
	}

	public BalanceInsufficientException(Throwable cause) {
		super(cause);
	}
}
