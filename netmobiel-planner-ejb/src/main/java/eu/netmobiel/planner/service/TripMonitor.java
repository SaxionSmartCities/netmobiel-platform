package eu.netmobiel.planner.service;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.Schedule;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.event.TripValidationEvent;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.repository.ClockDao;
import eu.netmobiel.commons.util.EventFireWrapper;
import eu.netmobiel.commons.util.ExceptionUtil;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.ValidEjbTimer;
import eu.netmobiel.planner.event.TripEvent;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripMonitorEvent;
import eu.netmobiel.planner.model.TripState;
import eu.netmobiel.planner.repository.TripDao;

/**
 * The monitor of the trips. This EJB is responsible for maintaining the proper state of the trips and legs in each trip.
 * It will observe relevant events and will update the state of the trip.
 * The state machine is built such to re-establish itself after startup: The state of the trip is calculated by 
 * looking at individual attributes of each trip. Events will only indirect cause transitions. 
 * The objective is to have a robust state machine that does not depend (only) on the occurrence of
 * a single event that might be missed because of some reason. Also, a restart of the application should 
 * start a fresh situation.   
 *
 * To prevent recursions, an observer method will not directly call the update state machine method, but instead
 * set a timer that immediately expires. 
 * 
 * @author Jaap Reitsma
 *
 */
@Stateless
@Logging
public class TripMonitor {
	/**
	 * The delay before sending a invitation for a confirmation.
	 */
	private static final Duration VALIDATION_DELAY = Duration.ofMinutes(15);
	/**
	 * The maximum duration of the first confirmation period.
	 */
	private static final Duration VALIDATION_INTERVAL = Duration.ofDays(2);
//	private static final Duration VALIDATION_INTERVAL = Duration.ofMinutes(2).plus(Duration.ofSeconds(10));
	/**
	 * The duration of the pre-departing period in which the monitoring should start or have started.
	 */
	private static final Duration PRE_DEPARTING_PERIOD = Leg.DEPARTING_PERIOD.plus(Duration.ofHours(2));

	/**
	 * The maximum number of reminders to sent during validation.
	 */
	private static final int MAX_REMINDERS = 2;

    @Resource
	private SessionContext sessionContext;

	@Inject
    private Logger log;

    @Inject
    private ClockDao clockDao;
    
    @Inject
    private TripDao tripDao;

    @Resource
    private TimerService timerService;

    @Inject
    private Event<TripValidationEvent> tripValidationEvent;
    

    @Inject
    private Event<TripEvent> tripEvent;

    private final static ValidEjbTimer validEjbTimer = new ValidEjbTimer();
    
    public static class TripInfo implements Serializable {
		private static final long serialVersionUID = -2715209888482006490L;
    	public Long tripId;
    	public TripInfo(Long aTripId) {
    		this.tripId = aTripId;
    	}
    	
		@Override
		public String toString() {
			return String.format("TripInfo [%s]", tripId);
		}
    }
    
