package eu.netmobiel.commons.jaxrs;

import javax.inject.Inject;
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
public class SecurityExceptionMapper implements
		ExceptionMapper<SecurityException> {

	@Inject
	private Logger log;

	/**
	 * Capture the cause of the exception and generate a proper HTTP status.
	 */
	@Override
	public Response toResponse(SecurityException e) {
		Response rsp = null;
		
		String[] msgs = ExceptionUtil.unwindExceptionMessage("Access denied", e);
		Response.StatusType statusType = Response.Status.FORBIDDEN;
		ErrorResponse err = new ErrorResponse(statusType, String.join(" - ", msgs));
		rsp =  Response.status(statusType).type(MediaType.APPLICATION_JSON).entity(err).build();
		// Log message only
		log.error(String.join(" - ", msgs));
		return rsp;
	}
}
