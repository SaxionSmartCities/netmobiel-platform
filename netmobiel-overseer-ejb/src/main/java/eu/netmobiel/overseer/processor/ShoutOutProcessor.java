package eu.netmobiel.overseer.processor;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.RunAs;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.model.NetMobielUserImpl;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.service.PublisherService;
import eu.netmobiel.planner.event.TravelOfferEvent;
import eu.netmobiel.planner.model.Itinerary;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.service.TripPlanManager;
import eu.netmobiel.profile.api.model.Profile;
import eu.netmobiel.profile.client.ProfileClient;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.BookingState;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.service.BookingManager;
import eu.netmobiel.rideshare.service.RideManager;

/**
 * Stateless bean for the management of the high-level shout-out process, involving multiple modules.
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
public class ShoutOutProcessor {
	private static final String DEFAULT_TIME_ZONE = "Europe/Amsterdam";
	private static final String DEFAULT_LOCALE = "nl-NL";
	private static final int DRIVER_MAX_RADIUS_METERS = 50000;
	private static final int DRIVER_NEIGHBOURING_RADIUS_METERS = 10000;
	
    @Inject
    private PublisherService publisherService;

    @Inject
    private ProfileClient profileService;

    @Inject
    private RideManager rideManager;
    
    @Inject
    private BookingManager bookingManager;

    @Inject
    private TripPlanManager tripPlanManager;

    @Resource
    private SessionContext context;

    private Locale defaultLocale;
    
    @PostConstruct
    public void initialize() {
    	defaultLocale = Locale.forLanguageTag(DEFAULT_LOCALE);
    }

    private String formatDate(Instant instant) {
    	return DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(defaultLocale).format(instant.atZone(ZoneId.of(DEFAULT_TIME_ZONE)));
    }

    private String formatTime(Instant instant) {
    	return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(defaultLocale).format(instant.atZone(ZoneId.of(DEFAULT_TIME_ZONE)));
    }

//    @Asynchronous
    public void onShoutOutRequested(@Observes(during = TransactionPhase.IN_PROGRESS) TripPlan event) throws BusinessException {
    	// We have a shout-out request
		List<Profile> profiles = profileService.searchShoutOutProfiles(event.getFrom(), event.getTo(), DRIVER_MAX_RADIUS_METERS, DRIVER_NEIGHBOURING_RADIUS_METERS);
		if (! profiles.isEmpty()) {
			Message msg = new Message();
			msg.setContext(event.getPlanRef());
			msg.setSubject("Rit gezocht!");
			String travelMoment = event.isUseAsArrivalTime() ? "aankomst" : "vertrek";  
			msg.setBody( 
				MessageFormat.format("{0} zoekt vervoer op {1} ({2} rond {3}) van {4} naar {5}. Wie kan helpen?",
						event.getTraveller().getGivenName(),
						formatDate(event.getTravelTime()),
						travelMoment,
						formatTime(event.getTravelTime()),
						event.getFrom().getLabel(), 
						event.getTo().getLabel() 
						));
//				msg.setBody( 
//					MessageFormat.format("{0} zoekt vervoer op {1} (ergens tussen vertrek {2} en aankomst {3}) van {4} naar {5}. Wie kan helpen?",
//							event.getTraveller().getGivenName(),
//							formatDate(event.getTravelTime()),
//							formatTime(event.getEarliestDepartureTime()),
//							formatTime(event.getLatestArrivalTime()),
//							event.getFrom().getLabel(), 
//							event.getTo().getLabel() 
//							));
			msg.setDeliveryMode(DeliveryMode.NOTIFICATION);
			profiles.forEach(profile -> msg.addRecipient(new NetMobielUserImpl(profile.getId(), profile.getFirstName(), profile.getLastName(), profile.getEmail())));
			publisherService.publish(null, msg);
		}
    }

    /**
     * Handles the TravelOfferEvent. A driver has found an trip plan for himself in which a traveller can take part. 
     * The handler creates a ride for the driver according the trip plan calculated before, adds a booking for the traveller.
     * When is the traveller's itinerary saved?
     * @param event
     * @throws BusinessException
     */
    public void onTravelOfferProposed(@Observes(during = TransactionPhase.IN_PROGRESS) TravelOfferEvent event) 
    		throws BusinessException  {
    	if (! event.getProposedPlan().getTraverseModes().contains(TraverseMode.RIDESHARE)) {
    		throw new BadRequestException("Only RIDESHARE modality is supported");
    	}
    	// We know only one rideshare provider and that's us.
    	if (event.getProposedPlan().getItineraries().size() != 1) {
    		throw new BadRequestException("proposed plan should contain exactly 1 itinerary");
    	}
    	TripPlan pp = event.getProposedPlan();
    	TripPlan sop = event.getShoutOutPlan();
    	Itinerary soi = event.getShoutOutItinerary();
    	Ride r = new Ride();
    	r.setCarRef(event.getVehicleRef());
    	r.setDriverRef(event.getDriverRef());
    	r.setFrom(pp.getFrom());
    	r.setTo(pp.getTo());
    	if (pp.isUseAsArrivalTime()) {
    		r.setArrivalTime(pp.getTravelTime());
    	} else {
    		r.setDepartureTime(pp.getTravelTime());
    	}
    	rideManager.createRide(r);
    	
    	Booking b = new Booking();
    	b.setDepartureTime(soi.getDepartureTime());
    	b.setArrivalTime(soi.getArrivalTime());
    	b.setPickup(soi.getFrom());
    	b.setDropOff(soi.getTo());
    	b.setNrSeats(pp.getNrSeats());
    	b.setState(BookingState.PROPOSED);
		// Which reference do we give to the booking? We do not have a trip yet. We do have a plan and an itinerary.
		// The reference is only needed by the transport provider to inform the planner on a cancel of the ride,
		// or to update some details like the car. Implicitly the reference is used to find the planner (in case of an
		// external service. So the use from the perspective of the transport provider is twofold: 
		// Find the service that booked a ride, and find the specific trip or tripplan within that service.
		
		// As a principle we should not use an itinerary as key. Each change will create a new itinerary. Therefore, we 
		// use a trip (refers to an itinerary) or shout-out tripplan. The latter is only used in case of proposals.
    	// The reference is to the shout-out plan!
    	b.setPassengerTripRef(sop.getPlanRef());
		String bookingRef = bookingManager.createBooking(r.getRideRef(), sop.getTraveller(), b);
		tripPlanManager.assignBookingProposalReference(RideManager.AGENCY_ID, soi, r, bookingRef);

		Message msg = new Message();
		msg.setContext(b.getBookingRef());
		msg.setDeliveryMode(DeliveryMode.NOTIFICATION);
		msg.addRecipient(b.getPassenger());
		msg.setSubject("Je hebt een reisaanbieding!");
		msg.setBody(
				MessageFormat.format("Voor jouw reisaanvraag op {0} naar {1} kun je meerijden met {2}.", 
						formatDate(soi.getDepartureTime()),
						b.getDropOff().getLabel(), 
						r.getDriver().getGivenName()
						)
				);
		publisherService.publish(null, msg);
    }
}
