package eu.netmobiel.overseer.processor;

import javax.annotation.Resource;
import javax.annotation.security.RunAs;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.NetMobielModule;
import eu.netmobiel.commons.annotation.Created;
import eu.netmobiel.commons.annotation.Removed;
import eu.netmobiel.commons.event.BookingCancelledFromProviderEvent;
import eu.netmobiel.commons.event.TripConfirmedByProviderEvent;
import eu.netmobiel.commons.event.TripUnconfirmedByProviderEvent;
import eu.netmobiel.commons.event.TripValidationEvent;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.communicator.model.Conversation;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.model.UserRole;
import eu.netmobiel.communicator.service.PublisherService;
import eu.netmobiel.planner.event.BookingCancelledEvent;
import eu.netmobiel.planner.event.BookingConfirmedEvent;
import eu.netmobiel.planner.event.BookingProposalRejectedEvent;
import eu.netmobiel.planner.event.BookingRequestedEvent;
import eu.netmobiel.planner.event.TripConfirmedEvent;
import eu.netmobiel.planner.event.TripUnconfirmedEvent;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.model.TripState;
import eu.netmobiel.planner.service.TripManager;
import eu.netmobiel.planner.service.TripPlanManager;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.BookingState;
import eu.netmobiel.rideshare.service.BookingManager;

/**
 * Stateless bean for the management of the high-level booking process, involving multiple modules.
 *  
 * The Overseer is intended as a kind of message handler. In case of distributed modules, the Overseer must use 
 * real messaging (e.g. Apache Kafka, ActiveMQ) , instead or in addition to CDI events.  
 * 
 * @author Jaap Reitsma
 *
 */
		
@Stateless
@Logging
@RunAs("system") 
public class BookingProcessor {
	
    @Inject
    private PublisherService publisherService;

    @Inject
    private BookingManager bookingManager;

    @Inject
    private TripManager tripManager;

    @Inject
    private TripPlanManager tripPlanManager;

    @Resource
    private SessionContext context;

    @Inject
    private Logger logger;
    
    @Inject
    private TextHelper textHelper;
    
    @Inject
    private PaymentProcessor paymentProcessor;

	/**********************************************/
	/**********   CALLBACK METHODS  ***************/
	/**********************************************/
	
    /**
     * Handler for the case when a traveller requests a booking of a ride. 
     * Autobooking is assumed, so no interaction with the driver is required. Because autoconfirm is enabled, the fare is also reserved
     * on expense of the passenger. 
     *  
     * @param event the booking request
     * @throws BusinessException 
     */
    public void onBookingRequested(@Observes(during = TransactionPhase.IN_PROGRESS) BookingRequestedEvent event) 
    		throws BusinessException {
    	Trip trip = event.getTrip();
    	Leg leg = event.getLeg();
    	if (!NetMobielModule.RIDESHARE.getCode().equals(UrnHelper.getService(leg.getTripId()))) {
    		logger.error("Booking is not suported for this service: " + leg.getTripId());
    	}
    	// It must be a Rideshare service booking
    	Booking b = new Booking();
		b.setArrivalTime(leg.getEndTime());
		b.setDepartureTime(leg.getStartTime());
		b.setDropOff(leg.getTo().getLocation());
		b.setPickup(leg.getFrom().getLocation());
		b.setNrSeats(trip.getNrSeats());
		b.setPassengerTripRef(trip.getTripRef());
		// Copy the fare amount
		b.setFareInCredits(event.getLeg().getFareInCredits());
		String bookingRef = bookingManager.createBooking(leg.getTripId(), trip.getTraveller(), b);
		// Check whether booking auto confirm is enabled or that we have long conversation
		Booking bdb = bookingManager.getBooking(UrnHelper.getId(Booking.URN_PREFIX, bookingRef));
		// Assign the booking reference to the trip leg
		boolean autoConfirmed = bdb.getState() == BookingState.CONFIRMED;
		if (! autoConfirmed) {
			logger.warn("Expecting booking AutoConfirm! Other situations are not handled!");
		}
		tripManager.assignBookingReference(trip.getTripRef(), leg.getTripId(), bookingRef, autoConfirmed);
		paymentProcessor.reserveFare(event.getTrip(), event.getLeg());
		// Inform passenger on booking. This will also start the conversation of the passenger for this ride! 
		Conversation passengerConv = publisherService.lookupOrCreateConversation(trip.getTraveller(), 
				UserRole.PASSENGER, trip.getTripRef(), textHelper.createPassengerTripTopic(trip), true);
    	Message msg = new Message();
		msg.setContext(bookingRef);
		msg.setDeliveryMode(DeliveryMode.ALL);
		msg.addRecipient(passengerConv, trip.getTripRef());
		msg.setBody(textHelper.createBookingTextForPassenger(b));
		publisherService.publish(msg);
    }
    
