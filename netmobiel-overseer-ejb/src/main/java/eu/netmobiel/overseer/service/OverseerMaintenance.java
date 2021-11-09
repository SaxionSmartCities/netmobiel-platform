package eu.netmobiel.overseer.service;

import java.text.MessageFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.communicator.filter.MessageFilter;
import eu.netmobiel.communicator.model.Conversation;
import eu.netmobiel.communicator.model.Envelope;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.service.PublisherService;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.service.TripManager;
import eu.netmobiel.planner.service.TripPlanManager;
import eu.netmobiel.profile.model.Delegation;
import eu.netmobiel.profile.service.DelegationManager;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.service.BookingManager;
import eu.netmobiel.rideshare.service.RideManager;

/**
 * Singleton startup bean for doing some maintenance on startup of the system.
 * 1. Migrate profiles to this profile service and assure all components know about all users.
 *  
 * @author Jaap Reitsma
 *
 */
@Singleton
@Startup
@Logging
public class OverseerMaintenance {
	private static final String DEFAULT_TIME_ZONE = "Europe/Amsterdam";
	private static final String DEFAULT_LOCALE = "nl-NL";
	@Inject
    private Logger log;

//	@Inject
//	private BankerUserManager bankerUserManager;
//
//	@Inject
//	private CommunicatorUserManager communicatorUserManager;
	@Inject
	private PublisherService publisherService;
	@Inject
	private BookingManager bookingManager; 
	@Inject
	private RideManager rideManager; 
//	@Inject
//	private PlannerUserManager plannerUserManager;
//
//	@Inject
//	private RideshareUserManager rideshareUserManager;
//
//	@Inject
//	private ProfileMaintenance profileMaintenance;

//	@Inject
//    private ProfileManager profileManager;
	@Inject
    private DelegationManager delegationManager;
	@Inject
    private TripPlanManager tripPlanManager;
	@Inject
    private TripManager tripManager;
    private Locale defaultLocale;

	@PostConstruct
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public void initialize() {
		log.info("Starting up the Overseer, checking for maintenance tasks");
    	defaultLocale = Locale.forLanguageTag(DEFAULT_LOCALE);
		// Get all CM users
		// Get all Profiles
		// Get all messages, skip those that have all envelopes set with a conversation

		// Is the sender the system?
		//   context: a booking
		//		Is the user a driver: Lookup or create conversation with the booking. Add the ride as context too
		//								Add booking and tripplan (if any) to the envelope. Add ride context to the message
		//		Is the user a passenger: Lookup or create conversation with the booking
		//								Add trip and/or tripplan to the envelope context. Add booking context to the message.
		//
		//   context: a tripplan
		//		Is the recipient a driver: Lookup or create conversation with the tripplan. Add the ride as context too
		//								Add booking to the envelope. Add ride context to the message
		//		Is the user a passenger: Error. No, could happen in case of direct message.
		//								
		//   context: a trip
		//		Is the recipient a driver: Error
		//		Is the user a passenger: Lookup or create conversation with the trip. 
		//								Add trip context the envelope. Add trip context to the message too.
		//
		//   context: a ride
		//		Is the recipient a driver: Lookup or create conversation with the ride. Add all bookings context too. 
		//		Is the recipient a passenger: Find the booking for this passenger and the trip (or tripplan). Lookup or create conversation with the trip or tripplan. 
		//								Add trip context the envelope. Add trip context to the message too.
		//
		// Sender is not system
		//   context: a ride
		//		Is the recipient a driver: Lookup or create conversation with the ride. Add all bookings contexts too. Booking is the driver's envelope context. Trip op tripplan is the message context 
		//		Is the recipient a passenger: Find the booking for this passenger and the trip (or tripplan). Lookup or create conversation with the trip or tripplan. 
		//								Add trip context the envelope. Add booking context to the message (sender's context).
//    	Map<NetMobielModule, List<String>> moduleUsersMap = new HashMap<>();
//    	moduleUsersMap.put(NetMobielModule.BANKER, bankerUserManager.listManagedIdentities());
//    	List<CommunicatorUser> commUsers = communicatorUserManager.listUsers();
//    	commUsers.remove(PublisherService.SYSTEM_USER.getManagedIdentity());
//    	moduleUsersMap.put(NetMobielModule.COMMUNICATOR, commUsers);
//    	moduleUsersMap.put(NetMobielModule.PLANNER, plannerUserManager.listManagedIdentities());
//    	moduleUsersMap.put(NetMobielModule.RIDESHARE, rideshareUserManager.listManagedIdentities());
//    	profileMaintenance.processReportOnNetMobielUsers(moduleUsersMap);
		try {
//	    	Map<String, Profile> profileMap = listAllProfiles();
	    	Cursor cursor = new Cursor(100, 0);
	    	MessageFilter filter = new MessageFilter(SortDirection.ASC.name());
	    	while (true) {
				PagedResult<Message> messages = publisherService.listMessages(filter, cursor);
				for (Message m : messages.getData()) {
					try {
						if (m.getContext().contains("booking")) {
							handleBooking(m);
						} else if (m.getContext().contains("tripplan")) {
							handleTripPlan(m);
						} else if (m.getContext().contains("trip")) {
							handleTrip(m);
						} else if (m.getContext().contains("ride")) {
							handleRide(m);
						} else if (m.getContext().contains("delegation")) {
							handleDelegation(m);
						} else {
							log.error("Cannot handle context: " + m.getContext());
						}
						publisherService.updateMessage(m.getId(), m);
					} catch (BusinessException e) {
						log.error("Error migrating message: " + e);
					}
				}
				if (messages.getCount() < cursor.getMaxResults()) {
					break;
				}
				cursor.next();
	    	}
		} catch (BusinessException e) {
			log.error("Error migrating messages", e);
		}
	}

