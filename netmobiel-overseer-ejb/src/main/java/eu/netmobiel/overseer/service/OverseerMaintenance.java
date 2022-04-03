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
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.service.PublisherService;
import eu.netmobiel.overseer.processor.TextHelper;
import eu.netmobiel.overseer.processor.UserProcessor;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.service.TripManager;
import eu.netmobiel.planner.service.TripPlanManager;
import eu.netmobiel.profile.filter.ProfileFilter;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.service.ProfileManager;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.service.BookingManager;

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
	@Inject
    private Logger log;

	@Inject
	private ProfileManager profileManager; 
	@Inject
	private PublisherService publisherService;
	@Inject
	private BookingManager bookingManager; 
	@Inject
    private TripPlanManager tripPlanManager;
	@Inject
    private TripManager tripManager;
    @Inject
    private TextHelper textHelper;

    @Inject
    private UserProcessor userProcessor;
    
	@PostConstruct
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public void initialize() {
		log.info("Starting up the Overseer, checking for maintenance tasks");
    	syncNetmobielUsers();
//    	updateMessageBody();
	}

	private void syncNetmobielUsers() {
		try {
			ProfileFilter filter = new ProfileFilter();
			Cursor cursor = new Cursor(100, 0);
			log.info("Start syncing the user data");
			Long total = null;
			while (true) {
				PagedResult<Profile> profiles = profileManager.listProfiles(filter, cursor);
				total = profiles.getTotalCount();
				for (Profile p : profiles.getData()) {
					userProcessor.syncAllUserDatabases(p);
				}
				if (profiles.getCount() < cursor.getMaxResults()) {
					break;
				}
				cursor.next();
			}
			log.info("Done syncing the users: #" + total);
		} catch (BusinessException e) {
			log.error("Error synchronizing users", e);
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
//			cursor.next();
//		}
//		return profileMap;
//	}

	
	@SuppressWarnings("unused")
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
