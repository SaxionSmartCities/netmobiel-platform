package eu.netmobiel.profile.util;

import eu.netmobiel.commons.NetMobielModule;
import eu.netmobiel.commons.util.UrnHelper;

public class ProfileUrnHelper extends UrnHelper {
	public static final NetMobielModule MODULE = NetMobielModule.PROFILE;
	
	public static String createUrnPrefix(Class<?> className) {
		return createUrnPrefix(className.getSimpleName());
	}

	public static String createUrnPrefix(String className) {
		return createUrnPrefix(MODULE.getCode(), className.toLowerCase());
	}

}
