package eu.netmobiel.commons.util;

import java.io.Serializable;

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
	@Inject
	private Logger logger;
	
	@AroundInvoke
	public Object logMethodEntry(InvocationContext ic) throws Exception {
		if (logger.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder();
			sb.append("Call: ");
		    sb.append(ic.getMethod().getDeclaringClass().getSimpleName());
			sb.append(".");
			sb.append(ic.getMethod().getName());
			sb.append("(");
			int count = 0;
			for (Object o : ic.getParameters()) {
				if (o != null) {
					if (count > 0) {
						sb.append(", ");
					}
					if (o.getClass() != null) {
						sb.append(o.getClass().getSimpleName());
					} else {
						sb.append("<Unknown>");
					}
					sb.append(" ");
					try {
						sb.append(StringUtils.abbreviate(o.toString(), 80));
					} catch (Exception ex) {
						// Probably not initialized, ignore
						sb.append("<proxy>");
					}
					count++;
				}
			}
			sb.append(")");
			logger.debug(sb.toString());
		}
		return ic.proceed();
	}
}
