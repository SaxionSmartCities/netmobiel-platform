package eu.netmobiel.overseer.processor;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.annotation.security.RunAs;
import javax.ejb.Asynchronous;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;

import eu.netmobiel.banker.service.LedgerService;
import eu.netmobiel.commons.NetMobielModule;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.SystemException;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.NetMobielUserImpl;
import eu.netmobiel.commons.model.PaymentState;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.communicator.service.PublisherService;
import eu.netmobiel.planner.event.TripEvaluatedEvent;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripState;
import eu.netmobiel.planner.service.TripManager;
import eu.netmobiel.rideshare.event.RideEvaluatedEvent;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.service.BookingManager;

/**
 * Stateless bean for the management of the high-level payment process, involving multiple modules.
 * The payment processor takes care of a synchronized update of bank accounts, trip and ride in a single transaction.
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
public class PaymentProcessor {
	
    @SuppressWarnings("unused")
	@Inject
    private PublisherService publisherService;

    @Inject
    private LedgerService ledgerService;

    @Inject
    private BookingManager bookingManager;

    @Inject
    private TripManager tripManager;

    @Inject
    private Event<TripEvaluatedEvent> tripEvaluatedEvent;

    @Inject
    private Event<RideEvaluatedEvent> rideEvaluatedEvent;

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

    private static void assertLegHasFareInCredits(Leg leg) {
		if (!leg.hasFareInCredits()) {
			throw new IllegalArgumentException("Leg has no fare: " + leg.getLegRef());
		}
    }

    private static void assertLegPaymentState(Leg leg, PaymentState expectedState) {
		if (leg.getPaymentState() != expectedState) {
			throw new IllegalStateException("Leg has unexpected payment state: " + 
					leg.getLegRef() + " " + leg.getPaymentState() + " " + leg.getPaymentId());
		}
    }

	private static void assertBookingPaymentState(Booking booking, PaymentState expectedState) {
		if (booking.getPaymentState() != expectedState) {
			throw new IllegalStateException("Unexpected booking payment state: " + 
					booking.getUrn() + " " + booking.getPaymentState() + " " + booking.getPaymentId());
		}
    }

    private static NetMobielUser resolveDriverId(Leg leg) throws BadRequestException {
    	if (!NetMobielModule.KEYCLOAK.getCode().equals(UrnHelper.getService(leg.getDriverId()))) {
    		throw new IllegalStateException("Driver Id cannot be resolved: " + leg.getDriverId());
    	} 
    	return new NetMobielUserImpl(UrnHelper.getIdAsString(NetMobielUser.KEYCLOAK_URN_PREFIX, leg.getDriverId()));
	}

	
	/**
     * Reserves the fare for a trip on account of the traveller 
     * The payment state of the trip leg is marked as RESERVED.
     * The leg also contains the reference to the reservation transaction.  
     * @param trip
     * @param leg
     * @param booking
     * @throws BusinessException, especially InsufficientBalanceException in case of not enough credits.
     */
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void reserveFare(Trip trip, Leg leg) throws BusinessException {
    	assertLegHasFareInCredits(leg);
		String reservationId = ledgerService.reserve(trip.getTraveller(), 
				leg.getFareInCredits(), textHelper.createDescriptionText(leg), leg.getLegRef());
		tripManager.updateLegPaymentState(trip, leg, PaymentState.RESERVED, reservationId);
    }

    /**
     * Cancels a previously reserved fare. The fare is released to the account of the passenger (traveller).
     * The payment state of both the trip leg and the booking are marked as CANCELLED.
     * The leg also contains the reference to the release transaction.
     * After successful completion, both trip and ride have finished the validation. 
     * @param trip
     * @param leg
     * @param booking
     * @throws BusinessException
     */
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void cancelFare(Trip trip, Leg leg, Booking booking) throws BusinessException {
    	assertLegHasFareInCredits(leg);
    	assertLegPaymentState(leg, PaymentState.RESERVED);
    	assertBookingPaymentState(booking, null);
		String releaseId = ledgerService.release(leg.getPaymentId());
		tripManager.updateLegPaymentState(trip, leg, PaymentState.CANCELLED, releaseId);
		bookingManager.updatePaymentState(booking, PaymentState.CANCELLED, null);
    }

    /**
     * Pays an earlier reserved fare for a trip to the driver.
     * The payment state of both the trip leg and the booking are marked as PAID.
     * The both booking and trip leg also contains the reference to the charge transaction.
     * After successful completion, both trip and ride have finished the validation. 
     * @param trip
     * @param leg
     * @param booking
     * @throws BusinessException
     */
    private void payFare(Trip trip, Leg leg, Booking booking) throws BusinessException {
    	assertLegHasFareInCredits(leg);
    	assertLegPaymentState(leg, PaymentState.RESERVED);
    	assertBookingPaymentState(booking, null);
		String chargeId = ledgerService.charge(resolveDriverId(leg), leg.getPaymentId(), leg.getFareInCredits());
		tripManager.updateLegPaymentState(trip, leg, PaymentState.PAID, chargeId);
		bookingManager.updatePaymentState(booking, PaymentState.PAID, chargeId);
    }

    /**
     * Mark the booking fare as disputed (in the booking only). 
     * @param trip
     * @param leg
     * @param booking
     * @throws BusinessException
     */
    private void disputeFare(Trip trip, Leg leg, Booking booking) throws BusinessException {
    	assertLegHasFareInCredits(leg);
    	assertLegPaymentState(leg, PaymentState.RESERVED);
    	assertBookingPaymentState(booking, null);
		bookingManager.updatePaymentState(booking, PaymentState.DISPUTED, null);
    }

    /**
     * Reverses the earlier cancel of a fare by reserving the fare again on expense of the passenger.
     * The payment state of the booking is cleared.
     * @param trip
     * @param leg
     * @param booking
     * @throws BusinessException
     */
    private void uncancelFare(Trip trip, Leg leg, Booking booking) throws BusinessException {
    	assertLegHasFareInCredits(leg);
    	assertLegPaymentState(leg, PaymentState.CANCELLED);
    	assertBookingPaymentState(booking, PaymentState.CANCELLED);
		// Reserve the fare again
		String reservationId = ledgerService.unrelease(leg.getPaymentId());
		tripManager.updateLegPaymentState(trip, leg, PaymentState.RESERVED, reservationId);
		bookingManager.updatePaymentState(booking, null, null);
    }

    /**
     * Reverses the earlier payment of a fare by reserving the fare again on expense of the driver.
     * @param trip
     * @param leg
     * @param booking
     * @throws BusinessException
     */
    private void unpayFare(Trip trip, Leg leg, Booking booking) throws BusinessException {
    	assertLegHasFareInCredits(leg);
    	assertLegPaymentState(leg, PaymentState.PAID);
    	assertBookingPaymentState(booking, PaymentState.PAID);
		String reservationId = ledgerService.uncharge(booking.getPaymentId());
		tripManager.updateLegPaymentState(trip, leg, PaymentState.RESERVED, reservationId);
		bookingManager.updatePaymentState(booking, null, null);
    }

