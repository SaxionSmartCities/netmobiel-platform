package eu.netmobiel.commons.util;

import eu.netmobiel.commons.exception.BadRequestException;

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

	public static String createUrn(String prefix, String id) {
		return id == null ? null : prefix + id;
	}

	public static String getSuffix(String urn) {
		return urn == null ? null : urn.substring(urn.lastIndexOf(":") + 1);
	}

	public static String getPrefix(String urn) {
		return urn == null ? null : urn.substring(0, urn.lastIndexOf(":") + 1);
	}

	public static boolean matchesPrefix(String expectedPrefix, String urn) {
		return urn != null && urn.startsWith(expectedPrefix);
	}
	
	public static Long getId(String prefix, String value) throws BadRequestException {
		String id = value;
		if (isUrn(value)) {
			String actualPrefix = getPrefix(value);
			if (! prefix.equals(actualPrefix)) {
				throw new BadRequestException(String.format("Expected prefix %s, actual is %s", prefix, actualPrefix));
			}
			id = getSuffix(value);
		}
		if (id != null) {
			try {
				return Long.parseLong(id);
			} catch (NumberFormatException ex) {
				throw new BadRequestException("Invalid identifier: " + value, ex);
			}
		}
		return null;
	}
	
	public static String getIdAsString(String prefix, String value) throws BadRequestException {
		String id = value;
		if (isUrn(value)) {
			String actualPrefix = getPrefix(value);
			if (! prefix.equals(actualPrefix)) {
				throw new BadRequestException(String.format("Expected prefix %s, actual is %s", prefix, actualPrefix));
			}
			id = getSuffix(value);
		}
		return id;
	}

	public static Long getId(String value) {
		return value == null ? null : Long.parseLong(value);
	}

	public static boolean isUrn(String value) {
		return value != null && value.startsWith("urn:"); 
	}
	
	public static String getService(String urn) {
		if (!isUrn(urn)) {
			throw new IllegalArgumentException("Not an urn: " + urn);
		}
		String[] parts = urn.split(":", 5);
		if (! "nb".equals(parts[1])) {
			throw new IllegalArgumentException("Not a Netmobiel urn: " + urn);
		}
		return parts[2];
	}

	/** 
	 * Checks for a string that could be a keycloak managed identity (an UUID). 
	 * The check is extremely superficial,
	 * @param value the string to check
	 * @return true if not null and containing at least one '-' character, and also is not a urn.
	 */
	public static boolean isKeycloakManagedIdentity(String value) {
		return value != null && value.contains("-") && !value.contains(":");
	}
}
