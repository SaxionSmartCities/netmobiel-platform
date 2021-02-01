package eu.netmobiel.overseer.processor;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;
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
import javax.mail.internet.MimeMessage.RecipientType;

import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;

import eu.netmobiel.banker.service.LedgerService;
import eu.netmobiel.commons.NetMobielModule;
import eu.netmobiel.commons.event.BookingCancelledFromProviderEvent;
import eu.netmobiel.commons.event.TripConfirmedByProviderEvent;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.SystemException;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.service.PublisherService;
import eu.netmobiel.planner.event.BookingCancelledEvent;
import eu.netmobiel.planner.event.BookingConfirmedEvent;
import eu.netmobiel.planner.event.BookingProposalRejectedEvent;
import eu.netmobiel.planner.event.BookingRequestedEvent;
import eu.netmobiel.planner.event.TripConfirmedEvent;
import eu.netmobiel.planner.event.TripValidationExpiredEvent;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.PaymentState;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.model.Trip;
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
	private static final String DEFAULT_TIME_ZONE = "Europe/Amsterdam";
	private static final String DEFAULT_LOCALE = "nl-NL";
	
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
    
    private Locale defaultLocale;

	@Resource(mappedName="java:jboss/mail/NetMobiel")
    private Session mailSession;	

    @Resource(lookup = "java:global/planner/disputeEmailAddress")
    private String disputeEmailAddress;

    @Resource(lookup = "java:global/planner/senderEmailAddress")
    private String senderEmailAddress;

    @PostConstruct
    public void initialize() {
    	defaultLocale = Locale.forLanguageTag(DEFAULT_LOCALE);
    }

    private String formatDate(Instant instant) {
    	return DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(defaultLocale).format(instant.atZone(ZoneId.of(DEFAULT_TIME_ZONE)));
    }

    private String createDescription(Leg leg) {
    	String prefix = null;
    	if (leg.getTraverseMode() == TraverseMode.RIDESHARE) {
    		prefix = String.format("Meerijden met %s", leg.getDriverName()); 
    	} else if (leg.getTraverseMode().isTransit()) {
    		prefix = String.format("Reizen met %s", leg.getAgencyName()); 
    	} else {
    		throw new IllegalStateException("TraverseMode is not supported: " + leg.getTraverseMode());
    	}
		return String.format("%s van %s naar %s op %s", prefix, 
			leg.getFrom().getLabel(), leg.getTo().getLabel(), formatDate(leg.getStartTime()));
    }

    protected void reserveFare(Trip trip, Leg leg) throws BusinessException {
		if (leg.hasFareInCredits()) {
			// Reserve the fare
			String reservationId = ledgerService.reserve(trip.getTraveller(), leg.getFareInCredits(), createDescription(leg), leg.getLegRef());
			tripManager.updateLegPaymentState(trip, leg, PaymentState.RESERVED, reservationId);
		}
    }

    protected void cancelFare(Trip trip, Leg leg) throws BusinessException {
		if (leg.hasFareInCredits()) {
			// Release the fare
			if (leg.getPaymentState() != PaymentState.RESERVED || leg.getPaymentId() == null) {
				throw new IllegalStateException("Cannot cancel fare, payment state is invalid: " + 
						leg.getLegRef() + " " + leg.getPaymentState() + " " + leg.getPaymentId());
			}
			String releaseId = ledgerService.release(leg.getPaymentId());
			tripManager.updateLegPaymentState(trip, leg, PaymentState.CANCELLED, releaseId);
			// Settled without payment
			bookingManager.informBookingSettled(leg.getTripId(), leg.getBookingId());
		}
    }

    protected void payFare(Trip trip, Leg leg) throws BusinessException {
		if (leg.hasFareInCredits()) {
			if (leg.getPaymentState() != PaymentState.RESERVED) {
				throw new IllegalStateException("Cannot pay fare, payment state is invalid: " + 
						leg.getLegRef() + " " + leg.getPaymentState() + " " + leg.getPaymentId());
			}
			String chargeId = ledgerService.charge(resolveDriverId(leg), leg.getPaymentId(), leg.getFareInCredits());
			tripManager.updateLegPaymentState(trip, leg, PaymentState.PAID, chargeId);
			// Settled with payment
			bookingManager.informBookingSettled(leg.getTripId(), leg.getBookingId());
		}
    }

    /**
     * Handler for the case when a traveller requests a booking of a ride. 
     * Autobooking is assumed, so no interaction with the driver is required. Because autoconfirm is enabled, the fare is also charged as a reservation. 
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
    }

    /**
     * Handles the case where a traveller confirms a proposed booking of a provider. The provider's gets the trip reference assigned.
     * The fare is debited for the traveller and credited to the reservation account.  
     * @param event the confirmed event
     * @throws BusinessException 
     */
    public void onBookingConfirmed(@Observes(during = TransactionPhase.IN_PROGRESS) BookingConfirmedEvent event) throws BusinessException {
		// Replace the plan reference with trip reference
		bookingManager.confirmBooking(UrnHelper.getId(Booking.URN_PREFIX, event.getLeg().getBookingId()), event.getTrip().getTripRef());
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
		if (b.getPassengerTripRef() != null) {
			// The call in in the trip manager checks the state of the leg.
			Leg leg = tripManager.cancelBooking(b.getPassengerTripRef(), event.getBookingRef(), event.getCancelReason(), event.isCancelledByDriver());
			if (leg.hasFareInCredits()) {
				// cancel the reservation
    			Trip trip = tripManager.getTrip(UrnHelper.getId(Trip.URN_PREFIX, b.getPassengerTripRef()));
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
			Message msg = new Message();
			msg.setContext(event.getBookingRef());
			msg.setSubject("Chauffeur heeft geannuleerd.");
			msg.setBody(
					MessageFormat.format("Voor jouw reis op {0} naar {1} kun je helaas niet meer met {2} meerijden.", 
							formatDate(b.getDepartureTime()),
							b.getDropOff().getLabel(), 
							b.getRide().getDriver().getGivenName()
							)
					);
			msg.setDeliveryMode(DeliveryMode.NOTIFICATION);
			msg.addRecipient(event.getTraveller());
			publisherService.publish(null, msg);
		} else {
			// Notification of the driver is done by transport provider
		}
    }

    /** 
     * Handle the event where the traveller or provider confirms (or denies) the trip. 
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

    public void onProviderConfirmation(@Observes(during = TransactionPhase.IN_PROGRESS) TripConfirmedByProviderEvent event) 
    		throws BusinessException {
  		// The trip manager checks the state for reasonable values
		tripManager.confirmTripByTransportProvider(event.getTravellerTripRef(), event.getBookingRef(), 
				event.getConfirmationByTransportProvider(), event.getConfirmationReason(), false);
    }

    public void onTripValidationExpired(@Observes(during = TransactionPhase.IN_PROGRESS) TripValidationExpiredEvent event) 
    		throws BusinessException {
   		evaluateTripAfterConfirmation(event.getTrip(), true);
    }
    
    protected void evaluateTripAfterConfirmation(Trip trip, boolean finalOrdeal) throws BusinessException {
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
//    	tripManager.markTripFaresPaid(trip);
    }
    
    private static final String SUBJECT = "Verschil van mening over rit";
    private static final String BODY = 
    			"Chauffeur ${driverName} (${driverEmail}) heeft volgens eigen zeggen passagier ${passengerName} " + 
    			"(${passengerEmail}) meegenomen van ${pickup} naar ${dropOff} op ${travelDate}. " +
				"Passagier ontkent de rit. Het gaat om trip ${tripId}. Bij deze een verzoek om te bemiddelen tussen de partijen. " +
    			"Na overeenkomst moet het dispuut handmatig worden opgelost in het systeem door een technisch medewerker. " +
    			"\n\nMet vriendelijke groet,\n\nNetMobiel Platform\n";
    
	protected void sendDisputeEmail(Trip trip, Leg leg) {
		NetMobielUser driver = resolveDriverId(leg);
		Map<String, String> valuesMap = new HashMap<>();
		valuesMap.put("driverName", leg.getDriverName());
		valuesMap.put("driverEmail", driver != null ? driver.getEmail() : "?");
		valuesMap.put("passengerName", trip.getTraveller().getName());
		valuesMap.put("passengerEmail", trip.getTraveller().getEmail());
		valuesMap.put("pickup", leg.getFrom().getLabel());
		valuesMap.put("dropOff", leg.getTo().getLabel());
		valuesMap.put("travelDate", formatDate(leg.getStartTime()));

		StringSubstitutor substitutor = new StringSubstitutor(valuesMap);
		String subject = substitutor.replace(SUBJECT);
		String body = substitutor.replace(BODY);
		sendEmail(subject, body, disputeEmailAddress);
	}

	protected void sendEmail(String subject, String body, String recipient) {
		try {
            MimeMessage m = new MimeMessage(mailSession);
            m.setRecipients(RecipientType.TO, recipient);
        	m.setFrom(senderEmailAddress);
            m.setSentDate(new Date());
            m.setSubject(subject);
            m.setContent(body, "text/plain");
            Transport.send(m);
        } catch (MessagingException e) {
            throw new SystemException(String.format("Failed to send email on '%s' to %s", subject, disputeEmailAddress), e);
        }
	}
	
	protected NetMobielUser resolveDriverId(Leg leg) {
		NetMobielUser nmuser = null;
    	if (!NetMobielModule.RIDESHARE.getCode().equals(UrnHelper.getService(leg.getDriverId()))) {
    		logger.error("Driver Id cannot be resolved this service: " + leg.getDriverId());
    	} else {
    		// Hmmm just fetch the booking and then the driver
    		try {
    			Booking bdb = bookingManager.getBooking(UrnHelper.getId(Booking.URN_PREFIX, leg.getBookingId()));
    			nmuser = bdb.getRide().getDriver();
    		} catch (NotFoundException ex) {
    			logger.error("No such booking: " + leg.getBookingId());
    		}
    	}
    	return nmuser;
	}
}
