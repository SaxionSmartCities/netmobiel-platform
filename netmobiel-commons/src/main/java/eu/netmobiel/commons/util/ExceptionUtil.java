package eu.netmobiel.commons.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ExceptionUtil {
	private ExceptionUtil() {
		// No instances allowed
	}
	
	public static String[] unwindException(Throwable exc) {
		return unwindException(null, exc);
	}

	public static String[] unwindException(String msg, Throwable exc) {
		return unwindException(msg, exc, e -> e.toString()); 
	}
	
	public static String[] unwindExceptionMessage(String msg, Throwable exc) {
		return unwindException(msg, exc, e -> e.getLocalizedMessage()); 
	}

	static String[] unwindException(String msg, Throwable exc, Function<Throwable, String> extractor) {
		List<String> messages = new ArrayList<String>();
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

}
