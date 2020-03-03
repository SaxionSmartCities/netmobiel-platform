package eu.netmobiel.communicator.util;

import eu.netmobiel.commons.NetMobielModule;
import eu.netmobiel.commons.util.UrnHelper;

public class CommunicatorUrnHelper extends UrnHelper {
	public static final NetMobielModule MODULE = NetMobielModule.COMMUNICATOR;
	
	public static String createUrnPrefix(Class<?> className) {
		return createUrnPrefix(className.getSimpleName());
	}

	public static String createUrnPrefix(String className) {
		return createUrnPrefix(MODULE.getCode(), className.toLowerCase());
	}

}
