package eu.netmobiel.commons.exception;

import javax.ejb.ApplicationException;

/**
 * Base class for business exceptions.
 *
 * @author Jaap.Reitsma
 *
 */
@ApplicationException(rollback = true)
public class BusinessException extends Exception {
	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = 3405463788177828830L;
	/**
	 * A vendor specific code identifying the reason of the exception.
	 */
	private String vendorCode;

	public BusinessException() {
	}

	public BusinessException(String message) {
		super(message);
	}

	public BusinessException(String message, String vendorCode) {
		super(message);
		this.vendorCode = vendorCode;
	}

	public BusinessException(Throwable cause) {
		super(cause);
	}

	public BusinessException(String message, Throwable cause) {
		super(message, cause);
	}

	public BusinessException(String message, String vendorCode, Throwable cause) {
		super(message, cause);
		this.vendorCode = vendorCode;
	}

	public String getVendorCode() {
		return vendorCode;
	}

}
