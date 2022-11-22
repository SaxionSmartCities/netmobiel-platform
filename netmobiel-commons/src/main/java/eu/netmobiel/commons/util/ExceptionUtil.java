package eu.netmobiel.commons.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.Response;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.SystemException;

public class ExceptionUtil {
	private ExceptionUtil() {
		// No instances allowed
	}
	
	public static String[] unwindException(Throwable exc) {
		return unwindException(null, exc);
	}

	public static List<Throwable> listCauses(Throwable exc) {
		return Stream.iterate(exc.getCause(), t -> t != null, t -> t.getCause())
			.collect(Collectors.toList());
	}

	public static String[] unwindException(String msg, Throwable exc) {
		return unwindException(msg, exc, e -> e.toString()); 
	}
	
	public static String[] unwindExceptionMessage(String msg, Throwable exc) {
		return unwindException(msg, exc, e -> e.getLocalizedMessage()); 
	}

	static String[] unwindException(String msg, Throwable exc, Function<Throwable, String> extractor) {
		List<String> messages = new ArrayList<>();
		if (msg != null) {
			messages.add(msg);
		}
		Throwable t = exc;
		while (t != null) {
			messages.add(extractor.apply(t));
			if (t.getCause() == null) {
				break;
			}
			t = t.getCause();
		}
		return messages.toArray(new String[messages.size()]);
	}

	public static void throwExceptionFromResponse(String applicationMessage, Response response) 
			throws BusinessException, SystemException, SecurityException {
		String reason = response.readEntity(String.class);
		String message = String.format("%s: %d - %s", applicationMessage, response.getStatus(), reason);
		if (response.getStatus() >= Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
			throw new SystemException(message);
		} else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
			throw new NotFoundException(message);
		} else if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
			throw new BadRequestException(message);
		} else if (response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode()) {
			throw new SecurityException(message);
		} else if (response.getStatus() == Response.Status.FORBIDDEN.getStatusCode()) {
			throw new SecurityException(message);
		} else {
			throw new BusinessException(message);
		}
	}
	
}
