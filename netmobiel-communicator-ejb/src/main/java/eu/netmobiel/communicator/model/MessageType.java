package eu.netmobiel.communicator.model;

public enum MessageType {
	// Important system message, cannot be blocked
	// Subject can be anything
	SYSTEM_MESSAGE,

	// The following types relate to the profile notification options
	// System message 
	// Subject can be anything
	GENERAL_MESSAGE,
	
	// Call-to-action for a shout-out 
	COMMUNITY_SHOUT_OUT,
	
	// Confirmation of a trip or ride
	TRIP_CONFIRMATION,

	// Reminder for a trip- or ride-related action. Which messages are involved?
	TRIP_REMINDER,
	
	// Updates on a trip or ride
	TRIP_UPDATE;
}
