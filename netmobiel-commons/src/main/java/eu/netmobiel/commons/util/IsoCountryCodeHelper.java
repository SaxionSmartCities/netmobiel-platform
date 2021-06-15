package eu.netmobiel.commons.util;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IsoCountryCodeHelper {

	private static Map<String, String> isoCountryCode3To2Map = 
			Stream.of(Locale.getAvailableLocales())
				.collect(Collectors.toMap(Locale::getISO3Country, Locale::getCountry));

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

}
