package eu.netmobiel.commons.jaxrs;

import java.util.List;

import javax.ejb.EJBAccessException;
import javax.ejb.EJBException;
import javax.persistence.OptimisticLockException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import eu.netmobiel.commons.api.ErrorResponse;
import eu.netmobiel.commons.util.ExceptionUtil;

/**
 * JAX-RS Exception mapper for building a customized error page in case of an EJBException.
 *
 * @author Jaap.Reitsma
 *
 */
@Provider
public class EJBExceptionMapper implements ExceptionMapper<EJBException> {

	/**
	 * Capture the cause of the exception and generate a proper HTTP status.
	 */
	@Override
	public Response toResponse(EJBException e) {
		Response rsp = null;
		Throwable dspExc = e.getCause() != null ? e.getCause() : e;
		/**
		 * Translate some EJB Exception to a different status.
		 * Otherwise it is always an internal server error. Web page will not disclose details, only the 
		 * log page contain the exception trace.
		 * EJB Exceptions are logged by the application server, don't log them again.
		 */
		Response.Status status = null;
		if (dspExc instanceof EJBAccessException || dspExc instanceof SecurityException) {
			status = Response.Status.FORBIDDEN;
		} else {
			List<Throwable> causes = ExceptionUtil.listCauses(e);
			if (causes.stream()
				.filter(t -> t instanceof OptimisticLockException)
				.findAny().isPresent()) {
				status = Response.Status.CONFLICT;	
			} else {
				status = Response.Status.INTERNAL_SERVER_ERROR;
			}
		}
		String[] msgs = ExceptionUtil.unwindExceptionMessage(null, dspExc);
		ErrorResponse err = new ErrorResponse(status, String.join(" - ", msgs));
		rsp =  Response.status(status).type(MediaType.APPLICATION_JSON).entity(err).build();
		return rsp;
	}
}
