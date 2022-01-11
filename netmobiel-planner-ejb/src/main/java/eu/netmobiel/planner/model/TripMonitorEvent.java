package eu.netmobiel.planner.model;

/**
 * Events occurring while monitoring the progress of a trip.
 * 
 * @author Jaap Reitsma
 *
 */
public enum TripMonitorEvent {
	/**
	 * It is time to check whether the more active monitoring should take place. 
	 */
	TIME_TO_CHECK,
	/**
	 * The timer to send a validation reminder has expired. 
	 */
	TIME_TO_SEND_VALIDATION_REMINDER,
	/**
	 * The timer to finish the validation. 
	 */
	TIME_TO_FINISH_VALIDATION,
}
