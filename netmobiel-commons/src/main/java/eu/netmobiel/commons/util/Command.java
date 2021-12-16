package eu.netmobiel.commons.util;

import eu.netmobiel.commons.exception.BusinessException;

@FunctionalInterface
public interface Command {
	void execute() throws BusinessException;
}
