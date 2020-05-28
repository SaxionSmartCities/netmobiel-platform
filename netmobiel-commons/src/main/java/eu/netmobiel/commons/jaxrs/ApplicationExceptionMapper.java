package eu.netmobiel.commons.jaxrs;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;

import eu.netmobiel.commons.api.ErrorResponse;
import eu.netmobiel.commons.exception.ApplicationException;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.UpdateException;
import eu.netmobiel.commons.util.ExceptionUtil;

/**
 * JAX-RS Exception mapper for building a customized error page in case of a WebApplicationException, i.e. any JAX-RS exception
 * thrown from the API layer..
 *
 * @author Jaap.Reitsma
 *
 */
@Provider
public class ApplicationExceptionMapper implements
		ExceptionMapper<ApplicationException> {

	@Inject
	private Logger log;

	/**
	 * Capture the cause of the exception and generate a proper HTTP status.
	 */
	@Override
	public Response toResponse(ApplicationException e) {
		Response rsp = null;
		String[] msgs = ExceptionUtil.unwindExceptionMessage(null, e);
		Response.StatusType status = Response.Status.INTERNAL_SERVER_ERROR;
		if (e instanceof BadRequestException) {
			status = Response.Status.BAD_REQUEST;
		} else if (e instanceof NotFoundException) {
			status = Response.Status.NOT_FOUND;
		} else if (e instanceof CreateException) {
			status = ExtendedStatus.UNPROCESSIBLE_ENTITY;
		} else if (e instanceof UpdateException) {
			status = ExtendedStatus.UNPROCESSIBLE_ENTITY;
		}
		ErrorResponse err = new ErrorResponse(status, e.getVendorCode(), String.join(" - ", msgs));
		rsp =  Response.status(status).type(MediaType.APPLICATION_JSON).entity(err).build();
		// Log message only
		String[] excs = ExceptionUtil.unwindException(null, e);
		log.error(String.join("\n\tCaused by: ", excs));
		return rsp;
	}
}
