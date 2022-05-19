package eu.netmobiel.overseer.processor;

import javax.annotation.Resource;
import javax.annotation.security.RunAs;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.model.UserRole;
import eu.netmobiel.communicator.service.PublisherService;
import eu.netmobiel.planner.event.TripEvent;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripState;
import eu.netmobiel.rideshare.event.RideEvent;
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
    
    @Inject
    private DelegationProcessor delegationProcessor; 

    @Resource
    private SessionContext context;

    @Inject
    private TextHelper textHelper;
    
    public void onTripEvent(@Observes(during = TransactionPhase.IN_PROGRESS) TripEvent event) 
    		throws BusinessException {
    	Trip trip = event.getTrip();
    	switch (event.getEvent()) {
    	case TIME_TO_CHECK:
    		if (event.isTransitionTo(TripState.DEPARTING)) {
       			informTravellerOnDeparture(trip);
    		} else if (event.isTransitionTo(TripState.VALIDATING)) {
       			informTravellerOnReview(trip);
    		}
    		break;
		case TIME_TO_SEND_VALIDATION_REMINDER:
			if (trip.getItinerary().isPassengerConfirmationPending()) {
				remindTravellerOnReview(trip);
			}
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
    		if (event.isTransitionTo(RideState.DEPARTING)) {
        		if (ride.hasConfirmedBooking()) {
        			informDriverOnDeparture(event.getRide());
        		}
    		} else if (event.isTransitionTo(RideState.VALIDATING)) {
       			informDriverOnReview(ride);
    		}
    		break;
		case TIME_TO_SEND_VALIDATION_REMINDER:
			if (ride.isConfirmationPending()) {
				remindDriverOnReview(ride);
			}
    		break;
    	default:
    		break;
    	}
    }

	private void informPassengerTripProgress(Trip trip, String text, String delegateText) throws BusinessException {
    	Message msg = Message.create()
    			.withBody(text)
    			.withContext(trip.getTripRef())
    			.addEnvelope()
	    			.withRecipient(trip.getTraveller())
	    			.withConversationContext(trip.getTripRef())
	    			.withUserRole(UserRole.PASSENGER)
	    			.withTopic(textHelper.createPassengerTripTopic(trip))
	    			.buildConversation()
    			.buildMessage();
		publisherService.publish(msg);
		// Inform the delegates, if any
		delegationProcessor.informDelegates(trip.getTraveller(), 
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
	
	private void informDriverBookedRideProgress(Ride ride, Booking b, String text) throws BusinessException {
		// Conversation context is the ride
		// Message context is the booking (without booking this message was not sent)
		// Recipient's context is the ride
    	Message msg = Message.create()
    			.withBody(text)
    			.withContext(b.getUrn())
    			.addEnvelope(ride.getUrn())
	    			.withRecipient(ride.getDriver())
	    			.withConversationContext(ride.getUrn())
	    			.withUserRole(UserRole.DRIVER)
	    			.withTopic(textHelper.createRideTopic(ride))
	    			.buildConversation()
    			.buildMessage();
		publisherService.publish(msg);
	}	

	private void informDriverOnDeparture(Ride ride) throws BusinessException {
		Booking b = getConfirmedBooking(ride);
		informDriverBookedRideProgress(ride, b, textHelper.createRideDepartureText(ride, b));
	}

    private void informDriverOnReview(Ride ride) throws BusinessException {
		Booking b = getConfirmedBooking(ride);
		informDriverBookedRideProgress(ride, b, textHelper.createRideReviewRequestText(ride, b));
	}

    private void remindDriverOnReview(Ride ride) throws BusinessException {
		Booking b = getConfirmedBooking(ride);
		informDriverBookedRideProgress(ride, b, textHelper.createRideReviewRequestReminderText(ride, b));
	}
	
}
