package eu.netmobiel.commons.api;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.Response;

public class ErrorResponse {
	private int status;
	private String reasonPhrase;
	private String message;
	
	public ErrorResponse(Response.StatusType status) {
		this(status, null);
	}

	public ErrorResponse(Response.StatusType status, String aMessage) {
		this(status.getStatusCode(), status.getReasonPhrase(), aMessage);
	}

	public ErrorResponse(int aCode, String aReason, String aMessage) {
		this.status = aCode;
		this.reasonPhrase = aReason;
		this.message = aMessage;
	}

	public int getStatus() {
		return status;
	}

	public String getReasonPhrase() {
		return reasonPhrase;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return String.format("%s - %s: %s", status, reasonPhrase, message);
	}

	public String stringify() {
		Jsonb jsonb = JsonbBuilder.create();
		return jsonb.toJson(this);
	}
}
