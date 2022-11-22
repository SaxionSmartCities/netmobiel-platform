package eu.netmobiel.commons.exception;

/**
 * Exception for signaling a situation where an existing record could not be modified.
 *
 * @author Jaap.Reitsma
 *
 */
public class UpdateException extends BusinessException {

	private static final long serialVersionUID = 660880157443491022L;

	public UpdateException() {
		super();
	}

	public UpdateException(String message, Throwable cause) {
		super(message, cause);
	}

	public UpdateException(String message) {
		super(message);
	}

	public UpdateException(Throwable cause) {
		super(cause);
	}

}
