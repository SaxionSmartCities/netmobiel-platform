package eu.netmobiel.commons.jaxrs;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import eu.netmobiel.commons.api.ErrorResponse;
import eu.netmobiel.commons.util.ExceptionUtil;

/**
 * JAX-RS Exception mapper for building a customized error page in case of a ProcessingException.
 *
 * @author Jaap.Reitsma
 *
 */
@Provider
public class JsonProcessingExceptionMapper implements
		ExceptionMapper<JsonProcessingException> {

	@Inject
	private Logger log;

	/**
	 * Capture the cause of the exception and generate a proper HTTP status.
	 */
	@Override
	public Response toResponse(JsonProcessingException e) {
		Response rsp = null;
		Throwable t = e;
		String errorCode = null;
		Response.StatusType status = Response.Status.BAD_REQUEST;
		String[] msgs = ExceptionUtil.unwindExceptionMessage(null, e);
		ErrorResponse err = new ErrorResponse(status, errorCode, String.join(" - ", msgs));
		rsp =  Response.status(status).type(MediaType.APPLICATION_JSON).entity(err).build();
		String[] excs = ExceptionUtil.unwindException(null, t);
		log.error(String.join("\n\tCaused by: ", excs));
		return rsp;
	}
}