//    /**
//     * Reverses the earlier dispute setting by clearing this state from the booking.
//     * The leg is not touched.
//     * @param trip
//     * @param leg
//     * @param booking
//     * @throws BusinessException
//     */
//    private void undisputeFare(Trip trip, Leg leg, Booking booking) throws BusinessException {
//    	assertLegHasFareInCredits(leg);
//    	assertLegPaymentState(leg, PaymentState.RESERVED);
//    	assertBookingPaymentState(booking, PaymentState.DISPUTED);
//		bookingManager.updatePaymentState(booking, null, null);
//    }

    /**
     * Evaluates the current state of trip. If enough information is provided a payment is made or cancelled.
     * In case of a dispute the payment state remains as is.
     * If the information is not complete, it has no effect.
     * If the payment state of the leg is not RESERVED, then it has no effect.
     * Force a new transaction to get a consistent view. 
     * @param trip the trip to consider.
     * @param finalOrdeal if true then force a final evaluation based on implicit answers. 
     * 					If false then an ordeal is made only when both 
     * 					driver and passenger have provided an explicit answer.
     * @throws BusinessException
     */
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void evaluateTripAfterConfirmation(String tripId, boolean finalOrdeal) throws BusinessException {
		/**
		 * Defaults: traveller confirms the trip, provider denies the trip.
		 * If both have answered we can complete the trip, otherwise we have to wait for the expiration of the validation period.
		 * We prefer explit answers because we also like to receive a review of each party.
		 * Trav /Driver	Yes			No 			?
		 *  Yes			Pay			Release		Release
		 *  No			Dispute		Release		Release
		 *  ?			Pay			Release		Release
		 */
    	Trip trip = tripManager.getTrip(UrnHelper.getId(Trip.URN_PREFIX, tripId));
    	if (trip.getState() != TripState.VALIDATING) {
    		logger.warn("Trip is not validating, skipping evaluation: " + trip.getId() + " " + trip.getState());
    		return;
    	}
    	// Check whether all answers are given
    	if (finalOrdeal || trip.getItinerary().isConfirmationComplete()) {
    		// So what shall be the ordeal?
    		for (Leg leg : trip.getItinerary().findLegsToConfirm()) {
    			if (!leg.isPaymentDue()) {
    				continue;
    			}
    			Booking b = bookingManager.getShallowBooking(leg.getBookingId());
				if (! leg.isConfirmedByProvider()) {
					// Provider denies or has not answered. 
					cancelFare(trip, leg, b);
				} else if (leg.isDenied()) {
					// Provider has driven, traveller says no, not with me. We have a dispute.
					// sendDisputeEmail(trip, leg);
					disputeFare(trip, leg, b);
					// Remain in this state
				} else {
					payFare(trip, leg, b);
				}
				tripEvaluatedEvent.fire(new TripEvaluatedEvent(trip));
				rideEvaluatedEvent.fire(new RideEvaluatedEvent(b.getRide()));
			}
    	}
    }

    /**
     * Revokes the negative confirmation of the trip. This call reverses 
     * the evaluateTripAfterConfirmation, that lead to a cancel of the payment. 
     * In addition, this method removes attributes leading to a confirmation.
     * 
     * @param tripId the trip to consider
     * @throws BusinessException
     */
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void revokeNegativeTripConfirmation(Trip trip) throws BusinessException {
    	// If the payment is already made, then the passenger cannot reverse, only the driver can
    	// Only when payment is cancelled, action is taken and the payment is reserved again.
   		for (Leg leg : trip.getItinerary().findLegsToConfirm()) {
			Booking booking = bookingManager.getShallowBooking(leg.getBookingId());
			if (leg.getPaymentState() == PaymentState.CANCELLED) {
				uncancelFare(trip, leg, booking);
			}
		}
    }

    /**
     * Handle the revocation of the confirmation by the driver.
     * 
     * @param tripId the trip to consider
     * @throws BusinessException
     */
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void revokePositiveTripConfirmation(Trip trip) throws BusinessException {
    	// If the payment is already made, then the passenger cannot reverse, only the driver can
    	// Only when payment is cancelled, action is taken and the payment is reserved again.
   		for (Leg leg : trip.getItinerary().findLegsToConfirm()) {
			Booking booking = bookingManager.getShallowBooking(leg.getBookingId());
			if (leg.getPaymentState() == PaymentState.PAID) {
				unpayFare(trip, leg, booking);
			}
		}
    }

