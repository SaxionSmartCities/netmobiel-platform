package eu.netmobiel.commons.util;

public class UrnHelper {
	public static final String URN_FORMAT = "urn:nb:%s:%s:"; 

	public static String createUrnPrefix(String module, Class<?> className) {
		return createUrnPrefix(module, className.getSimpleName().toLowerCase());
	}

	public static String createUrnPrefix(String module, String className) {
		return String.format(URN_FORMAT, module, className);
	}

	public static String createUrn(String prefix, Long id) {
		return id == null ? null : prefix + id.toString();
	}

	public static String getSuffix(String urn) {
		return urn == null ? null : urn.substring(urn.lastIndexOf(":") + 1);
	}

	public static Long getId(String prefix, String value) {
		String id = value;
		if (isUrn(value)) {
			if (! value.startsWith(prefix)) {
				throw new IllegalArgumentException(String.format("Expected prefix %s, actual is %s", prefix, value));
			}
			id = getSuffix(value);
		}
		return id == null ? null : Long.parseLong(id);
	}
	
	public static boolean isUrn(String value) {
		return value != null && value.startsWith("urn:"); 
	}
}
