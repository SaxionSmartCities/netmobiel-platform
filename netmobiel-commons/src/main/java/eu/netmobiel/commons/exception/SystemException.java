package eu.netmobiel.commons.exception;

/**
 * Base class for runtime exceptions. A system exception is thrown on an internal error or some other kind of 
 * irrecoverable error.
 *
 * @author Jaap.Reitsma
 *
 */
public class SystemException extends RuntimeException {
	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = -4350415653902764593L;

	/**
	 * Constructor.
	 */
	public SystemException() {
	}

	/**
	 * Constructor
	 * @param message
	 */
	public SystemException(String message) {
		super(message);
	}

	/**
	 * Constructor.
	 * @param cause
	 */
	public SystemException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor.
	 * @param message
	 * @param cause
	 */
	public SystemException(String message, Throwable cause) {
		super(message, cause);
	}

}