    private void informDriverOnBookingChangeConversation(Booking booking, String text) throws BusinessException {
		// Inform driver on new booking
    	// Create the ride context, it might not yet exist.
		// Add the booking context to the ride context
		Conversation driverConv = publisherService.lookupOrCreateConversation(booking.getRide().getDriver(), 
				UserRole.DRIVER, booking.getRide().getUrn(), textHelper.createRideTopic(booking.getRide()), true);
		publisherService.addConversationContext(driverConv, booking.getUrn(), textHelper.createRideTopic(booking.getRide()), true);
    	Message msg = new Message();
		msg.setContext(booking.getRide().getUrn());
		msg.setDeliveryMode(DeliveryMode.ALL);
		msg.addRecipient(driverConv, booking.getUrn());
		msg.setBody(text);
		publisherService.publish(msg);
    }

    public void onBookingCreated(@Observes(during = TransactionPhase.IN_PROGRESS) @Created Booking booking) throws BusinessException {
		// Inform driver on new booking
    	informDriverOnBookingChangeConversation(booking, textHelper.createBookingCreatedTextForDriver(booking));
	}

	public void onBookingRemoved(@Observes(during = TransactionPhase.IN_PROGRESS) @Removed Booking booking) throws BusinessException {
		// Inform driver about removal of a booking
    	informDriverOnBookingChangeConversation(booking, textHelper.createBookingCancelledByPassengerText(booking));

	}

	/**
     * Handles the case where a traveller confirms a proposed booking of a provider. The provider gets the trip reference assigned.
     * The fare is debited for the traveller and credited to the reservation account.  
     * @param event the confirmed event
     * @throws BusinessException 
     */
    public void onBookingConfirmed(@Observes(during = TransactionPhase.IN_PROGRESS) BookingConfirmedEvent event) throws BusinessException {
		// Add a trip reference to the booking.
    	Long bid = UrnHelper.getId(Booking.URN_PREFIX, event.getLeg().getBookingId());
		bookingManager.confirmBooking(bid, event.getTrip().getTripRef(), event.getLeg().getFareInCredits());
		paymentProcessor.reserveFare(event.getTrip(), event.getLeg());
    	event.getLeg().setBookingConfirmed(true);
    }

    /**
     * Signals the removal of a booking through the Netmobiel Planner API during a shout-out.
     * The state must be in PROPOSAL state.
     * 
     * @param event
     * @throws BusinessException 
     */
    public void onBookingProposalRejected(@Observes(during = TransactionPhase.IN_PROGRESS) BookingProposalRejectedEvent event) 
    		throws BusinessException {
		if (event.getLeg().getState() != TripState.PLANNING) {
			throw new IllegalStateException("Leg is not in planning state: " + event.getLeg().getId() + " " + event.getLeg().getState());
		}
    	logger.info(String.format("Booking proposal %s cancelled (from Netmobiel) by passenger because '%s'", 
    			event.getLeg().getBookingId(), event.getCancelReason() != null ? event.getCancelReason() : "---"));
		// The booking is cancelled through the TripManager or TripPlanManager
		bookingManager.removeBooking(event.getLeg().getBookingId(), event.getCancelReason(), false, false);
    }

