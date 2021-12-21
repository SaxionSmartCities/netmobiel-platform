package eu.netmobiel.planner.service;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.Schedule;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.repository.ClockDao;
import eu.netmobiel.commons.util.Command;
import eu.netmobiel.commons.util.EventFireWrapper;
import eu.netmobiel.commons.util.ExceptionUtil;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.ValidEjbTimer;
import eu.netmobiel.planner.event.BookingConfirmedEvent;
import eu.netmobiel.planner.event.BookingRequestedEvent;
import eu.netmobiel.planner.event.TripEvent;
import eu.netmobiel.planner.event.TripStateUpdatedEvent;
import eu.netmobiel.planner.event.TripValidationExpiredEvent;
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
//	private static final Duration VALIDATION_INTERVAL = Duration.ofDays(2);
	private static final Duration VALIDATION_INTERVAL = Duration.ofMinutes(10);
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
    private Event<BookingRequestedEvent> bookingRequestedEvent;

    @Inject
    private Event<BookingConfirmedEvent> bookingConfirmedEvent;

    @Inject
    private Event<TripStateUpdatedEvent> tripStateUpdatedEvent;

    @Inject
    private Event<TripValidationExpiredEvent> tripValidationExpiredEvent;

    @Inject
    private Event<TripEvent> tripEvent;

    private final static ValidEjbTimer validEjbTimer = new ValidEjbTimer();
    
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
     * Start a timer to check the state machine.
     * TODO: Does this timer interfere with the other timers? A timer event might get lost because of the deletion of a timer?
     * @param trip the trip to check
     */
    public void evaluateStateMachineDelayed(Trip trip) {
		setupTimer(trip, TripMonitorEvent.TIME_TO_CHECK, clockDao.now());
    }
    
    public void restartValidation(Trip trip) throws BusinessException {
    	if (trip.getState().isPostTravelState() ) {
    		// The fare was uncancelled or refunded, any way, the trip needs validation again
    		// Use the timer to decouple to assure no recursion occurs
    		trip.setState(TripState.ARRIVING);
    		setupTimer(trip, TripMonitorEvent.TIME_TO_VALIDATE, clockDao.now());
    	}
    }

    /**
     * Evaluates the state of a leg.
     * @param now The time reference.
     * @param trip The trip of the leg
     * @param leg The leg to evaluate.
     * @return the actions resulting from leg state transitions to execute.
     */
    private List<Command> legStateMachineHandler(Instant now, Trip trip, Leg leg) {
    	List<Command> actions = new ArrayList<>();
    	// Determine the new state(s)
    	TripState oldState = leg.getState();
    	// Update the legs
		TripState newState = leg.nextState(now);
		leg.setState(newState);
		if (log.isDebugEnabled()) {
			log.debug(String.format("Leg SM %s --> %s: %s", oldState, newState, leg.toStringCompact()));
		}
		switch (newState) {
		case PLANNING:
			break;
		case BOOKING:
			if (oldState == TripState.PLANNING) {
				// A booking is required. Create an action to inform
   				// Use the trip as reference, we are not sure the leg ID is a stable, permanent identifier in case of an update of a trip.
   				// Add the reference to the trip of the provider, e.g. the ride in case of rideshare.
				actions.add(() -> EventFireWrapper.fire(bookingRequestedEvent, new BookingRequestedEvent(trip, leg)));
			}
			break;
		case SCHEDULED:
			if (oldState == TripState.PLANNING) {
		    	// Check for bookingID set. If so than it was a shout-out and we need to convert the PROPOSAL to a CONFIRMED booking
		    	if (leg.getBookingId() != null) {
		    		// This must be a proposed booking. Confirm it and add a trip reference.
		    		actions.add(() -> EventFireWrapper.fire(bookingConfirmedEvent, new BookingConfirmedEvent(trip, leg)));
		    	}
			}
			break;
		case DEPARTING:
			break;
		case IN_TRANSIT:
			break;
		case ARRIVING:
			break;
		case VALIDATING:
			break;
		case COMPLETED:
			break;
		case CANCELLED:
			break;
		}
		return actions;
    }

    /**
     * Evaluate the trip state. NOTE: Only call methods that do not call an update of the state machine, i.e. no recursion!
     * @param trip the trip to evaluate.
     * @throws BusinessException
     */
    public void updateTripStateMachine(Trip trip) throws BusinessException {
    	List<Command> actions = new ArrayList<>();
    	Instant now = clockDao.now();
    	// Determine the new state(s)
    	TripState oldState = trip.getState();
    	// Update the legs and collect the action to execute
		trip.getItinerary().getLegs().forEach(lg -> actions.addAll(legStateMachineHandler(now, trip, lg)));
		TripState newState = trip.nextState(now);
		trip.setState(newState);
		if (log.isDebugEnabled()) {
			log.debug(String.format("Trip SM %s --> %s: %s", oldState, newState, trip.toStringCompact()));
		}
    	if (newState.ordinal() < oldState.ordinal()) {
    		log.warn(String.format("Trip %s: State is set back from %s --> %s", trip.getId(), oldState, newState));
    	}
		// Execute the delayed actions.
		for (Command action : actions) {
			action.execute();
		}
		switch (newState) {
		case PLANNING:
			break;
		case BOOKING:
			// What happens if the state remains in booking? Is that possible?
			break;
		case SCHEDULED:
			if (trip.getItinerary().getDepartureTime().minus(PRE_DEPARTING_PERIOD).isAfter(now)) {
				setupTimer(trip, TripMonitorEvent.TIME_TO_PREPARE, 
						trip.getItinerary().getDepartureTime().minus(Leg.DEPARTING_PERIOD));
			} else {
				cancelTripTimer(trip);
			}
			break;
		case DEPARTING:
			setupTimer(trip, TripMonitorEvent.TIME_TO_DEPART, trip.getItinerary().getDepartureTime());
			break;
		case IN_TRANSIT:
			setupTimer(trip, TripMonitorEvent.TIME_TO_ARRIVE, trip.getItinerary().getArrivalTime());
			break;
		case ARRIVING:
			if (trip.getItinerary().isConfirmationRequested()) {
				setupTimer(trip, TripMonitorEvent.TIME_TO_VALIDATE, 
						trip.getItinerary().getArrivalTime().plus(VALIDATION_DELAY));
			} else {
				setupTimer(trip, TripMonitorEvent.TIME_TO_COMPLETE, trip.getItinerary().getArrivalTime().plus(Leg.ARRIVING_PERIOD));
			}
			break;
		case VALIDATING:
			int nrRemindersLeft = MAX_REMINDERS;
			if (oldState != TripState.VALIDATING) {
				// Probably from arriving or from completed. Start or restart the validation, from now.
				trip.setValidationExpirationTime(now.plus(VALIDATION_INTERVAL.multipliedBy(1 + MAX_REMINDERS)));
				trip.setValidationReminderTime(now.plus(VALIDATION_INTERVAL));
				//FIXME The reminder should be a persistent timer firing an event.
			} else {
		    	Duration d = Duration.between(clockDao.now(), trip.getValidationExpirationTime());
		    	if (d.isNegative()) {
		    		d = Duration.ZERO;		    		
		    	}
		    	nrRemindersLeft = Math.toIntExact(d.dividedBy(VALIDATION_INTERVAL));
		    	Duration upToNextTime = d.minus(VALIDATION_INTERVAL.multipliedBy(nrRemindersLeft)); 
		    	if (!upToNextTime.isZero()) {
		    		trip.setValidationReminderTime(now.plus(upToNextTime));
		    	}
			}
			setupTimer(trip, nrRemindersLeft > 0 ? TripMonitorEvent.TIME_TO_VALIDATE_REMINDER : TripMonitorEvent.TIME_TO_COMPLETE, 
					trip.getValidationReminderTime());
			break;
		case COMPLETED:
			// Just to be sure
       		cancelTripTimer(trip);
			break;
		case CANCELLED:
       		cancelTripTimer(trip);
			break;
		}
		// Inform the observers
    	EventFireWrapper.fire(tripStateUpdatedEvent, new TripStateUpdatedEvent(oldState, trip));
    }

    public static class TripInfo implements Serializable {
		private static final long serialVersionUID = -2715209888482006490L;
		public TripMonitorEvent event;
    	public Long tripId;
    	public TripInfo(TripMonitorEvent anEvent, Long aTripId) {
    		this.event = anEvent;
    		this.tripId = aTripId;
    	}
    	
		@Override
		public String toString() {
			return String.format("TripInfo [%s %s]", event, tripId);
		}
    }
    
	private void handleTripMonitorEvent(Trip trip, TripMonitorEvent event) throws BusinessException {
		if (trip.getState() == TripState.CANCELLED) {
			log.warn("Cannot monitor, trip has been canceled: " + trip.getId());
			return;
		}
		updateTripStateMachine(trip);
		switch (event) {
		case TIME_TO_CHECK:
			break;
		case TIME_TO_PREPARE:
			break;
		case TIME_TO_DEPART:
			break;
		case TIME_TO_ARRIVE:
			break;
		case TIME_TO_VALIDATE:
			break;
		case TIME_TO_VALIDATE_REMINDER:
			break;
		case TIME_TO_COMPLETE:
			if (trip.getState() == TripState.VALIDATING) {
				EventFireWrapper.fire(tripValidationExpiredEvent, new TripValidationExpiredEvent(trip));
			}
			break;
		default:
			log.warn("Don't know how to handle event: " + event);
			break;
		}
		EventFireWrapper.fire(tripEvent, new TripEvent(event, trip));
		// The state machine is evaluated without regarding the event!
		// The timer is set by a state machine action
		updateTripStateMachine(trip);
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
			handleTripMonitorEvent(trip, tripInfo.event);
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
    public void handleTripEvent(TripMonitorEvent event, Long tripId) {
		try {
			// Find all rides that depart before the indicated time (the plus is the ramp-up)
			Trip trip = getTrip(tripId);
			handleTripMonitorEvent(trip, event);
		} catch (BusinessException ex) {
			log.error(String.join("\n\t", ExceptionUtil.unwindException(ex)));
			log.info("Rollback status after exception: " + sessionContext.getRollbackOnly()); 
		} catch (Exception ex) {
			log.error(String.format("Error updating trip state nachine: %s", ex.toString()));
		}
    }

    @Schedule(info = "Collect due trips", hour = "*/1", minute = "0", second = "0", persistent = false /* non-critical job */)
	public void checkForDueTrips() {
		// Get all trips that need monitoring and have a departure time within a certain window
		try {
			// Find all trips that depart before the indicated time (the plus is the ramp-up)
			List<Long> tripIds = tripDao.findTripsToMonitor(clockDao.now().plus(PRE_DEPARTING_PERIOD));
			for (Long tripId : tripIds) {
				sessionContext.getBusinessObject(this.getClass()).handleTripEvent(TripMonitorEvent.TIME_TO_CHECK, tripId);
			}
		} catch (Exception ex) {
			log.error(String.format("Error handling timeout: %s", ex.toString()));
		}
	}

	private void setupTimer(Trip trip, TripMonitorEvent event, Instant expirationTime) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setupTimer %s %s %s", trip.getTripRef(), event, expirationTime));
		}
		Collection<Timer> timers = getActiveTimers(trip);
		if (timers.size() > 1) {
			log.warn(String.format("Too many timers for Trip %s, cancelling all of them", trip.getId()));
			timers.forEach(tm -> tm.cancel());
		}
		Optional<Timer> tm = timers.stream().findFirst();
		if (tm.isPresent()) {
			Timer tmr = tm.get();
			if ((((TripInfo) tmr.getInfo()).event != event 
					|| !validEjbTimer.test(tmr)
					|| !tmr.getNextTimeout().toInstant().equals(expirationTime))) {
				log.debug("Canceling timer: " + tmr.getInfo());
				tmr.cancel();
				tm = Optional.empty();
			}
		}
		if (tm.isEmpty()) {
			timerService.createSingleActionTimer(Date.from(expirationTime), 
					new TimerConfig(new TripInfo(event, trip.getId()), false));
		}
	}

    private Collection<Timer> getActiveTimers(Trip trip) {
    	Collection<Timer> timers = timerService.getTimers();
    	timers.removeIf(tm -> !(tm.getInfo() instanceof TripInfo && ((TripInfo)tm.getInfo()).tripId.equals(trip.getId())));
//    	if (log.isDebugEnabled() && timers.size() < 2) {
//    		log.debug(String.format("getActiveTimers: %d timers in total active for this EJB", timers.size()));
//    	}
    	if (timers.size() >= 2) {
    		log.warn(String.format("getActiveTimers: %d timers active for trip %s", timers.size(), trip.getId()));
    	}
    	return timers;
    }
    
	private void cancelTripTimer(Trip trip) {
    	// Find all timers related to this trip and cancel them
		getActiveTimers(trip).forEach(tm -> tm.cancel());
	}

