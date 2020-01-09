package eu.netmobiel.commons.exception;

/**
 * Base class for business exceptions.
 *
 * @author Jaap.Reitsma
 *
 */
public class ApplicationException extends Exception {
	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = 3405463788177828830L;

	/**
	 * Constructor.
	 */
	public ApplicationException() {
	}

	/**
	 * Constructor.
	 * @param message
	 */
	public ApplicationException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 * @param cause
	 */
	public ApplicationException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor.
	 * @param message
	 * @param cause
	 */
	public ApplicationException(String message, Throwable cause) {
		super(message, cause);
	}

}
