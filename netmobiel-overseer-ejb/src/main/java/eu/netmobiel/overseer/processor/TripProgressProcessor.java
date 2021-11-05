package eu.netmobiel.overseer.processor;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.RunAs;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.communicator.model.Conversation;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.service.PublisherService;
import eu.netmobiel.planner.event.TripStateUpdatedEvent;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.TripState;
import eu.netmobiel.rideshare.event.RideStateUpdatedEvent;
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
	private static final String DEFAULT_TIME_ZONE = "Europe/Amsterdam";
	private static final String DEFAULT_LOCALE = "nl-NL";
	
    @Inject
    private PublisherService publisherService;

    @Resource
    private SessionContext context;

    private Locale defaultLocale;
    
    @PostConstruct
    public void initialize() {
    	defaultLocale = Locale.forLanguageTag(DEFAULT_LOCALE);
    }

    @SuppressWarnings("unused")
	private String formatDate(Instant instant) {
    	return DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(defaultLocale).format(instant.atZone(ZoneId.of(DEFAULT_TIME_ZONE)));
    }

    private String formatTime(Instant instant) {
    	return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(defaultLocale).format(instant.atZone(ZoneId.of(DEFAULT_TIME_ZONE)));
    }

    public void onTripStateChange(@Observes(during = TransactionPhase.IN_PROGRESS) TripStateUpdatedEvent event) 
    		throws BusinessException {
    	switch (event.getTrip().getState()) {
    	case PLANNING:
    		break;
    	case BOOKING:
    		break;
    	case SCHEDULED:
    		break;
    	case DEPARTING:
    		if (event.getPreviousState() == TripState.SCHEDULED) {
    			informTravellerOnDeparture(event.getTrip());
    		}
    		break;
    	case IN_TRANSIT:
    		break;
    	case ARRIVING:
    		break;
    	case VALIDATING:
    		if (event.getPreviousState() == TripState.ARRIVING) {
    			informTravellerOnReview(event.getTrip());
    		} else if (event.getPreviousState() == TripState.VALIDATING) {
    			remindTravellerOnReview(event.getTrip());
    		}
    		break;
    	case COMPLETED:
    		break;
    	case CANCELLED:
    		break;
    	default:
    		break;
    	}
//    	try {
//    		
//		} catch (ApplicationException e) {
//			logger.error("Unable to obtain nearby driver profiles: " + e.toString());
//		}
    }

    public void onRideStateChange(@Observes(during = TransactionPhase.IN_PROGRESS) RideStateUpdatedEvent event) 
    		throws BusinessException {
    	Ride ride = event.getRide();
    	switch (ride.getState()) {
    	case SCHEDULED:
    		break;
    	case DEPARTING:
    		if (event.getPreviousState() == RideState.SCHEDULED && ride.hasConfirmedBooking()) {
    			informDriverOnDeparture(event.getRide());
    		}
    		break;
    	case IN_TRANSIT:
    		break;
    	case ARRIVING:
    		break;
    	case VALIDATING:
    		if (ride.hasConfirmedBooking()) {
	    		if (event.getPreviousState() == RideState.ARRIVING) {
	    			informDriverOnReview(ride);
	    		} else if (event.getPreviousState() == RideState.VALIDATING) {
	    			remindDriverOnReview(ride);
	    		}
    		}
    		break;
    	case COMPLETED:
    		break;
    	case CANCELLED:
    		break;
    	default:
    		break;
    	}
//    	try {
//    		
//		} catch (ApplicationException e) {
//			logger.error("Unable to obtain nearby driver profiles: " + e.toString());
//		}
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

    protected void informTravellerOnDeparture(Trip trip) throws BusinessException {
		Conversation passengerConv = publisherService.lookupConversation(trip.getTraveller(), trip.getTripRef());
		Message msg = new Message();
		msg.setContext(trip.getTripRef());
		msg.setDeliveryMode(DeliveryMode.ALL);
		msg.addRecipient(passengerConv, trip.getTripRef());
		msg.setSubject("Je gaat bijna op pad!");
		msg.setBody(
				MessageFormat.format("Vertrek om {0} uur naar {1}. Je reist {2}.", 
						formatTime(trip.getItinerary().getDepartureTime()),
						trip.getTo().getLabel(), 
						travelsWith(trip.getAgencies())
						)
				);
		publisherService.publish(null, msg);
		// Inform the delegates, if any
		publisherService.informDelegates(trip.getTraveller(), 
				MessageFormat.format("{0} gaat bijna op pad!", trip.getTraveller().getName()), 
				DeliveryMode.ALL);
	}

	protected void informTravellerOnReview(Trip trip) throws BusinessException {
		Conversation passengerConv = publisherService.lookupConversation(trip.getTraveller(), trip.getTripRef());
		Message msg = new Message();
		msg.setContext(trip.getTripRef());
		msg.setDeliveryMode(DeliveryMode.ALL);
		msg.addRecipient(passengerConv, trip.getTripRef());
		msg.setSubject("Jouw reis zit erop!");
		msg.setBody(
				MessageFormat.format("Heb je de reis naar {0} gemaakt? Geef jouw waardering en beoordeel deze reis.", 
						trip.getTo().getLabel()
						)
				);
		publisherService.publish(null, msg);
		// Inform the delegates, if any
		publisherService.informDelegates(trip.getTraveller(), 
				MessageFormat.format("De reis van {0} zit erop, geef een beoordeling", trip.getTraveller().getName()), 
				DeliveryMode.ALL);
	}

	protected void remindTravellerOnReview(Trip trip) throws BusinessException {
		Conversation passengerConv = publisherService.lookupConversation(trip.getTraveller(), trip.getTripRef());
		Message msg = new Message();
		msg.setContext(trip.getTripRef());
		msg.setDeliveryMode(DeliveryMode.ALL);
		msg.addRecipient(passengerConv, trip.getTripRef());
		msg.setSubject("Beoordeel jouw reis!");
		msg.setBody(
				MessageFormat.format("Jouw reis op {0} naar {1} zit erop! Geef jouw waardering en beoordeel deze reis.", 
						formatTime(trip.getItinerary().getDepartureTime()),
						trip.getTo().getLabel()
						)
				);
		publisherService.publish(null, msg);
		// Inform the delegates, if any
		publisherService.informDelegates(trip.getTraveller(), 
				MessageFormat.format("Herhaling: De reis van {0} zit erop, geef een beoordeling", trip.getTraveller().getName()), 
				DeliveryMode.ALL);
}
	
    protected void informDriverOnDeparture(Ride ride) throws BusinessException {
		Booking b = ride.getConfirmedBooking().orElseThrow(() -> new IllegalStateException("Expected a confirmed booking for ride:" + ride.getId()));
		Conversation driverConv = publisherService.lookupConversation(ride.getDriver(), ride.getUrn());
		Message msg = new Message();
		msg.setContext(ride.getUrn());
		msg.setDeliveryMode(DeliveryMode.ALL);
		msg.addRecipient(driverConv, b.getUrn());
		msg.setSubject("Je gaat bijna op pad!");
		msg.setBody(
				MessageFormat.format("Vertrek om {0} uur naar {1}. Je wordt verwacht door {2}.", 
						formatTime(ride.getDepartureTime()),
						ride.getTo().getLabel(), 
						b.getPassenger().getGivenName()
						)
				);
		publisherService.publish(null, msg);
	}

	protected void informDriverOnReview(Ride ride) throws BusinessException {
		Booking b = ride.getConfirmedBooking().orElseThrow(() -> new IllegalStateException("Expected a confirmed booking for ride:" + ride.getId()));
		Conversation driverConv = publisherService.lookupConversation(ride.getDriver(), ride.getUrn());
		Message msg = new Message();
		msg.setContext(ride.getUrn());
		msg.setDeliveryMode(DeliveryMode.ALL);
		msg.addRecipient(driverConv, b.getUrn());
		msg.setSubject("Jouw rit zit erop!");
		msg.setBody(
				MessageFormat.format("Heb je {0} meegenomen naar {1}? Claim je credits en beoordeel je passagier!", 
						b.getPassenger().getGivenName(),
						ride.getTo().getLabel()
						)
				);
		publisherService.publish(null, msg);
	}

	protected void remindDriverOnReview(Ride ride) throws BusinessException {
		Booking b = ride.getConfirmedBooking().orElseThrow(() -> new IllegalStateException("Expected a confirmed booking for ride:" + ride.getId()));
		Conversation driverConv = publisherService.lookupConversation(ride.getDriver(), ride.getUrn());
		Message msg = new Message();
		msg.setContext(ride.getUrn());
		msg.setDeliveryMode(DeliveryMode.ALL);
		msg.addRecipient(driverConv, b.getUrn());
		msg.setSubject("Jouw rit zit erop!");
		msg.setBody("Claim je credits en beoordeel je passagier!");
		publisherService.publish(null, msg);
	}
	
}
