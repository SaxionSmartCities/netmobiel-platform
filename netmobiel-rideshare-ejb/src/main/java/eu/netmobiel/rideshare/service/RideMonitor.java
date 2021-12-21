package eu.netmobiel.rideshare.service;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
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

import eu.netmobiel.commons.annotation.Removed;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.repository.ClockDao;
import eu.netmobiel.commons.util.EventFireWrapper;
import eu.netmobiel.commons.util.ExceptionUtil;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.ValidEjbTimer;
import eu.netmobiel.rideshare.event.RideEvent;
import eu.netmobiel.rideshare.event.RideStateUpdatedEvent;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideMonitorEvent;
import eu.netmobiel.rideshare.model.RideState;
import eu.netmobiel.rideshare.repository.RideDao;

/**
 * The monitor of the rides. This EJB is responsible for maintaining the proper state of the rides.
 * It will observe relevant events and will update the state of ride involved.
 * The state machine is built such to re-establish itself after startup: The state of the ride is calculated by 
 * looking at individual attributes of each ride. Events will only indirect cause transitions. 
 * The objective is to have a robust state machine that does not depend (only) on the occurrence of
 * a single event that might be missed because of some reason. Also, a restart of the application should 
 * start a fresh situation.   
 *
 * To prevent nasty recursions, an observer method will not directly call the update state machine method, but instead
 * set a timer that immediately expires. 
 * 
 * @author Jaap Reitsma
 *
 */
@Stateless
@Logging
public class RideMonitor {
	/**
	 * The delay before sending a invitation for a confirmation.
	 */
	private static final Duration VALIDATION_DELAY = Duration.ofMinutes(15);
	/**
	 * The duration of the validation interval (before sending the next reminder).
	 */
//	private static final Duration VALIDATION_INTERVAL = Duration.ofDays(2);
	private static final Duration VALIDATION_INTERVAL = Duration.ofMinutes(10);
	/**
	 * The duration of the pre-departing period in which the monitoring should start or have started.
	 */
	private static final Duration PRE_DEPARTING_PERIOD = Ride.DEPARTING_PERIOD.plus(Duration.ofHours(2));

	/**
	 * The maximum number of reminders to sent during validation.
	 */
	private static final int MAX_REMINDERS = 2;
	
	@Inject
    private Logger log;

    @Resource
	private SessionContext sessionContext;

    @Inject
    private ClockDao clockDao;

    @Inject
    private RideDao rideDao;

    @Resource
    private SessionContext context;

    @Inject @Removed
    private Event<Ride> rideRemovedEvent;

    @Resource
    private TimerService timerService;

    @Inject
    private Event<RideEvent> rideEvent;
    @Inject
    private Event<RideStateUpdatedEvent> rideStateUpdatedEvent;

    private final static ValidEjbTimer validEjbTimer = new ValidEjbTimer();
    
    /**
     * Retrieves a ride with the booking details. 
     * @param id
     * @return
     * @throws NotFoundException
     */
    private Ride getRide(Long id) throws NotFoundException {
    	Ride ridedb = rideDao.loadGraph(id, Ride.RIDE_DRIVER_BOOKING_DETAILS_ENTITY_GRAPH)
    			.orElseThrow(() -> new NotFoundException("No such ride"));
    	return ridedb;
    }

    /**
     * Start a timer to check the state machine.
     * TODO: Does this timer interfere with the other timers? A timer event might get lost because of the deletion of a timer?
     * @param ride the ride to check
     */
    public void evaluateStateMachineDelayed(Ride ride) {
		setupTimer(ride, RideMonitorEvent.TIME_TO_CHECK, clockDao.now());
    }
    
    public void restartValidation(Ride ride) throws BusinessException {
    	if (ride.getState().isPostTravelState() ) {
    		// The fare was uncancelled or refunded, any way, the ride needs validation again
    		// Use the timer to decouple to assure no recursion occurs
    		ride.setState(RideState.ARRIVING);
    		setupTimer(ride, RideMonitorEvent.TIME_TO_VALIDATE, clockDao.now());
    	}
    }

    public static class RideInfo implements Serializable {
		private static final long serialVersionUID = -2715209888482006490L;
		public RideMonitorEvent event;
    	public Long rideId;
    	public RideInfo(RideMonitorEvent anEvent, Long aRideId) {
    		this.event = anEvent;
    		this.rideId = aRideId;
    	}
    	
		@Override
		public String toString() {
			return String.format("RideInfo [%s %s]", event, rideId);
		}
    }
    
