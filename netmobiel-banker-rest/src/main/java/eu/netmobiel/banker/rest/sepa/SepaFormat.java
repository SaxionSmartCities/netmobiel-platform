package eu.netmobiel.banker.rest.sepa;

import java.util.regex.Pattern;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

public class SepaFormat {
	public static final String REGEX_IDENTIFIER = "[a-zA-Z0-9/\\-?()., ]+";
	public static final String REGEX_TEXT = REGEX_IDENTIFIER;
	private static final Pattern FORBIDDEN_CHARS = Pattern.compile("[^a-zA-Z0-9/\\-?()., ]");
	public static final int MAX_LENGTH_IDENTIFIER = 35; 
	public static final int MAX_LENGTH_TEXT = 140; 

	public static String sepaString(String value, int maxLength) {
		value = StringUtils.stripAccents(value);
		value = RegExUtils.removeAll(value, FORBIDDEN_CHARS);
		value = StringUtils.normalizeSpace(value);
		value = StringUtils.substring(value, 0, maxLength);
		return value;
	}
	
	public static String identifier(String value) {
		return sepaString(value, MAX_LENGTH_IDENTIFIER);
	}

	public static String text(String value) {
		return sepaString(value, MAX_LENGTH_TEXT);
	}
}