	private void handleDelegation(Message m) throws NotFoundException, BadRequestException {
		Delegation delegation = delegationManager.getDelegation(UrnHelper.getId(Delegation.URN_PREFIX, m.getContext()), Delegation.PROFILES_ENTITY_GRAPH);

		for (Envelope e : m.getEnvelopes()) {
			if (e.getContext() != null || e.getOldRecipient() == null) {
				// Already handled
				continue;
			}
			// Driver is recipient
			e.setContext(m.getContext());
			Conversation conv = publisherService.lookupOrCreateConversation(e.getOldRecipient(), e.getContext(), createDelegationTopic(delegation), true);
			e.setConversation(conv);
			e.setOldRecipient(null);
		}
		if (m.getOldSender() != null) {
			// A chat message
			log.warn("No support to migrate sender conversation for delegation!");
		}
	}

	private void handleRide(Message m) throws NotFoundException, BadRequestException {
		Ride ride = rideManager.getRide(UrnHelper.getId(Ride.URN_PREFIX ,m.getContext()));

		for (Envelope e : m.getEnvelopes()) {
			if (e.getContext() != null || e.getOldRecipient() == null) {
				// Already handled
				continue;
			}
			if (e.getOldRecipient().getManagedIdentity().equals(ride.getDriver().getManagedIdentity())) {
				// Driver is recipient
				e.setContext(m.getContext());
				Conversation conv = publisherService.lookupOrCreateConversation(e.getOldRecipient(), e.getContext(), createRideTopic(ride), true);
				e.setConversation(conv);
			} else {
				// Passenger is recipient
				Booking myBooking = ride.getBookings().stream()
						.filter(b -> b.getPassenger().getManagedIdentity().equals(e.getOldRecipient().getManagedIdentity()))
						.findFirst()
						.orElse(null);
				if (myBooking == null) {
					log.warn("Can't find passenger in ride:" + ride.getUrn() + " " + e.getOldRecipient().getManagedIdentity());
				} else {
					Conversation conv = null;
					if (myBooking.getPassengerTripPlanRef() != null) {
						e.setContext(myBooking.getPassengerTripPlanRef());
						TripPlan plan = tripPlanManager.getTripPlan(UrnHelper.getId(TripPlan.URN_PREFIX, e.getContext()));
						conv = publisherService.lookupOrCreateConversation(e.getOldRecipient(), e.getContext(), createPassengerTripPlanTopic(plan), true);
						e.setConversation(conv);
					}
					if (myBooking.getPassengerTripRef() != null) {
						e.setContext(myBooking.getPassengerTripRef());
						Trip trip = tripManager.getTrip(UrnHelper.getId(Trip.URN_PREFIX, e.getContext()));
						if (conv == null) {
							conv = publisherService.lookupOrCreateConversation(e.getOldRecipient(), e.getContext(), createPassengerTripTopic(trip), true);
						} else {
							publisherService.addConversationContext(conv, e.getContext(), createPassengerTripTopic(trip), true);
						}
						e.setConversation(conv);
					}
				}
			}
			e.setOldRecipient(null);
		}
		if (m.getOldSender() != null) {
			if (m.getOldSender().getManagedIdentity().equals(ride.getDriver().getManagedIdentity())) {
				// Driver is sender
				Conversation conv = publisherService.lookupOrCreateConversation(m.getOldSender(), m.getContext(), createRideTopic(ride), true);
				m.addSender(conv, m.getContext());
			} else {
				// Passenger is sender
				Booking myBooking = ride.getBookings().stream()
						.filter(b -> b.getPassenger().getManagedIdentity().equals(m.getOldSender().getManagedIdentity()))
						.findFirst()
						.orElse(null);
				if (myBooking == null) {
					log.warn("Can't find passenger in ride:" + ride.getUrn() + " " + m.getOldSender().getManagedIdentity());
				} else {
					Conversation conv = null;
					String context = null;
					if (myBooking.getPassengerTripPlanRef() != null) {
						context = myBooking.getPassengerTripPlanRef();
						TripPlan plan = tripPlanManager.getTripPlan(UrnHelper.getId(TripPlan.URN_PREFIX, context));
						conv = publisherService.lookupOrCreateConversation(m.getOldSender(), context, createPassengerTripPlanTopic(plan), true);
					}
					if (myBooking.getPassengerTripRef() != null) {
						context = myBooking.getPassengerTripRef();
						Trip trip = tripManager.getTrip(UrnHelper.getId(Trip.URN_PREFIX, context));
						if (conv == null) {
							conv = publisherService.lookupOrCreateConversation(m.getOldSender(), context, createPassengerTripTopic(trip), true);
						} else {
							publisherService.addConversationContext(conv, context, createPassengerTripTopic(trip), true);
						}
					}
					m.addSender(conv, context);
					m.setContext(context);
				}
			}
			m.setOldSender(null);
		}
	}

