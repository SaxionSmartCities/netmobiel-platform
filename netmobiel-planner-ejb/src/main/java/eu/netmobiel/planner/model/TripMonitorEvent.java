package eu.netmobiel.planner.model;

/**
 * Events occurring while monitoring the progress of a trip.
 * 
 * @author Jaap Reitsma
 *
 */
public enum TripMonitorEvent {
	/**
	 * It is time to check whether to start the monitoring. This is an artificial event. 
	 */
	TIME_TO_PREPARE_MONITORING,
	/**
	 * It is time to prepare for the trip. Inform the passenger about the upcoming departure. 
	 */
	TIME_TO_PREPARE,
	/**
	 * The trip has started.
	 */
	TIME_TO_DEPART,
	/**
	 * The passenger should have arrived by now (no active monitoring of the position of the passenger, only-time based).
	 */
	TIME_TO_ARRIVE,
	/**
	 * The passenger is now at the destination location for some time, send an invitation for a confirmation (only if requested, e.g. for rideshare) 
	 */
	TIME_TO_VALIDATE,
	/**
	 * It is time to complete the trip and terminate the monitoring.
	 */
	TIME_TO_COMPLETE

}