    /**
     * Evaluate the ride state. NOTE: Only call methods that do not call an update of the state machine, i.e. no recursion!
     * @param ride the ride to evaluate.
     * @throws BusinessException
     */
    public void updateRideStateMachine(Ride ride) throws BusinessException {
    	Instant now = clockDao.now();
    	// Determine the new state(s)
    	RideState oldState = ride.getState();
		RideState newState = ride.nextState(now);
		ride.setState(newState);
		if (log.isDebugEnabled()) {
			log.debug(String.format("Ride SM %s --> %s: %s", oldState, newState, ride.toStringCompact()));
		}
    	if (newState.ordinal() < oldState.ordinal()) {
    		log.warn(String.format("Ride %s: State is set back from %s --> %s", ride.getId(), oldState, newState));
    	}
		switch (newState) {
		case SCHEDULED:
			if (ride.getDepartureTime().minus(PRE_DEPARTING_PERIOD).isAfter(now)) {
				setupTimer(ride, RideMonitorEvent.TIME_TO_PREPARE, 
						ride.getDepartureTime().minus(Ride.DEPARTING_PERIOD));
			} else {
				cancelRideTimer(ride);
			}
			break;
		case DEPARTING:
			setupTimer(ride, RideMonitorEvent.TIME_TO_DEPART, ride.getDepartureTime());
			break;
		case IN_TRANSIT:
			setupTimer(ride, RideMonitorEvent.TIME_TO_ARRIVE, ride.getArrivalTime());
			break;
		case ARRIVING:
			if (ride.isPaymentDue()) {
				setupTimer(ride, RideMonitorEvent.TIME_TO_VALIDATE, 
						ride.getArrivalTime().plus(VALIDATION_DELAY));
			} else {
				setupTimer(ride, RideMonitorEvent.TIME_TO_COMPLETE, ride.getArrivalTime().plus(Ride.ARRIVING_PERIOD));
			}
			break;
		case VALIDATING:
			int nrRemindersLeft = MAX_REMINDERS;
			if (oldState != RideState.VALIDATING) {
				// Probably from arriving or from completed. Start or restart the validation, from now.
				ride.setValidationExpirationTime(now.plus(VALIDATION_INTERVAL.multipliedBy(1 + MAX_REMINDERS)));
				ride.setValidationReminderTime(now.plus(VALIDATION_INTERVAL));
			} else {
		    	Duration d = Duration.between(clockDao.now(), ride.getValidationExpirationTime());
		    	if (d.isNegative()) {
		    		d = Duration.ZERO;		    		
		    	}
		    	nrRemindersLeft = Math.toIntExact(d.dividedBy(VALIDATION_INTERVAL));
		    	Duration upToNextTime = d.minus(VALIDATION_INTERVAL.multipliedBy(nrRemindersLeft)); 
		    	if (!upToNextTime.isZero()) {
					ride.setValidationReminderTime(now.plus(upToNextTime));
		    	}
			}
			setupTimer(ride, nrRemindersLeft > 0 ? RideMonitorEvent.TIME_TO_VALIDATE_REMINDER : RideMonitorEvent.TIME_TO_COMPLETE, 
					ride.getValidationReminderTime());
			break;
		case COMPLETED:
			// Just to be sure
       		cancelRideTimer(ride);
			break;
		case CANCELLED:
       		cancelRideTimer(ride);
			break;
		}
		// Inform the observers
    	EventFireWrapper.fire(rideStateUpdatedEvent, new RideStateUpdatedEvent(oldState, ride));
    }

