package eu.netmobiel.overseer.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
import eu.netmobiel.communicator.model.UserRole;
import eu.netmobiel.communicator.service.PublisherService;
import eu.netmobiel.overseer.processor.TextHelper;
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
    @Inject
    private TextHelper textHelper;
    

	@PostConstruct
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public void initialize() {
		log.info("Starting up the Overseer, checking for maintenance tasks");
    	migrateAllMessagesToConversations();
    	updateMessageBody();
	}
	
	private void migrateAllMessagesToConversations() {
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
						if (m.getContext().contains(":booking:")) {
							handleBooking(m);
						} else if (m.getContext().contains(":tripplan:")) {
							handleTripPlan(m);
						} else if (m.getContext().contains(":trip:")) {
							handleTrip(m);
						} else if (m.getContext().contains(":ride:")) {
							handleRide(m);
						} else if (m.getContext().contains(":delegation:")) {
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
			e.setContext(m.getContext());
			Conversation conv = publisherService.lookupOrCreateConversation(e.getOldRecipient(), UserRole.DELEGATE, e.getContext(), textHelper.createDelegationTopic(delegation), true);
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
				Conversation conv = publisherService.lookupOrCreateConversation(e.getOldRecipient(), UserRole.DRIVER, e.getContext(), textHelper.createRideTopic(ride), true);
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
						conv = publisherService.lookupOrCreateConversation(e.getOldRecipient(), UserRole.PASSENGER, e.getContext(), textHelper.createPassengerShoutOutTopic(plan), true);
						e.setConversation(conv);
					}
					if (myBooking.getPassengerTripRef() != null) {
						e.setContext(myBooking.getPassengerTripRef());
						Trip trip = tripManager.getTrip(UrnHelper.getId(Trip.URN_PREFIX, e.getContext()));
						if (conv == null) {
							conv = publisherService.lookupOrCreateConversation(e.getOldRecipient(), UserRole.PASSENGER, e.getContext(), textHelper.createPassengerTripTopic(trip), true);
						} else {
							publisherService.addConversationContext(conv, e.getContext(), textHelper.createPassengerTripTopic(trip), true);
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
				Conversation conv = publisherService.lookupOrCreateConversation(m.getOldSender(), UserRole.DRIVER, m.getContext(), textHelper.createRideTopic(ride), true);
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
						conv = publisherService.lookupOrCreateConversation(m.getOldSender(), UserRole.PASSENGER, context, textHelper.createPassengerShoutOutTopic(plan), true);
					}
					if (myBooking.getPassengerTripRef() != null) {
						context = myBooking.getPassengerTripRef();
						Trip trip = tripManager.getTrip(UrnHelper.getId(Trip.URN_PREFIX, context));
						if (conv == null) {
							conv = publisherService.lookupOrCreateConversation(m.getOldSender(), UserRole.PASSENGER, context, textHelper.createPassengerTripTopic(trip), true);
						} else {
							publisherService.addConversationContext(conv, context, textHelper.createPassengerTripTopic(trip), true);
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
				Conversation conv = publisherService.lookupOrCreateConversation(e.getOldRecipient(), UserRole.PASSENGER, e.getContext(), textHelper.createPassengerTripTopic(trip), true);
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
					Conversation conv = publisherService.lookupOrCreateConversation(e.getOldRecipient(), UserRole.DRIVER, r.getUrn(), textHelper.createRideTopic(r), true);
					publisherService.addConversationContext(conv, rsleg.getBookingId());
					e.setConversation(conv);
				}
			}
			e.setOldRecipient(null);
		}
		if (m.getOldSender() != null) {
			if (m.getOldSender().getManagedIdentity().equals(trip.getTraveller().getManagedIdentity())) {
				Conversation conv = publisherService.lookupOrCreateConversation(m.getOldSender(), UserRole.PASSENGER, m.getContext(), textHelper.createPassengerTripTopic(trip), true);
				m.addSender(conv, m.getContext());
			} else {
				// Driver is sender
				Leg rsleg = trip.getItinerary().getLegs().stream().filter(leg -> leg.getTraverseMode() == TraverseMode.RIDESHARE).findFirst().orElse(null);
				if (rsleg == null) {
					log.warn("Expected a RS leg in trip: " + m.getContext());
				} else {
					Booking b = bookingManager.getBooking(UrnHelper.getId(Booking.URN_PREFIX, rsleg.getBookingId()));
					Ride r = b.getRide();
					Conversation conv = publisherService.lookupOrCreateConversation(m.getOldSender(), UserRole.DRIVER, r.getUrn(), textHelper.createRideTopic(r), true);
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
			Conversation conv = publisherService.lookupOrCreateConversation(e.getOldRecipient(), UserRole.DRIVER, e.getContext(), textHelper.createDriverShoutOutTopic(plan), true);
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
				Conversation conv = publisherService.lookupOrCreateConversation(e.getOldRecipient(), UserRole.DRIVER, r.getUrn(), textHelper.createRideTopic(r), true);
				// Add booking context too.
				publisherService.addConversationContext(conv, m.getContext());
				e.setConversation(conv);
			} else {
				// Passenger is recipient
				Conversation conv = null;
				if (b.getPassengerTripPlanRef() != null) {
					e.setContext(b.getPassengerTripPlanRef());
					TripPlan plan = tripPlanManager.getTripPlan(UrnHelper.getId(TripPlan.URN_PREFIX, e.getContext()));
					conv = publisherService.lookupOrCreateConversation(e.getOldRecipient(), UserRole.PASSENGER, e.getContext(), textHelper.createPassengerShoutOutTopic(plan), true);
					e.setConversation(conv);
				}
				if (b.getPassengerTripRef() != null) {
					e.setContext(b.getPassengerTripRef());
					Trip trip = tripManager.getTrip(UrnHelper.getId(Trip.URN_PREFIX, e.getContext()));
					if (conv == null) {
						conv = publisherService.lookupOrCreateConversation(e.getOldRecipient(), UserRole.PASSENGER, e.getContext(), textHelper.createPassengerTripTopic(trip), true);
					} else {
						publisherService.addConversationContext(conv, e.getContext(), textHelper.createPassengerTripTopic(trip), true);
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

	private void updateMessageBody() {
		try {
	    	Cursor cursor = new Cursor(100, 0);
	    	MessageFilter filter = new MessageFilter(SortDirection.ASC.name());
	    	while (true) {
				PagedResult<Message> messages = publisherService.listMessages(filter, cursor);
				for (Message m : messages.getData()) {
					try {
						if (m.getSender() != null) {
							// Private message. Do not change
							continue;
						}
						if (m.getContext().contains(":booking:")) {
							updateBooking(m);
						} else if (m.getContext().contains(":tripplan:")) {
							updateTripPlan(m);
						} else if (m.getContext().contains(":trip:")) {
							updateTrip(m);
						} else if (m.getContext().contains(":ride:")) {
							updateRide(m);
						} else if (m.getContext().contains(":delegation:")) {
							updateDelegation(m);
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

	private void updateBooking(Message m) throws BusinessException {
		Booking b = bookingManager.getBooking(UrnHelper.getId(Booking.URN_PREFIX, m.getContext()));
		Ride r = b.getRide();
		if (m.getBody().startsWith("Voor jouw rit")) {
			// Driver
			if (m.getBody().contains("niet")) {
				String reason = b.getCancelReason() != null && !b.getCancelReason().isEmpty() ? ": " + b.getCancelReason() : ".";
				m.setBody(String.format("%s rijdt niet meer met je mee%s", b.getPassenger().getName(), reason));
			} else {
				m.setBody(String.format("%s rijdt graag met je mee.", b.getPassenger().getName()));
			}
		} else if (m.getBody().startsWith("Voor jouw reisaanvraag")) {
			// Passenger shout-out
			if (m.getBody().contains("niet")) {
				String reason = b.getCancelReason() != null && !b.getCancelReason().isEmpty() ? ": " + b.getCancelReason() : ".";
				m.setBody(String.format("Je kunt helaas niet meer meerijden met %s%s", r.getDriver().getName(), reason));
			} else {
				m.setBody(String.format("Je kunt meerijden met %s.", r.getDriver().getName()));
			}
		} else if (m.getBody().startsWith("Voor de reis")) {
			if (m.getBody().contains("niet")) {
				String reason = b.getCancelReason() != null && !b.getCancelReason().isEmpty() ? ": " + b.getCancelReason() : ".";
				m.setBody(String.format("%s kan helaas niet meer meerijden met %s%s", b.getPassenger().getName(), r.getDriver().getName(), reason));
			} else {
				m.setBody(String.format("%s kan meerijden met %s.", b.getPassenger().getName(), r.getDriver().getName()));
			}
		}
	}

	private void updateTripPlan(Message m) throws BusinessException {
		TripPlan plan = tripPlanManager.getTripPlan(UrnHelper.getId(TripPlan.URN_PREFIX, m.getContext()));
		m.setBody(String.format("%s zoekt vervoer (%s rond %s). Wie kan helpen?", 
				plan.getTraveller().getName(), plan.isUseAsArrivalTime() ? "aankomst" : "vertrek", textHelper.formatTime(plan.getTravelTime())));
	}

	 protected String travelsWith(Set<String> agencies) {
	    	// FIXME Als er geen agency is, dan moet het per voet zijn
	    	String desc = "te voet";
	    	List<String> ags = new ArrayList<>(agencies);
	    	if (ags.size() == 1) {
	    		desc = "met " + ags.get(0);
	    	} else if (ags.size() > 1) {
	    		String last = ags.remove(ags.size() - 1);
	    		desc = "met " + String.join( " en ", String.join(", ", ags), last);
	    	}
	    	return desc;
	    }
	 
	private void updateTrip(Message m) throws NotFoundException, BadRequestException {
		Trip trip = tripManager.getTrip(UrnHelper.getId(Trip.URN_PREFIX, m.getContext()));
		if (m.getBody().startsWith("Vertrek om ")) {
			m.setBody(String.format("Vertrek om %s uur. Je reist %s.", 
					textHelper.formatTime(trip.getItinerary().getDepartureTime()), travelsWith(trip.getAgencies())));
		} else if (m.getBody().startsWith("Heb je de reis")) {
			m.setBody(String.format("Heb je de reis gemaakt? Geef jouw waardering en beoordeel deze reis."));
		} else if (m.getBody().startsWith("Jouw reis op")) {
			m.setBody(String.format("Je reis zit erop! Geef jouw waardering en beoordeel deze reis."));
		}
		
	}
	
	private void updateRide(Message m) throws NotFoundException, BadRequestException {
		//Ride ride = rideManager.getRide(UrnHelper.getId(Ride.URN_PREFIX ,m.getContext()));
		// Seems all right for now
	}

	private void updateDelegation(Message m) throws NotFoundException, BadRequestException {
		// Delegation delegation = delegationManager.getDelegation(UrnHelper.getId(Delegation.URN_PREFIX, m.getContext()), Delegation.PROFILES_ENTITY_GRAPH);
		// Seems all right for now
	}
}
