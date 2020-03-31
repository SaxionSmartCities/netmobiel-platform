package eu.netmobiel.banker.util;

import eu.netmobiel.commons.NetMobielModule;
import eu.netmobiel.commons.util.UrnHelper;

public class BankerUrnHelper extends UrnHelper {
	public static final NetMobielModule MODULE = NetMobielModule.BANKER;
	
	public static String createUrnPrefix(Class<?> className) {
		return createUrnPrefix(className.getSimpleName());
	}

	public static String createUrnPrefix(String className) {
		return createUrnPrefix(MODULE.getCode(), className.toLowerCase());
	}

}
