package eu.netmobiel.commons.exception;

/**
 * Exception for signaling an Bad Request, similar in use as in the HTTP protocol.
 * A bad request can be corrected by the caller, such that a next call might succeed.
 *
 * @author Jaap.Reitsma
 *
 */
public class BadRequestException extends BusinessException {

	private static final long serialVersionUID = 660880157443491022L;

	public BadRequestException() {
		super();
	}

	public BadRequestException(String message, Throwable cause) {
		super(message, cause);
	}

	public BadRequestException(String message, String vendorCode, Throwable cause) {
		super(message, vendorCode, cause);
	}

	public BadRequestException(String message) {
		super(message);
	}

	public BadRequestException(String message, String vendorCode) {
		super(message, vendorCode);
	}

	public BadRequestException(Throwable cause) {
		super(cause);
	}
}
