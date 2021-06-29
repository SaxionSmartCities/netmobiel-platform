package eu.netmobiel.commons.api;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.Response;

import eu.netmobiel.commons.exception.SystemException;

public class ErrorResponse {
	private int status;
	private String reasonPhrase;
	private String message;
	private String errorCode;
	
	public ErrorResponse(Response.StatusType status) {
		this(status, null, null);
	}

	public ErrorResponse(Response.StatusType status, String aMessage) {
		this(status.getStatusCode(), status.getReasonPhrase(), null, aMessage);
	}

	public ErrorResponse(Response.StatusType status, String anErrorCode, String aMessage) {
		this(status.getStatusCode(), status.getReasonPhrase(), null, aMessage);
	}

	public ErrorResponse(int aCode, String aReason, String aMessage) {
		this(aCode, aReason, null, aMessage);
	}

	public ErrorResponse(int aCode, String aReason, String anErrorCode, String aMessage) {
		this.status = aCode;
		this.reasonPhrase = aReason;
		this.errorCode = anErrorCode;
		this.message = aMessage;
	}

	public int getStatus() {
		return status;
	}

	public String getReasonPhrase() {
		return reasonPhrase;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return String.format("%s - %s: %s %s", status, reasonPhrase, errorCode, message);
	}

	public String stringify() {
		String result = null;
		try (Jsonb jsonb = JsonbBuilder.create()) {
			result = jsonb.toJson(this);
		} catch (Exception e) {
			throw new SystemException("Unable to close jsonb writer", e);
		}
		return result;
	}
}