	private void handleTrip(Message m) throws NotFoundException, BadRequestException {
		Trip trip = tripManager.getTrip(UrnHelper.getId(Trip.URN_PREFIX, m.getContext()));

		for (Envelope e : m.getEnvelopes()) {
			if (e.getContext() != null || e.getOldRecipient() == null) {
				// Already handled
				continue;
			}
			if (e.getOldRecipient().getManagedIdentity().equals(trip.getTraveller().getManagedIdentity())) {
				// Passenger is recipient
				e.setContext(m.getContext());
				Conversation conv = publisherService.lookupOrCreateConversation(e.getOldRecipient(), e.getContext(), createPassengerTripTopic(trip), true);
				e.setConversation(conv);
			} else {
				// Driver is recipient
				Leg rsleg = trip.getItinerary().getLegs().stream().filter(leg -> leg.getTraverseMode() == TraverseMode.RIDESHARE).findFirst().orElse(null);
				if (rsleg == null) {
					log.warn("Expected a RS leg in trip: " + m.getContext());
				} else {
					e.setContext(rsleg.getBookingId());
					Booking b = bookingManager.getBooking(UrnHelper.getId(Booking.URN_PREFIX, rsleg.getBookingId()));
					Ride r = b.getRide();
					Conversation conv = publisherService.lookupOrCreateConversation(e.getOldRecipient(), r.getUrn(), createRideTopic(r), true);
					publisherService.addConversationContext(conv, rsleg.getBookingId());
					e.setConversation(conv);
				}
			}
			e.setOldRecipient(null);
		}
		if (m.getOldSender() != null) {
			if (m.getOldSender().getManagedIdentity().equals(trip.getTraveller().getManagedIdentity())) {
				Conversation conv = publisherService.lookupOrCreateConversation(m.getOldSender(), m.getContext(), createPassengerTripTopic(trip), true);
				m.addSender(conv, m.getContext());
			} else {
				// Driver is sender
				Leg rsleg = trip.getItinerary().getLegs().stream().filter(leg -> leg.getTraverseMode() == TraverseMode.RIDESHARE).findFirst().orElse(null);
				if (rsleg == null) {
					log.warn("Expected a RS leg in trip: " + m.getContext());
				} else {
					Booking b = bookingManager.getBooking(UrnHelper.getId(Booking.URN_PREFIX, rsleg.getBookingId()));
					Ride r = b.getRide();
					Conversation conv = publisherService.lookupOrCreateConversation(m.getOldSender(), r.getUrn(), createRideTopic(r), true);
					publisherService.addConversationContext(conv, rsleg.getBookingId());
					m.addSender(conv, rsleg.getBookingId());
				}
			}
			m.setOldSender(null);
		}
	}

