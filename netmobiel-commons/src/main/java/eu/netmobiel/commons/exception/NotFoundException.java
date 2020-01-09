package eu.netmobiel.commons.exception;

/**
 * Exception for signaling an HTTP 404 Not Found.
 *
 * @author Jaap.Reitsma
 *
 */
public class NotFoundException extends ApplicationException {
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

	public NotFoundException(Throwable cause) {
		super(cause);
	}

}
