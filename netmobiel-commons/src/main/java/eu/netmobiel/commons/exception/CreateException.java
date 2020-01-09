package eu.netmobiel.commons.exception;

/**
 * Exception for signaling a situation where a new record could not be created in the store.
 *
 * @author Jaap.Reitsma
 *
 */
public class CreateException extends ApplicationException {

	private static final long serialVersionUID = 660880157443491022L;

	public CreateException() {
		super();
	}

	public CreateException(String message, Throwable cause) {
		super(message, cause);
	}

	public CreateException(String message) {
		super(message);
	}

	public CreateException(Throwable cause) {
		super(cause);
	}

}