	private void handleTripPlan(Message m) throws NotFoundException, BadRequestException {
		TripPlan plan = tripPlanManager.getTripPlan(UrnHelper.getId(TripPlan.URN_PREFIX, m.getContext()));

		for (Envelope e : m.getEnvelopes()) {
			if (e.getContext() != null || e.getOldRecipient() == null) {
				// Already handled
				continue;
			}
			// Driver is recipient
			e.setContext(m.getContext());
			Conversation conv = publisherService.lookupOrCreateConversation(e.getOldRecipient(), e.getContext(), createDriverTripPlanTopic(plan), true);
			e.setConversation(conv);
			e.setOldRecipient(null);
		}
		if (m.getOldSender() != null) {
			// A chat message
			log.warn("No support to migrate sender conversation for tripplan!");
		}
	}

	private void handleBooking(Message m) throws NotFoundException, BadRequestException {
		Booking b = bookingManager.getBooking(UrnHelper.getId(Booking.URN_PREFIX, m.getContext()));
		Ride r = b.getRide();

		for (Envelope e : m.getEnvelopes()) {
			if (e.getContext() != null || e.getOldRecipient() == null) {
				// Already handled
				continue;
			}
			if (e.getOldRecipient().getManagedIdentity().equals(r.getDriver().getManagedIdentity())) {
				// Driver is recipient
				e.setContext(m.getContext());
				Conversation conv = publisherService.lookupOrCreateConversation(e.getOldRecipient(), r.getUrn(), createRideTopic(r), true);
				// Add booking context too.
				publisherService.addConversationContext(conv, m.getContext());
				e.setConversation(conv);
			} else {
				// Passenger is recipient
				Conversation conv = null;
				if (b.getPassengerTripPlanRef() != null) {
					e.setContext(b.getPassengerTripPlanRef());
					TripPlan plan = tripPlanManager.getTripPlan(UrnHelper.getId(TripPlan.URN_PREFIX, e.getContext()));
					conv = publisherService.lookupOrCreateConversation(e.getOldRecipient(), e.getContext(), createPassengerTripPlanTopic(plan), true);
					e.setConversation(conv);
				}
				if (b.getPassengerTripRef() != null) {
					e.setContext(b.getPassengerTripRef());
					Trip trip = tripManager.getTrip(UrnHelper.getId(Trip.URN_PREFIX, e.getContext()));
					if (conv == null) {
						conv = publisherService.lookupOrCreateConversation(e.getOldRecipient(), e.getContext(), createPassengerTripTopic(trip), true);
					} else {
						publisherService.addConversationContext(conv, e.getContext(), createPassengerTripTopic(trip), true);
					}
					e.setConversation(conv);
				}
				// For easier migration
				publisherService.addConversationContext(conv, m.getContext());
			}
			e.setOldRecipient(null);
		}
		if (m.getOldSender() != null) {
			// A chat message
			log.warn("No support to migrate sender conversation for booking!");
		}
	}

	private String createPassengerTripPlanTopic(TripPlan plan) {
		return MessageFormat.format("Oproep voor reis op {0} van {1} naar {2}", 
				DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(defaultLocale).format(plan.getTravelTime().atZone(ZoneId.of(DEFAULT_TIME_ZONE))),
				plan.getFrom().getLabel(), 
				plan.getTo().getLabel()
		);
	}

	private String createDriverTripPlanTopic(TripPlan plan) {
		return createPassengerTripPlanTopic(plan);
	}

	private String createPassengerTripTopic(Trip trip) {
		return MessageFormat.format("Reis op {0} van {1} naar {2}", 
				DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(defaultLocale).format(trip.getItinerary().getDepartureTime().atZone(ZoneId.of(DEFAULT_TIME_ZONE))),
				trip.getFrom().getLabel(), 
				trip.getTo().getLabel()
		);
	}
	
	private String createRideTopic(Ride r) {
		return MessageFormat.format("Rit op {0} van {1} naar {2}", 
				DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(defaultLocale).format(r.getDepartureTime().atZone(ZoneId.of(DEFAULT_TIME_ZONE))),
				r.getFrom().getLabel(), 
				r.getTo().getLabel()
		);
	}

	private static String createDelegationTopic(Delegation d) {
		return MessageFormat.format("Beheer van reizen van {0}", d.getDelegator().getName());
	}

//	private Map<String, Profile> listAllProfiles() throws BadRequestException {
//		Map<String, Profile> profileMap = new HashMap<>();
//		ProfileFilter filter = new ProfileFilter();
//		Cursor cursor = new Cursor(100, 0);
//		while (true) {
//			PagedResult<Profile> profiles = profileManager.listProfiles(filter, cursor);
//			profiles.getData().forEach(p -> profileMap.put(p.getManagedIdentity(), p));
//			if (profiles.getCount() < cursor.getMaxResults()) {
//				break;
//			}
//			cursor.increaseOffset(cursor.getMaxResults());
//		}
//		return profileMap;
//	}

	
}
