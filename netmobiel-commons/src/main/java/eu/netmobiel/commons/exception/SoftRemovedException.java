package eu.netmobiel.commons.exception;

/**
 * Exception for signaling a situation where an existing record could not be modified, because it was soft removed.
 *
 * @author Jaap.Reitsma
 *
 */
public class SoftRemovedException extends UpdateException {

	private static final long serialVersionUID = 660880157443491022L;

	public SoftRemovedException() {
		super();
	}

	public SoftRemovedException(String message, Throwable cause) {
		super(message, cause);
	}

	public SoftRemovedException(String message) {
		super(message);
	}

	public SoftRemovedException(Throwable cause) {
		super(cause);
	}

}
