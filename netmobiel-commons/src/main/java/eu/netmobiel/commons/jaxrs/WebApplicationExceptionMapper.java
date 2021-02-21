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
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.PaymentException;
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
		Throwable t = e;
		String errorCode = null;
		Response.StatusType status = e.getResponse().getStatusInfo();
		if (e.getCause() instanceof BusinessException) {
			t = e.getCause();
			BusinessException ae = (BusinessException) t;
			errorCode = ae.getVendorCode();
			status = Response.Status.INTERNAL_SERVER_ERROR;
			if (ae instanceof BadRequestException) {
				status = Response.Status.BAD_REQUEST;
			} else if (ae instanceof NotFoundException) {
				status = Response.Status.NOT_FOUND;
			} else if (ae instanceof PaymentException) {
				status = Response.Status.PAYMENT_REQUIRED;
			} else {
				status = ExtendedStatus.UNPROCESSIBLE_ENTITY;
			}
		}
		String[] msgs = ExceptionUtil.unwindExceptionMessage(null, t);
		ErrorResponse err = new ErrorResponse(status, errorCode, String.join(" - ", msgs));
		rsp =  Response.status(status).type(MediaType.APPLICATION_JSON).entity(err).build();
		if (e instanceof ServerErrorException || status == Response.Status.INTERNAL_SERVER_ERROR) {
			// Log stackdump
			log.error("Server error", t);
		} else {
			// Log message only
			String[] excs = ExceptionUtil.unwindException(null, t);
			log.error(String.join("\n\tCaused by: ", excs));
		}
		return rsp;
	}
}
