package eu.netmobiel.commons.repository;

import java.time.Instant;

import javax.enterprise.context.ApplicationScoped;

/**
 * Class for abstracting from the time. Useful for mocking 'now'.
 * @author Jaap Reitsma
 *
 */
@ApplicationScoped
public class ClockDao {

	/**
	 * Returns the current clock time as an Instant object.
	 * @return the current time.
	 */
	public Instant now() {
		return Instant.now();
	}
}
