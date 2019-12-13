package eu.netmobiel.commons.util;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class UriHelper {
	public static String encode(String s) {
    	try {
			return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Unexpected exception", e);
		}
    }

	public static String decode(String s) {
    	try {
			return URLDecoder.decode(s, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Unexpected exception", e);
		}
    }

	public static URI createURI(String path) {
    	try {
			return new URI(path);
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Unexpected exception", e);
		}
    }
}
