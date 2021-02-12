package eu.netmobiel.commons.exception;

/**
 * Exception for signaling an issues with payments, like not enough credits for a payment, 
 * or a withdrawal that exceeds the balance..
 *
 * @author Jaap.Reitsma
 *
 */
public class LegalReasonsException extends BusinessException {

	private static final long serialVersionUID = 660880157443491022L;

	public LegalReasonsException() {
		super();
	}

	public LegalReasonsException(String message, Throwable cause) {
		super(message, cause);
	}

	public LegalReasonsException(String message, String vendorCode, Throwable cause) {
		super(message, vendorCode, cause);
	}

	public LegalReasonsException(String message) {
		super(message);
	}

	public LegalReasonsException(String message, String vendorCode) {
		super(message, vendorCode);
	}

	public LegalReasonsException(Throwable cause) {
		super(cause);
	}
}
