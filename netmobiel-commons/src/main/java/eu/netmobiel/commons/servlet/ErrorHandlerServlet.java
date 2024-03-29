package eu.netmobiel.commons.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import eu.netmobiel.commons.api.ErrorResponse;

/**
 * Error handler of the last resort. Used to get a JSON response even for status code 403, which
 * is not handled by RestEasy.
 * 
 * @author Jaap Reitsma
 *
 */
public class ErrorHandlerServlet extends HttpServlet {

	private static final long serialVersionUID = -5098663689536882162L;

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		int statusCode = response.getStatus();

		if (statusCode >= 200 && statusCode < 299) {
			super.service(request, response);
		} else {
			response.setContentType(MediaType.APPLICATION_JSON);
			ErrorResponse rsp = new ErrorResponse(Response.Status.fromStatusCode(statusCode));
			@SuppressWarnings("resource")
			PrintWriter writer = response.getWriter();
			writer.write(rsp.stringify());
		}
	}
}