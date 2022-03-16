package eu.netmobiel.overseer.processor;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.annotation.security.RunAs;
import javax.ejb.Asynchronous;
import javax.ejb.Schedule;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;

import eu.netmobiel.banker.exception.BalanceInsufficientException;
import eu.netmobiel.banker.model.Reward;
import eu.netmobiel.banker.service.LedgerService;
import eu.netmobiel.banker.service.RewardService;
import eu.netmobiel.commons.NetMobielModule;
import eu.netmobiel.commons.annotation.Created;
import eu.netmobiel.commons.annotation.Removed;
import eu.netmobiel.commons.event.RewardEvent;
import eu.netmobiel.commons.event.RewardRollbackEvent;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.SystemException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.NetMobielUserImpl;
import eu.netmobiel.commons.model.PagedResult;
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
	/**
	 * The amount of premium credits that might be used to pay for a fare.
	 */
	private static final int FARE_MAXIMUM_PREMIUM_PERCENTAGE = 100;
	/**
	 * The incentive code for the payment of a completed ride.
	 */
	public static final String SHARED_RIDE_COMPLETED_CODE = "shared-ride-completed";

	@SuppressWarnings("unused")
	@Inject
    private PublisherService publisherService;

    @Inject
    private LedgerService ledgerService;

    @Inject
    private RewardService rewardService;

    @Inject
    private BookingManager bookingManager;

    @Inject
    private TripManager tripManager;

    @Inject
    private Event<TripEvaluatedEvent> tripEvaluatedEvent;

    @Inject
    private Event<RideEvaluatedEvent> rideEvaluatedEvent;

    @Inject
    private Event<RewardEvent> rewardEvent;

    @Inject
    private Event<RewardRollbackEvent> rewardRollbackEvent;
    
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
		if (booking.getPaymentState() != null && booking.getPaymentState() != expectedState) {
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
				leg.getFareInCredits(), FARE_MAXIMUM_PREMIUM_PERCENTAGE, textHelper.createDescriptionText(leg), leg.getLegRef());
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
		String releaseId = ledgerService.cancel(leg.getPaymentId());
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
		String chargeId = ledgerService.charge(resolveDriverId(leg), leg.getPaymentId());
		tripManager.updateLegPaymentState(trip, leg, PaymentState.PAID, chargeId);
		bookingManager.updatePaymentState(booking, PaymentState.PAID, chargeId);
		// Fire a rideshare fare completed event
		rewardEvent.fire(new RewardEvent(SHARED_RIDE_COMPLETED_CODE, booking.getRide().getDriver(), 
				booking.getUrn(), booking.getFareInCredits()));
    }

    /**
     * Mark the booking fare as disputed. To be designed.
     * @param trip
     * @param leg
     * @param booking
     * @throws BusinessException
     */
    private static void disputeFare(Trip trip, Leg leg, Booking booking) throws BusinessException {
    	assertLegHasFareInCredits(leg);
    	assertLegPaymentState(leg, PaymentState.RESERVED);
    	assertBookingPaymentState(booking, null);
    }

    /**
     * Reverses the earlier cancel of a fare by reserving the fare again on expense of the passenger.
     * It cannot be a real reversal, because the premium credits might already have spent on other travels.
     * However, that is not a an issue. Note that this call establishes a new transaction conversation.
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
		// Clear the booking payment details
		bookingManager.updatePaymentState(booking, null, null);
		// Uncancel (i.e. reserve again) the fare, this start a new transaction conversation. 
		// In the statement overview the entry is marked as a rollback.
		String reservationId = ledgerService.uncancel(leg.getPaymentId(), FARE_MAXIMUM_PREMIUM_PERCENTAGE);
		tripManager.updateLegPaymentState(trip, leg, PaymentState.RESERVED, reservationId);
    }

    /**
     * Reverses the earlier payment of a fare by refunding the fare on expense of the driver and 
     * reserving the same amount at the passenger's side.
     * Because the payment may have been made with premium credits, the resulting transaction is still part
     * of the existing transaction conversation. The head of this conversation contains the original reservation and
     * knows the origin of the credits involved.
     * @param trip
     * @param leg
     * @param booking
     * @throws BusinessException
     */
    private void unpayFare(Trip trip, Leg leg, Booking booking) throws BusinessException {
    	assertLegHasFareInCredits(leg);
    	assertLegPaymentState(leg, PaymentState.PAID);
    	assertBookingPaymentState(booking, PaymentState.PAID);
		rewardRollbackEvent.fire(new RewardRollbackEvent(SHARED_RIDE_COMPLETED_CODE, booking.getRide().getDriver(), booking.getUrn(), false));
		String reservationId = ledgerService.uncharge(booking.getPaymentId());
		tripManager.updateLegPaymentState(trip, leg, PaymentState.RESERVED, reservationId);
		bookingManager.updatePaymentState(booking, null, null);
    }

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
	
    /**
     * Attempt to pay-out a newly created Reward.
     * @param reward
     */
    @Asynchronous
    public void onNewReward(@Observes(during = TransactionPhase.AFTER_SUCCESS) @Created Reward reward) {
    	try {
    		if (reward.getIncentive().isRedemption()) {
    			ledgerService.rewardWithRedemption(reward, OffsetDateTime.now(), textHelper.createRewardStatementText(reward));
    		} else {
    			ledgerService.rewardWithPremium(reward, OffsetDateTime.now(), textHelper.createRewardStatementText(reward));
    		}
    	} catch (BalanceInsufficientException e) {
    		logger.warn("Premium balance is insufficient, reward payment is pending: " + reward.getUrn());
    	} catch (Exception e) {
    		logger.error("Error in onNewReward: " + e);
    	}
    }

    /**
     * For testing: Handle the disposal of a reward by reverting the payment of the reward.
     * This method a synchronous.
     * @param reward
     * @throws BusinessException
     */
    public void onRewardDisposal(@Observes(during = TransactionPhase.IN_PROGRESS) @Removed Reward reward) throws BusinessException {
    	if (reward.getTransaction() != null) {
    		ledgerService.refundReward(reward, OffsetDateTime.now());
    	}
    }

    /**
     * Check whether a reward could not be paid because there was insufficient balance on the system premium account.
     * How can we detect that case? When the transaction is null, it is certainly pending. What if it was cancelled? 
     * Is then a new reward created or is the old one paid again? 
     * OK. A reward is actually removed when it is withdrawn. So when a reward is still there, it must be paid when 
     * there is no transaction or when cancelTime is set. Withdrawal of rewards is now only for testing.
     * 
     * The method is placed here because we need the text for the accounting statement.
     * 
     * Note: redeemable rewards are never pending.
     */
    @Schedule(info = "Reward check", hour = "*/1", minute = "15", second = "0", persistent = false /* non-critical job */)
	public void resolvePendingRewardPayments() {
		try {
	    	Cursor cursor = new Cursor(10, 0);
	    	OffsetDateTime when = OffsetDateTime.now();
			PagedResult<Reward> pendingRewards = rewardService.listPendingRewards(cursor);
			if (! pendingRewards.getData().isEmpty()) {
				if (ledgerService.fiatForPremiumBalance(pendingRewards.getData().get(0).getAmount())) {
					// OK, at least the first can be processed. Go!	
		    		for (Reward reward: pendingRewards.getData()) {
				    	ledgerService.rewardWithPremium(reward, when, textHelper.createRewardStatementText(reward)); 
					}
					// Note: There is a potential danger that one bad reward will block processing of all others.
		    		// 		 For now we take that risk.
				} else {
		    		logger.warn("Premium balance is insufficient, rewards are pending payment");
				}
			}
    	} catch (BalanceInsufficientException e) {
    		logger.warn("Premium balance is insufficient, reward(s) payment remains pending");
    	} catch (Exception ex) {
			logger.error("Error processing pending rewards", ex);
    	}
	}

}
