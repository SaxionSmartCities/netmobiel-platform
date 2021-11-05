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
	 * Returns the body text of the message.
	 * @return a text or null if none set.
	 */
	String getBody();
	/**
	 * Returns the context of message. The context is a urn referring to the business object triggering the message. 
	 * @return the context urn. 
	 */
	String getContext();
	/**
	 * Returns the context as a textual message. 
	 * @return a text or null if not set.
	 */
	String getSubject();
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
