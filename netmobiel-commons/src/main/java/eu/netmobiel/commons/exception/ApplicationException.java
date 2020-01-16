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
	 * A vendor specific code identifying the reason of the exception.
	 */
	private String vendorCode;

	public ApplicationException() {
	}

	public ApplicationException(String message) {
		super(message);
	}

	public ApplicationException(String message, String vendorCode) {
		super(message);
		this.vendorCode = vendorCode;
	}

	public ApplicationException(Throwable cause) {
		super(cause);
	}

	public ApplicationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ApplicationException(String message, String vendorCode, Throwable cause) {
		super(message, cause);
		this.vendorCode = vendorCode;
	}

	public String getVendorCode() {
		return vendorCode;
	}

}
