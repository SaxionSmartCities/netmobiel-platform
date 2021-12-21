package eu.netmobiel.rideshare.model;

/**
 * Events occurring while monitoring the progress of a ride.
 * 
 * @author Jaap Reitsma
 *
 */
public enum RideMonitorEvent {
	/**
	 * It is time to check whether the more active monitoring should take place. 
	 */
	TIME_TO_CHECK,
	/**
	 * It is time to prepare for the ride. Inform the driver about the upcoming departure. 
	 */
	TIME_TO_PREPARE,
	/**
	 * The trip has started.
	 */
	TIME_TO_DEPART,
	/**
	 * The driver should have arrived by now (no active monitoring of the position of the passenger, only-time based).
	 */
	TIME_TO_ARRIVE,
	/**
	 * The driver is now at the destination location for some time, send an invitation for a confirmation (only with a passenger) 
	 */
	TIME_TO_VALIDATE,
	/**
	 * The timer to send a validation reminder has expired. 
	 */
	TIME_TO_VALIDATE_REMINDER,
	/**
	 * It is time to complete the trip and terminate the monitoring.
	 */
	TIME_TO_COMPLETE

}
