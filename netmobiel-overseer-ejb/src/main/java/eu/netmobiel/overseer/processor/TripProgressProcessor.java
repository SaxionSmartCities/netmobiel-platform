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
import eu.netmobiel.planner.event.TripEvent;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.rideshare.event.RideEvent;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.Ride;

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
    
    public void onTripEvent(@Observes(during = TransactionPhase.IN_PROGRESS) TripEvent event) 
    		throws BusinessException {
    	Trip trip = event.getTrip();
    	switch (event.getEvent()) {
    	case TIME_TO_CHECK:
    		break;
    	case TIME_TO_PREPARE:
   			informTravellerOnDeparture(trip);
    		break;
    	case TIME_TO_DEPART:
    		break;
    	case TIME_TO_ARRIVE:
    		break;
    	case TIME_TO_VALIDATE:
			informTravellerOnReview(trip);
    		break;
		case TIME_TO_VALIDATE_REMINDER:
			remindTravellerOnReview(trip);
    		break;
    	case TIME_TO_COMPLETE:
    		break;
    	default:
    		break;
    	}
    }

    public void onRideEvent(@Observes(during = TransactionPhase.IN_PROGRESS) RideEvent event) 
    		throws BusinessException {
    	Ride ride = event.getRide();
    	switch (event.getEvent()) {
    	case TIME_TO_CHECK:
    		break;
    	case TIME_TO_PREPARE:
    		if (ride.hasConfirmedBooking()) {
    			informDriverOnDeparture(event.getRide());
    		}
    		break;
    	case TIME_TO_DEPART:
    		break;
    	case TIME_TO_ARRIVE:
    		break;
    	case TIME_TO_VALIDATE:
   			informDriverOnReview(ride);
    		break;
		case TIME_TO_VALIDATE_REMINDER:
			remindDriverOnReview(ride);
    		break;
    	case TIME_TO_COMPLETE:
    		break;
    	default:
    		break;
    	}
    }

	private void informPassengerTripProgress(Trip trip, String text, String delegateText) throws BusinessException {
		Conversation passengerConv = publisherService.lookupOrCreateConversation(trip.getTraveller(), 
				UserRole.PASSENGER, trip.getTripRef(), textHelper.createPassengerTripTopic(trip), true);
		Message msg = new Message();
		msg.setContext(trip.getTripRef());
		msg.setDeliveryMode(DeliveryMode.ALL);
		msg.addRecipient(passengerConv, trip.getTripRef());
		msg.setBody(text);
		publisherService.publish(msg);
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
		publisherService.publish(msg);
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