//    /**
//     * Handle the revocation of the confirmation if disputed. The disputed state is only at the driver.
//     * 
//     * @param tripId the trip to consider
//     * @throws BusinessException
//     */
//    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
//    public void revokeDisputedTripConfirmation(String tripId) throws BusinessException {
//    	Trip trip = tripManager.getTrip(UrnHelper.getId(Trip.URN_PREFIX, tripId));
//   		for (Leg leg : trip.getItinerary().findLegsToConfirm()) {
//			Booking booking = bookingManager.getShallowBooking(leg.getBookingId());
//			if (booking.getPaymentState() == PaymentState.DISPUTED) {
//				undisputeFare(trip, leg, booking);
//				resetConfirmation(leg, booking);
//			}
//		}
//    }

    private static final String SUBJECT = "Verschil van mening over rit";
    private static final String BODY = 
    			"Chauffeur ${driverName} (${driverEmail}) heeft volgens eigen zeggen passagier ${passengerName} " + 
    			"(${passengerEmail}) meegenomen van ${pickup} naar ${dropOff} op ${travelDate}. " +
				"Passagier ontkent de rit. Het gaat om trip ${tripId}. Bij deze een verzoek om te bemiddelen tussen de partijen. " +
    			"Na overeenkomst moet het dispuut handmatig worden opgelost in het systeem door een technisch medewerker. " +
    			"\n\nMet vriendelijke groet,\n\nNetMobiel Platform\n";
    
    @SuppressWarnings("unused")
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
	
}
