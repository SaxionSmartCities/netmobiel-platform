package eu.netmobiel.commons.util;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

/**
 * Interceptor for logging debug messages on dao's and beans 
 * @author Jaap Reitsma
 *
 */
@Priority(Interceptor.Priority.APPLICATION + 10)
@Interceptor
@Logging
public class DebugLogger implements Serializable {
	private static final long serialVersionUID = -6043539210117864252L;
	private static final boolean PRINT_CLASS_NAME = false;
	
	@Inject
	private Logger logger;
	
	@AroundInvoke
	public Object logMethodEntry(InvocationContext ic) throws Exception {
		if (logger.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder();
		    sb.append(ic.getTarget().getClass().getSimpleName());
			sb.append(".");
			sb.append(ic.getMethod().getName());
			sb.append("(");
			int count = 0;
			for (Object o : ic.getParameters()) {
				if (count > 0) {
					sb.append(", ");
				}
				sb.append(printValue(o));
				count++;
			}
			sb.append(")");
			logger.debug(sb.toString());
		}
		return ic.proceed();
	}
	
	private String printValue(Object parm) {
		String prefix = "";
		String value;
		if (parm == null) {
			value = "<null>";
		} else {
			// Should we print the class name? Often not really necessary
			if (PRINT_CLASS_NAME) {
				if (parm.getClass() != null) {
					prefix = parm.getClass().getSimpleName();
				} else {
					prefix = "<Unknown>";
				}
				prefix += " ";
			}
			try {
				if (parm.getClass().isArray()) {
					value = "{ "+ IntStream.range(0, Array.getLength(parm))
							.mapToObj(i -> Array.get(parm, i).toString())
							.collect(Collectors.joining(", ")) + " }";
				} else {
					value = parm.toString();
				}
				value = StringUtils.abbreviate(value, 80);
			} catch (Exception ex) {
				// Probably not initialized, ignore
				value = "<proxy>";
				logger.error("Error getting value", ex);
			}
		}
		return prefix + value;
	}
}
