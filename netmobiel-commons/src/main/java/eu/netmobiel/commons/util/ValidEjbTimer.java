package eu.netmobiel.commons.util;

import java.util.function.Predicate;

import javax.ejb.NoMoreTimeoutsException;
import javax.ejb.Timer;

public class ValidEjbTimer implements Predicate<Timer>{
	
	@Override
	public boolean test(Timer timer) {
		boolean valid = false;
		try {
			timer.getNextTimeout();
			// No exceptions raised, the timer seems valid.
			valid = true;
		} catch (NoMoreTimeoutsException ex) {
			// Ok, this timer exists, but it will not timeout anymore.
		}
		return valid;
	}

}
