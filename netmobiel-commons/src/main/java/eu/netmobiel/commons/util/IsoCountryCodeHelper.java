package eu.netmobiel.commons.util;

import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Retrieves the conversion of ISO 3166-1 alpha-3 country codes to alpha-2.
 * Not so straightforward as expected due to unexpected behaviour of the Locales.
 *  
 * @author Jaap Reitsma
 *
 */
public class IsoCountryCodeHelper {

	private static Map<String, String> isoCountryCode3To2Map =  
			Stream.of(Locale.getAvailableLocales())
				.filter(IsoCountryCodeHelper::hasISO3CountryCode)
				.collect(Collectors.toMap(Locale::getISO3Country, Locale::getCountry, (k, v) -> v));
	private static final boolean DEBUG = false;
	
	private IsoCountryCodeHelper() {
		// No instances
	}

	public static String getIso2CountryCode(String iso3CountryCode) {
		String code = iso3CountryCode; 
		if (iso3CountryCode != null && iso3CountryCode.length() > 2) {
			code = isoCountryCode3To2Map.get(iso3CountryCode);
		}
		return code;
	}
	
	private static boolean hasISO3CountryCode(Locale loc) {
		boolean hasCode = false;
		try {
			if (loc.getCountry() != null && !loc.getCountry().isBlank() && loc.getCountry().length() == 2) {
				String code3 = loc.getISO3Country();
				if (code3 != null && code3.length() == 3) {
					hasCode = true;
				}
			}
		} catch (MissingResourceException ex) {
			// Ignore
			if (DEBUG) {
				System.out.printf("Error %s - %s\n", loc, ex.getMessage());
			}
		}
		return hasCode;
	}

	public static void main(String[] args) {
		Stream.of(Locale.getAvailableLocales())
			.filter(loc -> hasISO3CountryCode(loc))
			.forEach(loc -> System.out.printf("%s - %s\n", loc.getISO3Country(), loc.getCountry()));
	}

}
