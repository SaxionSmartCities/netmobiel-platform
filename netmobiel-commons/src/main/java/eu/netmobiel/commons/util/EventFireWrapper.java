package eu.netmobiel.commons.util;

import javax.enterprise.event.Event;
import javax.enterprise.event.ObserverException;

import eu.netmobiel.commons.exception.BusinessException;

public class EventFireWrapper {
	
	public static <T> void fire(Event<T> event, T eventObject) throws BusinessException {
		try {
			event.fire(eventObject);
		} catch (ObserverException ex) {
			if (ex.getCause() instanceof BusinessException) {
				throw (BusinessException) ex.getCause();
			}
			throw ex;
    	}
	}
}