	private void handleRideMonitorEvent(Ride ride, RideMonitorEvent event) throws BusinessException {
		if (log.isDebugEnabled()) {
			log.debug("Received ride event: " + event.toString());
		}
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
			if (ride.getState() == RideState.VALIDATING) {
				// OK time to finish up, that is up to the trip.
			}
			break;
		default:
			log.warn("Don't know how to handle event: " + event);
			break;
		}
		EventFireWrapper.fire(rideEvent, new RideEvent(event, ride));
		// The state machine is evaluated without regarding the event!
		// The timer is set by a state machine action
		updateRideStateMachine(ride);
	}

	@Timeout
	public void onTimeout(Timer timer) {
		try {
			if (! (timer.getInfo() instanceof RideInfo)) {
				log.error("Don't know how to handle timeout: " + timer.getInfo());
				return;
			}
			RideInfo rideInfo = (RideInfo) timer.getInfo();
			if (log.isDebugEnabled()) {
				// Note: non-persistent timer does not have a handle! Will throw exception if you try.
				log.debug(String.format("onTimeout: Received ride event: %s", rideInfo.toString()));
			}
			Ride ride = getRide(rideInfo.rideId);
			handleRideMonitorEvent(ride, rideInfo.event);
		} catch (BusinessException ex) {
			log.error(String.join("\n\t", ExceptionUtil.unwindException(ex)));
			log.info("Rollback status after exception: " + sessionContext.getRollbackOnly()); 
		} catch (NoSuchObjectLocalException ex) {
			log.error(String.format("Error handling timeout: %s", ex.toString()));
		}
	}

    /**
     * Update the ride state machine after an event. This call is intended to be called from a scheduled timer.
     * It has its own transaction to prevent one failure causing the failure of all. 
     * @param event the event that is is the trigger for the evaluation
     * @param rideId the ride involved
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void handleRideEvent(RideMonitorEvent event, Long rideId) {
		try {
			// Find all rides that depart before the indicated time (the plus is the ramp-up)
			Ride ride = getRide(rideId);
			handleRideMonitorEvent(ride, event);
		} catch (BusinessException ex) {
			log.error(String.join("\n\t", ExceptionUtil.unwindException(ex)));
			log.info("Rollback status after exception: " + sessionContext.getRollbackOnly()); 
		} catch (Exception ex) {
			log.error(String.format("Error updating ride state nachine: %s", ex.toString()));
		}
    }

    @Schedule(info = "Collect due rides", hour = "*/1", minute = "0", second = "0", persistent = false /* non-critical job */)
	public void checkForDueRides() {
		// Get all rides that need monitoring and have a departure time within a certain window
		try {
			// Find all rides that depart before the indicated time (the plus is the ramp-up)
			List<Long> rideIds = rideDao.findRidesToMonitor(clockDao.now().plus(PRE_DEPARTING_PERIOD));
			for (Long rideId : rideIds) {
				sessionContext.getBusinessObject(this.getClass()).handleRideEvent(RideMonitorEvent.TIME_TO_CHECK, rideId);
			}
		} catch (Exception ex) {
			log.error(String.format("Error handling timeout: %s", ex.toString()));
		}
	}

	private void setupTimer(Ride ride, RideMonitorEvent event, Instant expirationTime) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setupTimer %s %s %s", ride.getUrn(), event, expirationTime));
		}
		Collection<Timer> timers = getActiveTimers(ride);
		if (timers.size() > 1) {
			log.warn(String.format("Too many timers for Ride %s, cancelling all of them", ride.getId()));
			timers.forEach(tm -> tm.cancel());
		}
		Optional<Timer> tm = timers.stream().findFirst();
		if (tm.isPresent()) {
			Timer tmr = tm.get();
			if ((((RideInfo) tmr.getInfo()).event != event 
					|| !validEjbTimer.test(tmr)
					|| !tmr.getNextTimeout().toInstant().equals(expirationTime))) {
				log.debug("Canceling timer: " + tmr.getInfo());
				tmr.cancel();
				tm = Optional.empty();
			}
		}
		if (tm.isEmpty()) {
			timerService.createSingleActionTimer(Date.from(expirationTime), 
					new TimerConfig(new RideInfo(event, ride.getId()), false));
		}
	}

    private Collection<Timer> getActiveTimers(Ride ride) {
    	Collection<Timer> timers = timerService.getTimers();
    	timers.removeIf(tm -> !(tm.getInfo() instanceof RideInfo && ((RideInfo)tm.getInfo()).rideId.equals(ride.getId())));
//    	if (log.isDebugEnabled() && timers.size() < 2) {
//    		log.debug(String.format("getActiveTimers: %d timers in total active for this EJB", timers.size()));
//    	}
    	if (timers.size() >= 2) {
    		log.warn(String.format("getActiveTimers: %d timers active for ride %s!", timers.size(), ride.getId()));
    	}
    	return timers;
    }
    
	private void cancelRideTimer(Ride ride) {
    	// Find all timers related to this ride and cancel them
		getActiveTimers(ride).forEach(tm -> tm.cancel());
	}

	private List<RideInfo> listAllRideMonitorTimers() {
    	// Find all timers related to the ride manager
    	Collection<Timer> timers = timerService.getTimers();
    	Collection<Timer> invalidTimers = timers.stream()
        		.filter(validEjbTimer.negate())
        		.collect(Collectors.toList());
    	invalidTimers.stream()
    		.forEach(tm -> {
    			log.info(String.format("Cancelling invalid ride timer: %s", tm.getInfo()));
    			tm.cancel();
    		});
    	timers.removeAll(invalidTimers);
    	timers.removeIf(tm -> !(tm.getInfo() instanceof RideInfo));
		if (timers.isEmpty()) {
			log.info("NO active ride timers");
		} else {
			log.info("Active ride timers:\n\t" + String.join("\n\t", 
					timers.stream()
					.map(tm -> String.format("%s %s %d %s", tm.getInfo(), tm.getNextTimeout(), tm.getTimeRemaining(), tm.isPersistent()))
					.collect(Collectors.toList()))
			);
		}		
    	return timers.stream()
    			.map(tm -> (RideInfo) tm.getInfo())
    			.collect(Collectors.toList());
	}
	
	/**
	 * Revive the ride monitors that have been down after system restart.
	 * TODO: Some timer events might get missed:, like the reminder. 
	 * Either use a persistent one-shot timer, or check at startup whether some event might be missed.
	 */
	public void reviveRideMonitors() {
		listAllRideMonitorTimers();
		checkForDueRides();
	}

}
