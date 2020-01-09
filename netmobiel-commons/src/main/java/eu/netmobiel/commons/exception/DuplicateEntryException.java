package eu.netmobiel.commons.exception;

/**
 * This exception is thrown when an entity already exists in the database or data store, based on it's unique key.
 * @author Jaap Reitsma
 *
 */
public class DuplicateEntryException extends CreateException {
	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = -919464677173773619L;

	public DuplicateEntryException() {
	}

	public DuplicateEntryException(String message, Throwable cause) {
		super(message, cause);
	}

	public DuplicateEntryException(String message) {
		super(message);
	}

	public DuplicateEntryException(Throwable cause) {
		super(cause);
	}

}
