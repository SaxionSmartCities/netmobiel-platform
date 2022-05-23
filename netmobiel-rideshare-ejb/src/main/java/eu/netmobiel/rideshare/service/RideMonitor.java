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

import eu.netmobiel.commons.annotation.Removed;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.repository.ClockDao;
import eu.netmobiel.commons.util.EventFireWrapper;
import eu.netmobiel.commons.util.ExceptionUtil;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.ValidEjbTimer;
import eu.netmobiel.rideshare.event.RideEvent;
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
	 * The maximum number of reminders to sent during validation.
	 */
	private static final int MAX_REMINDERS = 2;
	
	/**
	 * The duration of the validation interval (before sending the next reminder).
	 */
	private static final Duration VALIDATION_INTERVAL = Duration.ofDays(2);
//	private static final Duration VALIDATION_INTERVAL = Duration.ofMinutes(2);
	/**
	 * The duration of the pre-departing period in which the monitoring should start or have started.
	 */
	private static final Duration PRE_DEPARTING_PERIOD = Ride.DEPARTING_PERIOD.plus(Duration.ofHours(2));

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
     * Reset the state to arriving as if the ride was just finished. Update the state of the ride. 
     * @param ride
     * @throws BusinessException
     */
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void restartValidation(Ride ride) throws BusinessException {
    	if (ride.getState().isPostTravelState() ) {
    		// The fare was uncancelled or refunded, any way, the ride needs validation again
    		// Force a restart from the arriving state
    		cancelRideTimers(ride);
    		ride.setState(RideState.ARRIVING);
    		updateRideStateMachine(ride);
    	}
    }

    public static class RideInfo implements Serializable {
		private static final long serialVersionUID = -2715209888482006490L;
    	public Long rideId;
    	public RideInfo(Long aRideId) {
    		this.rideId = aRideId;
    	}
    	
		@Override
		public String toString() {
			return String.format("RideInfo [%s]", rideId);
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
		RideMonitorEvent event = RideMonitorEvent.TIME_TO_CHECK;
		Instant nextTimeout = null; 
		switch (newState) {
		case SCHEDULED:
//			log.info("Departure time: " + ride.getDepartureTime());
//			log.info("Departure time minus period: " + ride.getDepartureTime().minus(PRE_DEPARTING_PERIOD));
//			log.info("Now: " + now);
//			log.info("isAfter(now): " + ride.getDepartureTime().minus(PRE_DEPARTING_PERIOD).isAfter(now));
			if (now.plus(PRE_DEPARTING_PERIOD).isAfter(ride.getDepartureTime())) {
				nextTimeout = ride.getDepartureTime().minus(Ride.DEPARTING_PERIOD);
			}
			break;
		case DEPARTING:
			nextTimeout =  ride.getDepartureTime();
			break;
		case IN_TRANSIT:
			nextTimeout = ride.getArrivalTime();
			break;
		case ARRIVING:
			Duration nextCheckDuration = ride.isPaymentDue() ? VALIDATION_DELAY : Ride.ARRIVING_PERIOD;   
			nextTimeout =  ride.getArrivalTime().plus(nextCheckDuration);
			break;
		case VALIDATING:
			// In this state we wait for: 
			// 1. the driver to confirm the passenger's presence on the ride, set a reminder message timer  
			// 2. the payment decision by the Overseer
			// Note: the ride monitor does not handle expiration of the validation time, the trip monitor takes care  of that.
			//        the validation reminder could also be sent by the trip monitor. Decide (and change) later. 
			if (oldState != RideState.VALIDATING) {
				// Probably from arriving or from completed. Start or restart the validation, from now.
				ride.setValidationExpirationTime(now.plus(VALIDATION_INTERVAL.multipliedBy(1L + MAX_REMINDERS)));
				ride.setValidationReminderTime(now.plus(VALIDATION_INTERVAL));
				nextTimeout = ride.getValidationReminderTime(); 
			} else {
				if (ride.isConfirmationPending() &&	now.isAfter(ride.getValidationReminderTime())) {
					// The ride timer only causes reminders, but no expiration. 
					ride.setValidationReminderTime(now.plus(VALIDATION_INTERVAL));
					nextTimeout = ride.getValidationReminderTime();
					event = RideMonitorEvent.TIME_TO_SEND_VALIDATION_REMINDER;
				}
			}
			break;
		case COMPLETED:
       		cancelRideTimers(ride);
			break;
		case CANCELLED:
       		cancelRideTimers(ride);
			break;
		}
		// Inform the observers
		EventFireWrapper.fire(rideEvent, new RideEvent(ride, event, oldState, newState));
		if (nextTimeout != null) {
			setupTimer(ride, nextTimeout);
		}
	}

	/**
	 * A timeout occurred. The timeou-out itself is not very significant, it is only a way of working to check whether 
	 * something should be done. The state machine determines what is actually to be done.
	 * Note that the timeout-out can be delayed in case of a system down time etc.
	 * To prevent unnecessary messages to the end-user, the chosen principle is to jump to the right state, instead of clicking
	 * to the next logical state (departing, in transit, arriving etc). 
	 * @param timer
	 */
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
			updateRideStateMachine(ride);
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
    public void updateStateMachine(Long rideId) {
		try {
			Ride ride = getRide(rideId);
			updateRideStateMachine(ride);
		} catch (BusinessException ex) {
			log.error(String.join("\n\t", ExceptionUtil.unwindException(ex)));
			log.info("Rollback status after exception: " + sessionContext.getRollbackOnly()); 
		} catch (Exception ex) {
			log.error(String.format("Error updating ride state machine: %s", ex.toString()));
		}
    }

    /**
     * Updates the state machine asynchronously.
     * @param rideId
     */
    @Asynchronous
    public void updateStateMachineAsync(Long rideId) {
    	updateStateMachine(rideId);
    }

    @Schedule(info = "Collect due rides", hour = "*/1", minute = "0", second = "30", persistent = false /* non-critical job */)
	public void checkForDueRides() {
		// Get all rides that need monitoring and have a departure time within a certain window
		try {
			// Find all rides that depart before the indicated time (the plus is the ramp-up)
			//TODO check only rides that have no timer set yet
			List<Long> rideIds = rideDao.findRidesToMonitor(clockDao.now().plus(PRE_DEPARTING_PERIOD));
			for (Long rideId : rideIds) {
				sessionContext.getBusinessObject(this.getClass()).updateStateMachine(rideId);
			}
		} catch (Exception ex) {
			log.error(String.format("Error handling timeout: %s", ex.toString()));
		}
	}

	private void setupTimer(Ride ride, Instant expirationTime) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setupTimer %s %s", ride.getUrn(), expirationTime));
		}
		Collection<Timer> timers = getActiveTimers(ride);
		if (timers.size() > 1) {
			log.warn(String.format("Too many timers for Ride %s, cancelling all of them", ride.getId()));
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
			timerService.createTimer(Date.from(expirationTime), new RideInfo(ride.getId())); 
		}
	}

    private Collection<Timer> getActiveTimers(Ride ride) {
    	Collection<Timer> timers = timerService.getTimers();
    	// Remove the timers that are not used with RideInfo
    	timers.removeIf(tm -> !(tm.getInfo() instanceof RideInfo && ((RideInfo)tm.getInfo()).rideId.equals(ride.getId())));
//    	if (log.isDebugEnabled()) {
//    		log.debug(String.format("getActiveTimers: %d timers in total active for this EJB", timers.size()));
//    	}
    	if (timers.size() > 1) {
    		log.warn(String.format("getActiveTimers: %d timers active for ride %s!", timers.size(), ride.getId()));
    	}
    	return timers;
    }
    
    private void cancelTimer(Timer tm) {
		try {
			tm.cancel();
		} catch (Exception ex) {
			log.error(String.format("Error canceling timer: %s", ex.toString()));
		}
    }

    private void cancelRideTimers(Ride ride) {
    	// Find all timers related to this ride and cancel them
		getActiveTimers(ride).forEach(tm -> cancelTimer(tm));
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
    			cancelTimer(tm);
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
	 * Not used anymore, only for logging the active timers now.
	 */
	public void reviveRideMonitors() {
		listAllRideMonitorTimers();
//		checkForDueRides();
	}

}