    /**
     * Signals the removal of a booking through the Netmobiel Planner API.
     * 
     * @param event
     * @throws BusinessException 
     */
    public void onBookingCancelled(@Observes(during = TransactionPhase.IN_PROGRESS) BookingCancelledEvent event) 
    		throws BusinessException {
		if (event.getLeg().getState() == TripState.CANCELLED) {
			throw new IllegalStateException("Leg already cancelled: " + event.getLeg().getId());
		}
    	logger.info(String.format("Booking %s cancelled (from Netmobiel) by passenger because '%s'", 
    			event.getLeg().getBookingId(), event.getCancelReason() != null ? event.getCancelReason() : "---"));
		// The booking is cancelled through the TripManager or TripPlanManager
		if (event.getLeg().hasFareInCredits()) {
			Booking b = bookingManager.getShallowBooking(event.getLeg().getBookingId());
			paymentProcessor.cancelFare(event.getTrip(), event.getLeg(), b);
		}
		bookingManager.removeBooking(event.getLeg().getBookingId(), event.getCancelReason(), false, false);
    }
    
    /**
     * Signals the removal of a booking through the provider API. 
     * 
     * @param event
     * @throws BusinessException 
     */
    public void onBookingCancelledFromProvider(@Observes(during = TransactionPhase.IN_PROGRESS) BookingCancelledFromProviderEvent event) 
    		throws BusinessException {
    	logger.info(String.format("Booking %s cancelled from Transport Provider by %s because '%s'", 
    			event.getBookingRef(),
    			event.isCancelledByDriver() ? "Driver" : "Passenger",
    			event.getCancelReason() != null ? event.getCancelReason() : "---"));
		// The booking is cancelled by transport provider
		Booking b = bookingManager.getBooking(UrnHelper.getId(Booking.URN_PREFIX, event.getBookingRef()));
		Trip trip = null;
		if (b.getPassengerTripRef() != null) {
			// The call in in the trip manager checks the state of the leg.
			Leg leg = tripManager.cancelBooking(b.getPassengerTripRef(), event.getBookingRef(), event.getCancelReason(), event.isCancelledByDriver());
			trip = tripManager.getTrip(UrnHelper.getId(Trip.URN_PREFIX, b.getPassengerTripRef()));
			if (leg.hasFareInCredits()) {
				// cancel the reservation
				paymentProcessor.cancelFare(trip, leg, b);
			}
		} else if (b.getPassengerTripPlanRef() != null) { 
			// The booking is only a proposal, no reservation done yet, only a proposal for a shout-out
			tripPlanManager.cancelBooking(b.getPassengerTripPlanRef(), event.getBookingRef());
		} else {
			logger.error(String.format("Booking %s has neither trip ref nor trip plan ref", event.getBookingRef()));
		}

		if (event.isCancelledByDriver()) {
			// Notify the passenger
			// Find the conversation of the passenger
			Conversation passengerConv = null;
			String passengerContext = null;
			String passengerTopic = null;
			if (b.getPassengerTripRef() != null) {
				passengerContext = b.getPassengerTripRef();
				passengerTopic = textHelper.createPassengerTripTopic(trip);
			} else if (b.getPassengerTripPlanRef() != null) {
				passengerContext = b.getPassengerTripPlanRef();
				TripPlan plan = tripPlanManager.getTripPlan(UrnHelper.getId(TripPlan.URN_PREFIX, b.getPassengerTripPlanRef()));
				passengerTopic = textHelper.createPassengerShoutOutTopic(plan);
			} else {
				logger.error("Booking has no reference to trip or tripplan: " + b.getUrn());
			}
			if (passengerContext != null) {
				passengerConv = publisherService.lookupOrCreateConversation(event.getTraveller(), 
						UserRole.PASSENGER, passengerContext, passengerTopic, true); 
				Message msg = new Message();
				msg.setContext(event.getBookingRef());
				msg.setBody(textHelper.createDriverCancelledBookingText(b));
				msg.setDeliveryMode(DeliveryMode.ALL);
				msg.addRecipient(passengerConv, passengerContext);
				publisherService.publish(msg);
				// Inform the delegates, if any. They receive limited information only. The delegate can switch to the delegator view and see the normal messages.
				publisherService.informDelegates(event.getTraveller(), 
						textHelper.informDelegateCancelledBookingText(b), DeliveryMode.ALL);
			}
		} else {
			// Notification of the driver is done by transport provider
		}
    }

