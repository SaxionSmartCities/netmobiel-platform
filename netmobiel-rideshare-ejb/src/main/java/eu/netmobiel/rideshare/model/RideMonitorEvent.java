package eu.netmobiel.rideshare.model;

/**
 * Events occurring while monitoring the progress of a ride.
 * 
 * @author Jaap Reitsma
 *
 */
public enum RideMonitorEvent {
	/**
	 * It is time to evaluate the state the ride. 
	 */
	TIME_TO_CHECK,
	/**
	 * The timer to send a validation reminder has expired. 
	 */
	TIME_TO_SEND_VALIDATION_REMINDER,

}
