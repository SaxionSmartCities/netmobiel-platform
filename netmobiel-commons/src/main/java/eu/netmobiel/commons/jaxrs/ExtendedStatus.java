package eu.netmobiel.commons.jaxrs;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

public enum ExtendedStatus implements Response.StatusType {

	UNPROCESSIBLE_ENTITY(422, "Unprocessable Entity"),
	UNAVAILABLE_FOR_LEGAL_REASONS(451, "Unavailable for Legal Reasons");
    private final int code;
    private final String reason;
    private final Family family;

	ExtendedStatus(final int statusCode, final String reasonPhrase) {
        this.code = statusCode;
        this.reason = reasonPhrase;
        this.family = Family.familyOf(statusCode);
    }

    @Override
	public Family getFamily() {
		return family;
	}

	@Override
	public int getStatusCode() {
		return code;
	}

	@Override
	public String getReasonPhrase() {
		return reason;
	}
	
    public static ExtendedStatus fromStatusCode(final int statusCode) {
        for (ExtendedStatus s : ExtendedStatus.values()) {
            if (s.code == statusCode) {
                return s;
            }
        }
        return null;
    }

}
