package eu.netmobiel.commons.exception;

/**
 * Exception for signaling that an object could not be found.
 *
 * @author Jaap.Reitsma
 *
 */
public class NotFoundException extends BusinessException {
	private static final long serialVersionUID = -8467666333189918504L;

	public NotFoundException() {
		super();
	}

	public NotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public NotFoundException(String message) {
		super(message);
	}

	public NotFoundException(String message, String vendorCode) {
		super(message, vendorCode);
	}

	public NotFoundException(Throwable cause) {
		super(cause);
	}

	public NotFoundException(String message, String vendorCode, Throwable cause) {
		super(message, vendorCode, cause);
	}

}
