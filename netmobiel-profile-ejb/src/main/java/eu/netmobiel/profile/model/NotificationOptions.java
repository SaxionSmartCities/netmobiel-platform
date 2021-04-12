package eu.netmobiel.profile.model;

import java.io.Serializable;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
@Vetoed
@Access(AccessType.FIELD)
public class NotificationOptions implements Serializable {
	private static final long serialVersionUID = 3574251533748362135L;

	
	@Column(name = "opt_messages", nullable = false)
	private boolean messages = true;
	
	@Column(name = "opt_shoutouts", nullable = false)
	private boolean shoutouts = true;

	@Column(name = "opt_trip_confirmations", nullable = false)
	private boolean tripConfirmations = true;

	@Column(name = "opt_trip_reminders", nullable = false)
	private boolean tripReminders = true;

	@Column(name = "opt_trip_updates", nullable = false)
	private boolean tripUpdates = true;

	public NotificationOptions() {
	}

	public static NotificationOptions createDefault() {
		return new NotificationOptions();
	}
	
	public boolean isMessages() {
		return messages;
	}
	
	public void setMessages(boolean messages) {
		this.messages = messages;
	}
	
	public boolean isShoutouts() {
		return shoutouts;
	}
	
	public void setShoutouts(boolean shoutouts) {
		this.shoutouts = shoutouts;
	}
	
	public boolean isTripConfirmations() {
		return tripConfirmations;
	}
	
	public void setTripConfirmations(boolean tripConfirmations) {
		this.tripConfirmations = tripConfirmations;
	}
	
	public boolean isTripReminders() {
		return tripReminders;
	}
	
	public void setTripReminders(boolean tripReminders) {
		this.tripReminders = tripReminders;
	}
	
	public boolean isTripUpdates() {
		return tripUpdates;
	}
	
	public void setTripUpdates(boolean tripUpdates) {
		this.tripUpdates = tripUpdates;
	}
}
