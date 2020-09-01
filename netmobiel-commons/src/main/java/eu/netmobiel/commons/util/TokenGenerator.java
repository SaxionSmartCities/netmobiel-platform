package eu.netmobiel.commons.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;

public class TokenGenerator {
	private static final Random random = new SecureRandom();

	/**
	 * Creates a (secure) random UID of 130 bits, stored in (at most) 26 characters, 5 bits per character.
	 * Note that leading zero's are not included. 
	 * 
	 * @return A uid.
	 */
	public static String createSecureToken() {
		return new BigInteger(130, random).toString(32);
	}

	public static String createUUID() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Creates a UID of 130 bits, stored in (at most) 26 characters, 5 bits per character.
	 * Note that leading zero's are not included. 
	 * @param input The input string to create a digest from.
	 * @return A uid.
	 */
	public static String createToken(String input) {
		String uid = null;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
		    md.update(input.getBytes());
		    byte[] digest = md.digest();
			uid = new BigInteger(1, digest).toString(32);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Unable to create digest",  e);
		}
		return uid;
	}
}