    /** 
     * Handle the event where the provider confirms (or denies) the trip. 
     * @param event
     * @throws BusinessException 
     */
    public void onProviderConfirmation(@Observes(during = TransactionPhase.IN_PROGRESS) TripConfirmedByProviderEvent event) 
    		throws BusinessException {
  		// The trip manager checks the state for reasonable values
		tripManager.afterConfirmTripByTransportProvider(event.getTravellerTripRef(), event.getBookingRef(), 
				event.getConfirmationByTransportProvider(), event.getConfirmationReason(), true);
    }

    /** 
     * Handle the event where the passenger confirms (or denies) the trip. 
     * @param event
     * @throws BusinessException 
     */
    public void onPassengerConfirmation(@Observes(during = TransactionPhase.IN_PROGRESS) TripConfirmedEvent event) 
    		throws BusinessException {
  		// The booking manager checks the state for reasonable values
    	Leg leg = event.getLeg();
		bookingManager.confirmTravellingByPassenger(leg.getBookingId(), leg.getConfirmed(), leg.getConfirmationReason()); 
    }

    private void resetValidationAtBothSides(Trip trip) throws BusinessException {
   		// reset the validation (through the booking manager). This is an asynchronous call.
    	// Should be only one leg.
    	for (Leg leg : trip.getItinerary().findLegsToConfirm()) {
			Booking b = bookingManager.getShallowBooking(leg.getBookingId());
    		bookingManager.resetValidation(b);
		} 
		tripManager.resetValidation(trip);
    }

    /** 
     * Handle the event where the traveller unconfirms the trip. 
     * This must be done in a single transaction. 
     * 1. Roll back the cancelled fare: Undo payment status at trip manager and ride manager.
     * 2. Reset the booking validation.
     * @param event the trip unconfirm event.
     * @throws BusinessException 
     */
    public void onTripUnconfirmation(@Observes(during = TransactionPhase.IN_PROGRESS) TripUnconfirmedEvent event) 
    		throws BusinessException {
    	Trip trip = event.getTrip();
    	paymentProcessor.revokeNegativeTripConfirmation(trip);
    	resetValidationAtBothSides(trip);
    }

    /** 
     * Handle the event where the provider unconfirms the trip. 
     * This must be done in a single transaction. 
     * 1. Roll back the payed fare: Undo payment status at trip manager and ride manager.
     * 2. Reset the trip validation.
     * @param event
     * @throws BusinessException 
     */
    public void onProviderUnconfirmation(@Observes(during = TransactionPhase.IN_PROGRESS) TripUnconfirmedByProviderEvent event) 
    		throws BusinessException {
		Trip trip = tripManager.getTrip(UrnHelper.getId(Trip.URN_PREFIX, event.getTravellerTripRef()));
    	paymentProcessor.revokePositiveTripConfirmation(trip);
    	resetValidationAtBothSides(trip);
    }

    /**
     * Listener for evaluating the trip. Only evaluate after a successful transaction, otherwise it has no use.
     * @param event
     * @throws BusinessException
     */
    public void onTripValidation(@Observes(during = TransactionPhase.AFTER_SUCCESS) TripValidationEvent event) throws BusinessException {
    	paymentProcessor.evaluateTripAfterConfirmation(event.getTripId(), event.isFinalOrdeal());
    }
    
}
