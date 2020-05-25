package eu.netmobiel.commons.exception;

/**
 * Exception for signaling a situation where a record could not be removed.
 *
 * @author Jaap.Reitsma
 *
 */
public class RemoveException extends ApplicationException {

	private static final long serialVersionUID = 660880157443491022L;

	public RemoveException() {
		super();
	}

	public RemoveException(String message, Throwable cause) {
		super(message, cause);
	}

	public RemoveException(String message) {
		super(message);
	}

	public RemoveException(Throwable cause) {
		super(cause);
	}

}