//    private Optional<Timer> getActiveTimer(Trip trip) {
//    	return getActiveTimers(trip).stream().findFirst();
//    }

//    private void checkToStartMonitoring(Trip trip) {
//		if (trip.getState() == TripState.CANCELLED) {
//			log.warn("Cannot monitor, trip has been canceled: " + trip.getId());
//			return;
//		}
//   		Duration timeLeftToDeparture = Duration.between(Instant.now(), trip.getItinerary().getDepartureTime());
//		if (log.isDebugEnabled()) {
//			log.debug(String.format("Trip %s is scheduled, time left to departure is %s", trip.getId(), timeLeftToDeparture.toString()));
//		}
//    	if (! trip.isMonitored() && timeLeftToDeparture.compareTo(PRE_DEPARTING_PERIOD) < 0) {
//    		if (log.isDebugEnabled()) {
//    			log.debug("Start monitoring trip " + trip.getId());
//    		}
//    		trip.setMonitored(true);
//    		// Should we generate multiple timer events and let the state machine decide what to do?
//    		// Tested. Result: Timer events are received in random order, not in the order created.
//    		// Workaround: Set the next timer successively in each event handler
//    		timerService.createTimer(Date.from(trip.getItinerary().getDepartureTime().minus(Leg.DEPARTING_PERIOD)), 
//    				new TripInfo(TripMonitorEvent.TIME_TO_PREPARE, trip.getId()));
//    	}
//    }

	private List<TripInfo> listAllTripMonitorTimers() {
    	// Find all timers related to the trip manager
    	Collection<Timer> timers = timerService.getTimers();
    	Collection<Timer> invalidTimers = timers.stream()
        		.filter(validEjbTimer.negate())
        		.collect(Collectors.toList());
    	invalidTimers.stream()
    		.forEach(tm -> {
    			log.info(String.format("Cancelling invalid trip timer: %s", tm.getInfo()));
    			tm.cancel();
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
	 * Create a map to revive the monitor. Use the event that would cause the favourable transition.
	 * Note that only trips that are monitored are considered, e.g. SCHEDULED AND monitored = true. 
	 */
//	private static Map<TripState, TripMonitorEvent> tripStateToMonitorRevivalEvent = Map.ofEntries(
//			new AbstractMap.SimpleEntry<>(TripState.SCHEDULED, TripMonitorEvent.TIME_TO_PREPARE_MONITORING),
//			new AbstractMap.SimpleEntry<>(TripState.DEPARTING, TripMonitorEvent.TIME_TO_PREPARE),
//			new AbstractMap.SimpleEntry<>(TripState.IN_TRANSIT, TripMonitorEvent.TIME_TO_DEPART),
//			new AbstractMap.SimpleEntry<>(TripState.ARRIVING, TripMonitorEvent.TIME_TO_ARRIVE),
//			new AbstractMap.SimpleEntry<>(TripState.VALIDATING, TripMonitorEvent.TIME_TO_VALIDATE),
//			new AbstractMap.SimpleEntry<>(TripState.COMPLETED, TripMonitorEvent.TIME_TO_COMPLETE)
//		);
	
	/**
	 * Revive the trip monitors that have been crashed due due to some unrecoverable errors.
	 */
	public void reviveTripMonitors() {
//		List<TripInfo> tripInfos = 
		listAllTripMonitorTimers();
		checkForDueTrips();
//		
//		Set<Long> timedTripIds = tripInfos.stream()
//				.map(ti -> ti.tripId)
//				.collect(Collectors.toSet());
//		List<Trip> monitoredTrips = tripDao.findMonitoredTrips();
//		monitoredTrips.removeIf(t -> timedTripIds.contains(t.getId()));
//		if (monitoredTrips.isEmpty()) {
//			log.info("All required trip monitors are in place");
//		} else {
//			log.warn(String.format("There are %d trips without active monitoring, fixing now...", monitoredTrips.size()));
//			for (Trip trip : monitoredTrips) {
//				TripMonitorEvent event = tripStateToMonitorRevivalEvent.get(trip.getState());
//				if (event == null) {
//					log.warn(String.format("Trip %s state is %s, no suitable revival event found", trip.getId(), trip.getState()));
//					// First check what is really needed before switching off the monitor 
//					// trip.setMonitored(false);
//				} else {
//					try {
//						TripInfo ti = new TripInfo(event, trip.getId());
//						tripDao.detach(trip);
//						handleTimeout(ti);
//					} catch (BusinessException ex) {
//						log.error(String.join("\n\t", ExceptionUtil.unwindException(ex)));
//					} catch (Exception ex) {
//						log.error(String.format("Error reviving trip monitor: %s", ex.toString()));
//					}
//				}
//			}
//		}
	}
	
}
