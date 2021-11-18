package eu.netmobiel.overseer.processor;

import javax.annotation.Resource;
import javax.annotation.security.RunAs;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.communicator.model.Conversation;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.model.UserRole;
import eu.netmobiel.communicator.service.PublisherService;
import eu.netmobiel.planner.event.TripStateUpdatedEvent;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripState;
import eu.netmobiel.rideshare.event.RideStateUpdatedEvent;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.RideState;

/**
 * Stateless bean for the monitoring of trips and rides.
 *  
 * The Overseer is intended as a kind of message handler. In case of distributed modules, the Overseer must use 
 * real messaging (e.g. Apache Kafka, ActiveMQ) , instead or in addition to CDI events.  
 * 
 * @author Jaap Reitsma
 *
 */
		
@Stateless
@RunAs("system") 
public class TripProgressProcessor {
    @Inject
    private PublisherService publisherService;

    @Resource
    private SessionContext context;

    @Inject
    private TextHelper textHelper;
    
    public void onTripStateChange(@Observes(during = TransactionPhase.IN_PROGRESS) TripStateUpdatedEvent event) 
    		throws BusinessException {
    	switch (event.getTrip().getState()) {
    	case PLANNING:
    		break;
    	case BOOKING:
    		break;
    	case SCHEDULED:
    		break;
    	case DEPARTING:
    		if (event.getPreviousState() == TripState.SCHEDULED) {
    			informTravellerOnDeparture(event.getTrip());
    		}
    		break;
    	case IN_TRANSIT:
    		break;
    	case ARRIVING:
    		break;
    	case VALIDATING:
    		if (event.getPreviousState() == TripState.ARRIVING) {
    			informTravellerOnReview(event.getTrip());
    		} else if (event.getPreviousState() == TripState.VALIDATING) {
    			remindTravellerOnReview(event.getTrip());
    		}
    		break;
    	case COMPLETED:
    		break;
    	case CANCELLED:
    		break;
    	default:
    		break;
    	}
//    	try {
//    		
//		} catch (ApplicationException e) {
//			logger.error("Unable to obtain nearby driver profiles: " + e.toString());
//		}
    }

    public void onRideStateChange(@Observes(during = TransactionPhase.IN_PROGRESS) RideStateUpdatedEvent event) 
    		throws BusinessException {
    	Ride ride = event.getRide();
    	switch (ride.getState()) {
    	case SCHEDULED:
    		break;
    	case DEPARTING:
    		if (event.getPreviousState() == RideState.SCHEDULED && ride.hasConfirmedBooking()) {
    			informDriverOnDeparture(event.getRide());
    		}
    		break;
    	case IN_TRANSIT:
    		break;
    	case ARRIVING:
    		break;
    	case VALIDATING:
    		if (ride.hasConfirmedBooking()) {
	    		if (event.getPreviousState() == RideState.ARRIVING) {
	    			informDriverOnReview(ride);
	    		} else if (event.getPreviousState() == RideState.VALIDATING) {
	    			remindDriverOnReview(ride);
	    		}
    		}
    		break;
    	case COMPLETED:
    		break;
    	case CANCELLED:
    		break;
    	default:
    		break;
    	}
//    	try {
//    		
//		} catch (ApplicationException e) {
//			logger.error("Unable to obtain nearby driver profiles: " + e.toString());
//		}
    }

	private void informPassengerTripProgress(Trip trip, String text, String delegateText) throws BusinessException {
		Conversation passengerConv = publisherService.lookupOrCreateConversation(trip.getTraveller(), 
				UserRole.PASSENGER, trip.getTripRef(), textHelper.createPassengerTripTopic(trip), true);
		Message msg = new Message();
		msg.setContext(trip.getTripRef());
		msg.setDeliveryMode(DeliveryMode.ALL);
		msg.addRecipient(passengerConv, trip.getTripRef());
		msg.setBody(text);
		publisherService.publish(null, msg);
		// Inform the delegates, if any
		publisherService.informDelegates(trip.getTraveller(), 
				delegateText, 
				DeliveryMode.ALL);
	}

	private void informTravellerOnDeparture(Trip trip) throws BusinessException {
		informPassengerTripProgress(trip, textHelper.createTripDepartureText(trip), textHelper.informDelegateTripDepartureText(trip));
	}

    
	private void informTravellerOnReview(Trip trip) throws BusinessException {
		informPassengerTripProgress(trip, textHelper.createTripReviewRequestText(trip), textHelper.informDelegateTripReviewRequestText(trip));
	}

	private void remindTravellerOnReview(Trip trip) throws BusinessException {
		informPassengerTripProgress(trip, textHelper.createTripReviewRequestReminderText(trip), textHelper.informDelegateTripReviewReminderText(trip));
	}
	
	private static Booking getConfirmedBooking(Ride ride) throws BusinessException {
		return ride.getConfirmedBooking().orElseThrow(() -> new IllegalStateException("Expected a confirmed booking for ride:" + ride.getId()));
	}
	
	private void informDriverRideProgress(Ride ride, Booking b, String text) throws BusinessException {
		Conversation driverConv = publisherService.lookupOrCreateConversation(ride.getDriver(), 
				UserRole.DRIVER, ride.getUrn(), textHelper.createRideTopic(ride), true);
		Message msg = new Message();
		msg.setContext(ride.getUrn());
		msg.setDeliveryMode(DeliveryMode.ALL);
		msg.addRecipient(driverConv, b.getUrn());
		msg.setBody(text);
		publisherService.publish(null, msg);
	}	

	private void informDriverOnDeparture(Ride ride) throws BusinessException {
		Booking b = getConfirmedBooking(ride);
		informDriverRideProgress(ride, b, textHelper.createRideDepartureText(ride, b));
	}

    private void informDriverOnReview(Ride ride) throws BusinessException {
		Booking b = getConfirmedBooking(ride);
		informDriverRideProgress(ride, b, textHelper.createRideReviewRequestText(ride, b));
	}

    private void remindDriverOnReview(Ride ride) throws BusinessException {
		Booking b = getConfirmedBooking(ride);
		informDriverRideProgress(ride, b, textHelper.createRideReviewRequestReminderText(ride, b));
	}
	
}
