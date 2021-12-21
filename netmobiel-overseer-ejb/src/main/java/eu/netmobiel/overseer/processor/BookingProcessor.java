package eu.netmobiel.overseer.processor;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.annotation.security.RunAs;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;

import eu.netmobiel.banker.service.LedgerService;
import eu.netmobiel.commons.NetMobielModule;
import eu.netmobiel.commons.annotation.Created;
import eu.netmobiel.commons.annotation.Removed;
import eu.netmobiel.commons.event.BookingCancelledFromProviderEvent;
import eu.netmobiel.commons.event.TripConfirmedByProviderEvent;
import eu.netmobiel.commons.event.TripUnconfirmedByProviderEvent;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.RemoveException;
import eu.netmobiel.commons.exception.SystemException;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.PaymentState;
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
import eu.netmobiel.planner.event.TripValidationExpiredEvent;
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
    private LedgerService ledgerService;

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
    
	@Resource(mappedName="java:jboss/mail/NetMobiel")
    private Session mailSession;	

    @Resource(lookup = "java:global/planner/disputeEmailAddress")
    private String disputeEmailAddress;

    @Resource(lookup = "java:global/planner/senderEmailAddress")
    private String senderEmailAddress;

    @Inject
    private TextHelper textHelper;
    
    private void reserveFare(Trip trip, Leg leg) throws BusinessException {
		if (leg.hasFareInCredits()) {
			// Reserve the fare
			String reservationId = ledgerService.reserve(trip.getTraveller(), 
					leg.getFareInCredits(), textHelper.createDescriptionText(leg), leg.getLegRef());
			tripManager.updateLegPaymentState(trip, leg, PaymentState.RESERVED, reservationId);
		}
    }

    private void cancelFare(Trip trip, Leg leg) throws BusinessException {
		if (leg.hasFareInCredits()) {
			// Release the fare
			if (leg.getPaymentState() == PaymentState.CANCELLED) {
				logger.warn(String.format("Cannot cancel fare of Leg %s in Trip %s, it is already cancelled!", leg.getId(), trip.getId()));
			} else {
				if (leg.getPaymentState() == PaymentState.PAID) {
					throw new IllegalStateException("Cannot cancel fare, payment state is invalid: " + 
							leg.getLegRef() + " " + leg.getPaymentState() + " " + leg.getPaymentId());
				}
				String releaseId = ledgerService.release(leg.getPaymentId());
				tripManager.updateLegPaymentState(trip, leg, PaymentState.CANCELLED, releaseId);
				// Settled without payment
			}
			bookingManager.informBookingFareSettled(leg.getTripId(), leg.getBookingId(), PaymentState.CANCELLED, null);
		}
    }

    /**
     * Reverses the earlier cancel of a fare by reserving the fare again on expense of the passenger.
     * @param trip
     * @param leg
     * @throws BusinessException
     */
    private void uncancelFare(Trip trip, Leg leg) throws BusinessException {
		if (leg.hasFareInCredits()) {
			if (leg.getPaymentState() != PaymentState.CANCELLED) {
				throw new IllegalStateException("Cannot uncancel fare, payment state is invalid: " + 
						leg.getLegRef() + " " + leg.getPaymentState() + " " + leg.getPaymentId());
			}
			// Reserve the fare again
			String reservationId = ledgerService.reserve(trip.getTraveller(), 
					leg.getFareInCredits(), textHelper.createDescriptionText(leg), leg.getLegRef());
			tripManager.updateLegPaymentState(trip, leg, PaymentState.RESERVED, reservationId);
			bookingManager.informBookingFareReset(leg.getTripId(), leg.getBookingId());
		}
    }

    private void payFare(Trip trip, Leg leg) throws BusinessException {
		if (leg.hasFareInCredits()) {
			if (leg.getPaymentState() != PaymentState.RESERVED) {
				throw new IllegalStateException("Cannot pay fare, payment state is invalid: " + 
						leg.getLegRef() + " " + leg.getPaymentState() + " " + leg.getPaymentId());
			}
			String chargeId = ledgerService.charge(resolveDriverId(leg), leg.getPaymentId(), leg.getFareInCredits());
			tripManager.updateLegPaymentState(trip, leg, PaymentState.PAID, chargeId);
			// Settled with payment
			bookingManager.informBookingFareSettled(leg.getTripId(), leg.getBookingId(), PaymentState.PAID, chargeId);
		}
    }

    /**
     * Reverses the earlier cancel of a fare by reserving the fare again on expense of the passenger.
     * @param trip
     * @param leg
     * @throws BusinessException
     */
    private void refundFare(Trip trip, Leg leg) throws BusinessException {
		if (leg.hasFareInCredits()) {
			if (leg.getPaymentState() != PaymentState.PAID) {
				throw new IllegalStateException("Cannot refund fare, payment state is invalid: " + 
						leg.getLegRef() + " " + leg.getPaymentState() + " " + leg.getPaymentId());
			}
			// Refund the fare credits
			String reservationId = ledgerService.reserve(resolveDriverId(leg), 
				leg.getFareInCredits(), textHelper.createDescriptionText(leg), leg.getLegRef());
			tripManager.updateLegPaymentState(trip, leg, PaymentState.RESERVED, reservationId);
			bookingManager.informBookingFareReset(leg.getTripId(), leg.getBookingId());
		}
    }

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
		reserveFare(event.getTrip(), event.getLeg());
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
		reserveFare(event.getTrip(), event.getLeg());
    }

    /**
     * Signals the removal of a booking through the NetMobiel Planner API during a shout-out.
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
    	logger.info(String.format("Booking proposal %s cancelled (from NetMobiel) by passenger because '%s'", 
    			event.getLeg().getBookingId(), event.getCancelReason() != null ? event.getCancelReason() : "---"));
		// The booking is cancelled through the TripManager or TripPlanManager
		bookingManager.removeBooking(event.getLeg().getBookingId(), event.getCancelReason(), false, false);
    }

    /**
     * Signals the removal of a booking through the NetMobiel Planner API.
     * 
     * @param event
     * @throws BusinessException 
     */
    public void onBookingCancelled(@Observes(during = TransactionPhase.IN_PROGRESS) BookingCancelledEvent event) 
    		throws BusinessException {
		if (event.getLeg().getState() == TripState.CANCELLED) {
			throw new IllegalStateException("Leg already cancelled: " + event.getLeg().getId());
		}
    	logger.info(String.format("Booking %s cancelled (from NetMobiel) by passenger because '%s'", 
    			event.getLeg().getBookingId(), event.getCancelReason() != null ? event.getCancelReason() : "---"));
		// The booking is cancelled through the TripManager or TripPlanManager
		bookingManager.removeBooking(event.getLeg().getBookingId(), event.getCancelReason(), false, false);
		if (event.getLeg().hasFareInCredits()) {
			cancelFare(event.getTrip(), event.getLeg());
		}
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
				cancelFare(trip, leg);
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
     * Handle the event where the traveller confirms (or denies) the trip. 
     * @param event
     * @throws BusinessException 
     */
    public void onTripConfirmation(@Observes(during = TransactionPhase.IN_PROGRESS) TripConfirmedEvent event) 
    		throws BusinessException {
    	if (event.getTrip().getState() != TripState.VALIDATING) {
    		throw new IllegalStateException("Trip has unexpected state: " + event.getTrip().getId() + " " + event.getTrip().getState());
    	}
   		evaluateTripAfterConfirmation(event.getTrip(), false);
    }

    /** 
     * Handle the event where the traveller unconfirms the trip. 
     * @param event
     * @throws BusinessException 
     */
    public void onTripUnconfirmation(@Observes(during = TransactionPhase.IN_PROGRESS) TripUnconfirmedEvent event) 
    		throws BusinessException {
    	List<Leg> legs = event.getTrip().getItinerary().findLegsToConfirm();
    	if (legs.size() > 1) {
    		logger.error("Multiple legs to (un)confirm: " + event.getTrip().getTripRef());
    	} else if (legs.isEmpty()) {
    		logger.warn("No leg to (un)confirm: " + event.getTrip().getTripRef());
    		return;
    	}
    	Leg legToConfirm = legs.get(0);
   		evaluateTripConfirmationRevocation(event.getTrip());
   		// Inform the rideshare to restart the validation (through the booking manager)
   		bookingManager.restartValidation(legToConfirm.getBookingId());
    }

    /** 
     * Handle the event where the provider confirms (or denies) the trip. 
     * @param event
     * @throws BusinessException 
     */
    public void onProviderConfirmation(@Observes(during = TransactionPhase.IN_PROGRESS) TripConfirmedByProviderEvent event) 
    		throws BusinessException {
  		// The trip manager checks the state for reasonable values
		Trip trip = tripManager.confirmTripByTransportProvider(event.getTravellerTripRef(), event.getBookingRef(), 
				event.getConfirmationByTransportProvider(), event.getConfirmationReason(), true);
   		evaluateTripAfterConfirmation(trip, false);
    }

    /** 
     * Handle the event where the provider unconfirms the trip. 
     * @param event
     * @throws BusinessException 
     */
    public void onProviderUnconfirmation(@Observes(during = TransactionPhase.IN_PROGRESS) TripUnconfirmedByProviderEvent event) 
    		throws BusinessException {
    	Trip trip = tripManager.getTrip(UrnHelper.getId(Trip.URN_PREFIX, event.getTravellerTripRef()));
   		evaluateTripConfirmationRevocationByProvider(trip);
   		// Trip manager must start revalidation
		tripManager.unconfirmTripByTransportProvider(event.getTravellerTripRef(), event.getBookingRef());
    }

    /**
     * The validation period has expired. Notice that this guard is placed at the trip. One guard is enough, the ride has no expiration
     * event defined.
     * @param event
     * @throws BusinessException
     */
    public void onTripValidationExpired(@Observes(during = TransactionPhase.IN_PROGRESS) TripValidationExpiredEvent event) 
    		throws BusinessException {
   		evaluateTripAfterConfirmation(event.getTrip(), true);
    }
    
    private void evaluateTripAfterConfirmation(Trip trip, boolean finalOrdeal) throws BusinessException {
		/**
		 * Defaults: traveller confirms the trip, provider denies the trip.
		 * If both have answered we can complete the trip, otherwise we have to wait for the expiration of the validation period.
		 * Trav /Driver	Yes			No 			?
		 *  Yes			Pay			Release		Release
		 *  No			Dispute		Release		Release
		 *  ?			Pay			Release		Release
		 */
    	// Check whether all answers are given 
    	if (finalOrdeal || trip.getItinerary().isConfirmationComplete()) {
    		// So what shall be the ordeal?
    		for (Leg leg : trip.getItinerary().findLegsToConfirm()) {
				if (! leg.isConfirmedByProvider()) {
					// Provider denies or has not answered. 
					cancelFare(trip, leg);
				} else if (leg.isDenied()) {
					// Provider has driven, traveller says no, not with me. We have a dispute.
					sendDisputeEmail(trip, leg);
				} else {
					payFare(trip, leg);
				}
			}
    	}
    }
    
    /**
     * Handle the revocation of the confirmation by the passenger.
     * 
     * @param trip the trip to consider
     * @throws BusinessException
     */
    private void evaluateTripConfirmationRevocation(Trip trip) throws BusinessException {
    	// If the payment is already made, then the passenger cannot reverse, only the driver can
    	// Only when payment is cancelled, action is taken and the payment is reserved again.
   		for (Leg leg : trip.getItinerary().findLegsToConfirm()) {
			if (leg.getPaymentState() == PaymentState.PAID) {
        		throw new RemoveException(String.format("Trip has been paid for, ask the driver to reconsider: %s %s",
        				trip.getTripRef(), leg.getLegRef()));
			} else if (leg.getPaymentState() == PaymentState.CANCELLED) {
				uncancelFare(trip, leg);
			} else {
				// Keep as is
			}
		}
    }

    /**
     * Handle the revocation of the confirmation by the driver.
     * 
     * @param trip the trip to consider
     * @throws BusinessException
     */
    private void evaluateTripConfirmationRevocationByProvider(Trip trip) throws BusinessException {
    	// If the payment is already made, then the passenger cannot reverse, only the driver can
    	// Only when payment is cancelled, action is taken and the payment is reserved again.
   		for (Leg leg : trip.getItinerary().findLegsToConfirm()) {
			if (leg.getPaymentState() == PaymentState.CANCELLED) {
        		throw new RemoveException(String.format("Trip payment has been cancelled, ask the passenger to reconsider: %s %s",
        				trip.getTripRef(), leg.getLegRef()));
			} else if (leg.getPaymentState() == PaymentState.PAID) {
				refundFare(trip, leg);
			} else {
				// Keep as is
			}
		}
    }
    private static final String SUBJECT = "Verschil van mening over rit";
    private static final String BODY = 
    			"Chauffeur ${driverName} (${driverEmail}) heeft volgens eigen zeggen passagier ${passengerName} " + 
    			"(${passengerEmail}) meegenomen van ${pickup} naar ${dropOff} op ${travelDate}. " +
				"Passagier ontkent de rit. Het gaat om trip ${tripId}. Bij deze een verzoek om te bemiddelen tussen de partijen. " +
    			"Na overeenkomst moet het dispuut handmatig worden opgelost in het systeem door een technisch medewerker. " +
    			"\n\nMet vriendelijke groet,\n\nNetMobiel Platform\n";
    
    private void sendDisputeEmail(Trip trip, Leg leg) throws BadRequestException {
		NetMobielUser driver = resolveDriverId(leg);
		Map<String, String> valuesMap = new HashMap<>();
		valuesMap.put("driverName", leg.getDriverName());
		valuesMap.put("driverEmail", driver != null ? driver.getEmail() : "?");
		valuesMap.put("passengerName", trip.getTraveller().getName());
		valuesMap.put("passengerEmail", trip.getTraveller().getEmail());
		valuesMap.put("pickup", leg.getFrom().getLabel());
		valuesMap.put("dropOff", leg.getTo().getLabel());
		valuesMap.put("travelDate", textHelper.formatDate(leg.getStartTime()));

		StringSubstitutor substitutor = new StringSubstitutor(valuesMap);
		String subject = substitutor.replace(SUBJECT);
		String body = substitutor.replace(BODY);
		sendEmail(subject, body, disputeEmailAddress);
	}

    private void sendEmail(String subject, String body, String recipient) {
		try {
            MimeMessage m = new MimeMessage(mailSession);
            m.setRecipients(javax.mail.Message.RecipientType.TO, recipient);
        	m.setFrom(senderEmailAddress);
            m.setSentDate(new Date());
            m.setSubject(subject);
            m.setContent(body, "text/plain");
            Transport.send(m);
        } catch (MessagingException e) {
            throw new SystemException(String.format("Failed to send email on '%s' to %s", subject, disputeEmailAddress), e);
        }
	}
	
    private NetMobielUser resolveDriverId(Leg leg) throws BadRequestException {
		NetMobielUser nmuser = null;
    	if (!NetMobielModule.KEYCLOAK.getCode().equals(UrnHelper.getService(leg.getDriverId()))) {
    		logger.error("Driver Id cannot be resolved this service: " + leg.getDriverId());
    	} else {
    		// Hmmm just fetch the booking and then the driver
    		try {
    			Booking bdb = bookingManager.getBooking(UrnHelper.getId(Booking.URN_PREFIX, leg.getBookingId()));
    			nmuser = bdb.getRide().getDriver();
    			if (!nmuser.getManagedIdentity().equals(UrnHelper.getIdAsString(NetMobielUser.KEYCLOAK_URN_PREFIX, leg.getDriverId()))) {
    				throw new IllegalStateException(String.format("Leg driverId %s is inconsistent with Booking driver Id %s", leg.getDriverId(), nmuser.getManagedIdentity()));
    			}
    		} catch (NotFoundException ex) {
    			logger.error("No such booking: " + leg.getBookingId());
    		}
    	}
    	return nmuser;
	}
}
