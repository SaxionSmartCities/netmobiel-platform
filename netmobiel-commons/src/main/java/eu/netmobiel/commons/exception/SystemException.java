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
	 * A vendor specific code identifying the reason of the exception.
	 */
	private String vendorCode;

	public SystemException() {
	}

	public SystemException(String message) {
		super(message);
	}

	public SystemException(String message, String vendorCode) {
		super(message);
		this.vendorCode = vendorCode;
	}

	public SystemException(Throwable cause) {
		super(cause);
	}

	public SystemException(String message, Throwable cause) {
		super(message, cause);
	}

	public SystemException(String message, String vendorCode, Throwable cause) {
		super(message, cause);
		this.vendorCode = vendorCode;
	}

	public String getVendorCode() {
		return vendorCode;
	}

}
