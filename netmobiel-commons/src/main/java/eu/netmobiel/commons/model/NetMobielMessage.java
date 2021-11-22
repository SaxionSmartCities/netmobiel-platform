package eu.netmobiel.commons.model;

import java.time.Instant;

/**
 * Definition of a message in NetMobiel.
 * 
 * @author Jaap Reitsma
 *
 */
public interface NetMobielMessage {

	/**
	 * The unique identifier of the message
	 * @return The urn of the message
	 */
	String getUrn();
	
	/**
	 * Returns the body text of the message.
	 * @return a text or null if none set.
	 */
	String getBody();

	/**
	 * Returns the creation time of the message.
	 * @return the creation time. Never null.
	 */
	Instant getCreatedTime();
	/**
	 * Returns the sender of the message
	 * @return the sender object.
	 */
	NetMobielUser getSender();

}