    /**
     * Retrieves a trip. Anyone can read a trip, given the id. All details are retrieved.
     * @param id the trip id
     * @return a trip object
     * @throws NotFoundException No matching trip found.
     */
    private Trip getTrip(Long id) throws NotFoundException {
    	Trip tripdb = tripDao.loadGraph(id, Trip.DETAILED_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such trip: " + id));
    	return tripdb;
    }
    
    /**
     * Reset the state to arriving as if the trip was just finished. Update the state of the trip. 
     * @param ride
     * @throws BusinessException
     */
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void restartValidation(Trip trip) throws BusinessException {
    	if (trip.getState().isPostTravelState() ) {
    		// The fare was uncancelled or refunded, any way, the trip needs validation again
    		trip.setState(TripState.ARRIVING);
    		cancelTripTimer(trip, TripMonitorEvent.TIME_TO_CHECK);
    		updateTripStateMachine(trip);
    	}
    }

//    /**
//     * Evaluates the state of a leg.
//     * @param now The time reference.
//     * @param trip The trip of the leg
//     * @param leg The leg to evaluate.
//     * @return the actions resulting from leg state transitions to execute.
//     */
//    private List<Command> legStateMachineHandler(Instant now, Trip trip, Leg leg) {
//    	List<Command> actions = new ArrayList<>();
//    	// Determine the new state(s)
//    	TripState oldState = leg.getState();
//    	// Update the legs
//		TripState newState = leg.nextState(now);
//		leg.setState(newState);
//		if (log.isDebugEnabled()) {
//			log.debug(String.format("Leg SM %s --> %s: %s", oldState, newState, leg.toStringCompact()));
//		}
//		switch (newState) {
//		case PLANNING:
//			break;
//		case BOOKING:
//			if (oldState == TripState.PLANNING) {
//				// A booking is required. Create an action to inform
//   				// Use the trip as reference, we are not sure the leg ID is a stable, permanent identifier in case of an update of a trip.
//   				// Add the reference to the trip of the provider, e.g. the ride in case of rideshare.
//				actions.add(() -> EventFireWrapper.fire(bookingRequestedEvent, new BookingRequestedEvent(trip, leg)));
//			}
//			break;
//		case SCHEDULED:
//			if (oldState == TripState.PLANNING) {
//		    	// Check for bookingID set. If so than it was a shout-out and we need to convert the PROPOSAL to a CONFIRMED booking
//		    	if (leg.getBookingId() != null) {
//		    		// This must be a proposed booking. Confirm it and add a trip reference.
//		    		actions.add(() -> EventFireWrapper.fire(bookingConfirmedEvent, new BookingConfirmedEvent(trip, leg)));
//		    	}
//			}
//			break;
//		case DEPARTING:
//			break;
//		case IN_TRANSIT:
//			break;
//		case ARRIVING:
//			break;
//		case VALIDATING:
//			break;
//		case COMPLETED:
//			break;
//		case CANCELLED:
//			break;
//		}
//		return actions;
//    }

    /**
     * Evaluate the trip state. NOTE: Only call methods that do not call an update of the state machine, i.e. no recursion!
     * @param trip the trip to evaluate.
     * @throws BusinessException
     */
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void updateTripStateMachine(Trip trip) throws BusinessException {
//    	List<Command> actions = new ArrayList<>();
    	Instant now = clockDao.now();
    	// Determine the new state(s)
    	TripState oldState = trip.getState();
    	// Update the legs and collect the action to execute
//		trip.getItinerary().getLegs().forEach(lg -> actions.addAll(legStateMachineHandler(now, trip, lg)));
		TripState newState = trip.nextStateWithLegsToo(now);
		trip.setState(newState);
		if (log.isDebugEnabled()) {
			log.debug(String.format("Trip SM %s --> %s: %s", oldState, newState, trip.toStringCompact()));
		}
    	if (newState.ordinal() < oldState.ordinal()) {
    		log.warn(String.format("Trip %s: State is set back from %s --> %s", trip.getId(), oldState, newState));
    	}
		// Execute the delayed actions.
//		for (Command action : actions) {
//			action.execute();
//		}
		TripMonitorEvent event = TripMonitorEvent.TIME_TO_CHECK;
		Instant nextTimeout = null; 
		switch (newState) {
		case PLANNING:
			break;
		case BOOKING:
			// What happens if the state remains in booking? Is that possible?
			break;
		case SCHEDULED:
			if (now.plus(PRE_DEPARTING_PERIOD).isAfter(trip.getItinerary().getDepartureTime())) {
				nextTimeout = trip.getItinerary().getDepartureTime().minus(Leg.DEPARTING_PERIOD);
			}
			break;
		case DEPARTING:
			nextTimeout = trip.getItinerary().getDepartureTime();
			break;
		case IN_TRANSIT:
			nextTimeout = trip.getItinerary().getArrivalTime();
			break;
		case ARRIVING:
			Duration nextCheckDuration = trip.getItinerary().isConfirmationRequested() ? VALIDATION_DELAY : Leg.ARRIVING_PERIOD;   
			nextTimeout = trip.getItinerary().getArrivalTime().plus(nextCheckDuration);
			break;
		case VALIDATING:
			// In this state we wait for: 
			// 1. the driver to confirm the passenger's presence on the ride  
			// 2. the passenger to confirm his own presence at the trip
			// 3. the payment decision by the Overseer
			if (oldState != TripState.VALIDATING) {
				// Probably from arriving or from completed. Start or restart the validation, from now.
				trip.setValidationExpirationTime(now.plus(VALIDATION_INTERVAL.multipliedBy(1L + MAX_REMINDERS)));
				trip.setValidationReminderTime(now.plus(VALIDATION_INTERVAL));
				nextTimeout = trip.getValidationReminderTime(); 
			} else {
				if (trip.getItinerary().isPassengerConfirmationPending() && now.isAfter(trip.getValidationReminderTime())) {
					trip.setValidationReminderTime(now.plus(VALIDATION_INTERVAL));
					nextTimeout = trip.getValidationReminderTime();
					event = TripMonitorEvent.TIME_TO_SEND_VALIDATION_REMINDER;
				}
				if (now.isAfter(trip.getValidationExpirationTime())) {
		        	trip.setValidationExpirationTime(now.plus(VALIDATION_INTERVAL));
					nextTimeout = trip.getValidationExpirationTime();
					event = TripMonitorEvent.TIME_TO_FINISH_VALIDATION;
					// Trigger an asynchronous evaluation, use a separate event (processed only after a successful commit of the current update)
		        	EventFireWrapper.fire(tripValidationEvent, new TripValidationEvent(trip.getTripRef(), true));
				}
			}
			break;
		case COMPLETED:
			// Just to be sure
       		cancelTripTimers(trip);
			break;
		case CANCELLED:
       		cancelTripTimers(trip);
			break;
		}
		// Inform the observers
		EventFireWrapper.fire(tripEvent, new TripEvent(trip, event, oldState, newState));
		if (nextTimeout != null) {
			setupTimer(trip, nextTimeout);
		}
    }

    @Timeout
	public void onTimeout(Timer timer) {
		try {
			if (! (timer.getInfo() instanceof TripInfo)) {
				log.error("Don't know how to handle timeout: " + timer.getInfo());
				return;
			}
			TripInfo tripInfo = (TripInfo) timer.getInfo();
			if (log.isDebugEnabled()) {
				// Note: non-persistent timer does not have a handle! Will throw exception if you try.
				log.debug(String.format("onTimeout: Received trip event: %s", tripInfo.toString()));
			}
			Trip trip = getTrip(tripInfo.tripId);
			updateTripStateMachine(trip);
		} catch (BusinessException ex) {
			log.error(String.join("\n\t", ExceptionUtil.unwindException(ex)));
			log.info("Rollback status after exception: " + sessionContext.getRollbackOnly()); 
		} catch (NoSuchObjectLocalException ex) {
			log.error(String.format("Error handling timeout: %s", ex.toString()));
		}
	}

    /**
     * Update the trip state machine after an event. This call is intended to be called from a scheduled timer.
     * It has its own transaction to prevent one failure causing the failure of all. 
     * @param event the event that is is the trigger for the evaluation
     * @param tripId the ride involved
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateStateMachine(Long tripId) {
		try {
			Trip trip = getTrip(tripId);
			updateTripStateMachine(trip);
		} catch (BusinessException ex) {
			log.error(String.join("\n\t", ExceptionUtil.unwindException(ex)));
			log.info("Rollback status after exception: " + sessionContext.getRollbackOnly()); 
		} catch (Exception ex) {
			log.error(String.format("Error updating trip state nachine: %s", ex.toString()));
		}
    }

    /**
     * Updates the state machine asynchronously.
     * @param tripId
     */
    @Asynchronous
    public void updateStateMachineAsync(Long tripId) {
    	updateStateMachine(tripId);
    }

    @Schedule(info = "Collect due trips", hour = "*/1", minute = "1", second = "30", persistent = false /* non-critical job */)
	public void checkForDueTrips() {
    	//FIXME Change the algorithm such that there is just one time calling the state machine. Then there are no concurrency issues.
    	//      Would that help? If there is already a timer running, it has no use to run a schedule timer for that trip.
		// Get all trips that need monitoring and have a departure time within a certain window
		try {
			// Find all trips that depart before the indicated time (the plus is the ramp-up)
			List<Long> tripIds = tripDao.findTripsToMonitor(clockDao.now().plus(PRE_DEPARTING_PERIOD));
			for (Long tripId : tripIds) {
				// FIXME if not an active timer for this trip, then check it to see we need one.
				// If so then only set the timer, do not update the state.
				sessionContext.getBusinessObject(this.getClass()).updateStateMachine(tripId);
			}
		} catch (Exception ex) {
			log.error(String.format("Error handling timeout: %s", ex.toString()));
		}
	}

	private void setupTimer(Trip trip, Instant expirationTime) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setupTimer %s %s", trip.getTripRef(), expirationTime));
		}
		Collection<Timer> timers = getActiveTimers(trip);
		if (timers.size() > 1) {
			log.warn(String.format("Too many timers for Trip %s, cancelling all of them", trip.getId()));
			timers.forEach(tm -> tm.cancel());
		}
		Optional<Timer> tm = timers.stream().findFirst();
		if (tm.isPresent()) {
			Timer tmr = tm.get();
			if (!validEjbTimer.test(tmr) || !tmr.getNextTimeout().toInstant().equals(expirationTime)) {
//				log.debug("Canceling timer: " + tmr.getInfo());
				cancelTimer(tmr);
				tm = Optional.empty();
			}
		}
		if (tm.isEmpty()) {
			timerService.createTimer(Date.from(expirationTime), new TripInfo(trip.getId()));
		}
	}

    private void cancelTimer(Timer tm) {
		try {
			tm.cancel();
		} catch (Exception ex) {
			log.error(String.format("Error canceling timer: %s", ex.toString()));
		}
    }

    private Collection<Timer> getActiveTimers(Trip trip) {
    	Collection<Timer> timers = timerService.getTimers();
    	// Remove the timers that are not used with TripInfo
    	timers.removeIf(tm -> !(tm.getInfo() instanceof TripInfo && ((TripInfo)tm.getInfo()).tripId.equals(trip.getId())));
    	if (timers.size() > TripMonitorEvent.values().length) {
    		log.warn(String.format("getActiveTimers: %d timers active for trip %s!", timers.size(), trip.getId()));
    	}
    	return timers;
    }
    
	private void cancelTripTimers(Trip trip) {
    	// Find all timers related to this trip and cancel them
		getActiveTimers(trip).forEach(tm -> cancelTimer(tm));
	}

	private void cancelTripTimer(Trip trip, TripMonitorEvent event) {
		Collection<Timer> timers = getActiveTimers(trip);
		timers.forEach(tm -> cancelTimer(tm));
	}
	
	private List<TripInfo> listAllTripMonitorTimers() {
    	// Find all timers related to the trip manager
    	Collection<Timer> timers = timerService.getTimers();
    	Collection<Timer> invalidTimers = timers.stream()
        		.filter(validEjbTimer.negate())
        		.collect(Collectors.toList());
    	invalidTimers.stream()
    		.forEach(tm -> {
    			log.info(String.format("Cancelling invalid trip timer: %s", tm.getInfo()));
    			cancelTimer(tm);
    		});
    	timers.removeAll(invalidTimers);
    	timers.removeIf(tm -> !(tm.getInfo() instanceof TripInfo));
		if (timers.isEmpty()) {
			log.info("NO active trip timers");
		} else {
			log.info("Active trip timers:\n\t" + String.join("\n\t", 
					timers.stream()
					.map(tm -> String.format("%s %s %d %s", tm.getInfo(), tm.getNextTimeout(), tm.getTimeRemaining(), tm.isPersistent()))
					.collect(Collectors.toList()))
			);
		}		
    	return timers.stream()
    			.map(tm -> (TripInfo) tm.getInfo())
    			.collect(Collectors.toList());
	}
	
	/**
	 * Revive the trip monitors that have been crashed due due to some unrecoverable errors.
	 * This call is made on startup of the application only.
	 * Not used anymore, only for logging the active timers now.
	 */
	public void reviveTripMonitors() {
		listAllTripMonitorTimers();
//		checkForDueTrips();
	}
	
}
