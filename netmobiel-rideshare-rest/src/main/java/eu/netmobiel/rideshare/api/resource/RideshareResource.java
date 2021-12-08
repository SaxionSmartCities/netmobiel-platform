package eu.netmobiel.rideshare.api.resource;

import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * Base class for the rideshare resource handling. Contains a few convenience methods.
 * 
 * @author Jaap Reitsma
 *
 */
class RideshareResource {

    static Instant toInstant(OffsetDateTime odt) {
		return odt == null ? null : odt.toInstant();
	}

}
