package eu.netmobiel.commons.jaxrs;

import javax.inject.Inject;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;

import eu.netmobiel.commons.api.ErrorResponse;
import eu.netmobiel.commons.util.ExceptionUtil;

/**
 * JAX-RS Exception mapper for building a customized error page in case of a WebApplicationException, i.e. any JAX-RS exception
 * thrown from the API layer..
 *
 * @author Jaap.Reitsma
 *
 */
@Provider
public class WebApplicationExceptionMapper implements
		ExceptionMapper<WebApplicationException> {

	@Inject
	private Logger log;

	/**
	 * Capture the cause of the exception and generate a proper HTTP status.
	 */
	@Override
	public Response toResponse(WebApplicationException e) {
		Response rsp = null;
		
		String[] msgs = ExceptionUtil.unwindExceptionMessage(null, e);
		ErrorResponse err = new ErrorResponse(e.getResponse().getStatusInfo(), String.join(" - ", msgs));
		rsp =  Response.status(e.getResponse().getStatusInfo()).type(MediaType.APPLICATION_JSON).entity(err).build();
		if (e instanceof ServerErrorException) {
			// Log stackdump
			log.error("Server error", e);
		} else {
			// Log message only
			String[] excs = ExceptionUtil.unwindException(null, e);
			log.error(String.join("\n\tCaused by: ", excs));
		}
		return rsp;
	}
}
