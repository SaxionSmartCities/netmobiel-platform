package eu.netmobiel.overseer.processor;

import java.text.MessageFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.RunAs;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.NetMobielModule;
import eu.netmobiel.commons.exception.ApplicationException;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.CreateException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.UpdateException;
import eu.netmobiel.commons.model.event.BookingCancelledEvent;
import eu.netmobiel.commons.model.event.BookingRequestedEvent;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.commons.util.UrnHelper;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.service.PublisherService;
import eu.netmobiel.planner.model.Leg;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.service.TripManager;
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
    private BookingManager bookingManager;

    @Inject
    private TripManager tripManager;

    @Resource
    private SessionContext context;

    @Inject
    private Logger logger;
    
    private Locale defaultLocale;
    
    @PostConstruct
    public void initialize() {
    	defaultLocale = Locale.forLanguageTag(DEFAULT_LOCALE);
    }
    
//    @Asynchronous
    public void onBookingRequestedEvent(@Observes(during = TransactionPhase.IN_PROGRESS) BookingRequestedEvent event) {
    	if (!NetMobielModule.RIDESHARE.getCode().equals(UrnHelper.getService(event.getProviderTripRef()))) {
    		logger.error("Unsupported service: " + event.getProviderTripRef());
    	}
    	// It is a Rideshare service booking
    	Booking b = new Booking();
    	b.setArrivalTime(event.getArrivalTime());
    	b.setDepartureTime(event.getDepartureTime());
    	b.setDropOff(event.getDropOff());
    	b.setNrSeats(event.getNrSeats());
    	b.setPassengerTripRef(event.getTravellerTripRef());
    	b.setPickup(event.getPickup());
    	try {
			String bookingRef = bookingManager.createBooking(event.getProviderTripRef(), event.getTraveller(), b);
			// Check whether auto conform is enabled or that we have long conversation
			Booking bdb = bookingManager.getBooking(UrnHelper.getId(Booking.URN_PREFIX, bookingRef));
			// Assign the booking reference to the trip leg
			boolean autoConfirmed = bdb.getState() == BookingState.CONFIRMED;
			tripManager.assignBookingReference(event.getTravellerTripRef(), event.getProviderTripRef(), bookingRef, autoConfirmed);
			if (bdb.getState() != BookingState.CONFIRMED) {
				logger.warn("Expecting booking AutoConfirm! Other situations are not handled!");
			}
		} catch (CreateException | NotFoundException | BadRequestException| UpdateException e) {
			logger.error("Unable to create a booking: " + e.toString());
			context.setRollbackOnly();
		}
    }

    /**
     * Signals the removal of a booking. The event can be produced by the transport provider or by the Trip Manager of netMobiel.
     * 
     * @param event
     */
    public void onBookingCancel(@Observes(during = TransactionPhase.IN_PROGRESS) BookingCancelledEvent event) {
    	logger.info(String.format("Booking %s cancelled from %s by %s because '%s'", 
    			event.getBookingRef(),
    			event.isCancelledFromTransportProvider() ? "Transport Provider" : "NetMobiel",
    			event.isCancelledByDriver() ? "Driver" : "Passenger",
    			event.getCancelReason() != null ? event.getCancelReason() : "---"));
    	try {
    		if (event.isCancelledFromTransportProvider()) {
    			// The booking is cancelled by rideshare
    			tripManager.cancelBooking(event.getTravellerTripRef(), event.getBookingRef(), event.getCancelReason(), event.isCancelledByDriver());
    			Trip trip = tripManager.getTrip(UrnHelper.getId(Trip.URN_PREFIX, event.getTravellerTripRef()));
    			Leg leg = trip.getItinerary().findLegByBookingId(event.getBookingRef())
    					.orElseThrow(() -> new NotFoundException("No such booking in leg: " + event.getBookingRef()));
    			if (event.isCancelledByDriver()) {
    				// Notify the passenger
    				Message msg = new Message();
    				msg.setContext(event.getBookingRef());
    				msg.setSubject("Chauffeur heeft geannuleerd.");
    				msg.setBody(
    						MessageFormat.format("Voor jouw reis op {0} naar {1} kun je helaas meer met {2} meerijden.", 
    								DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(defaultLocale).format(trip.getItinerary().getDepartureTime().atZone(ZoneId.of(DEFAULT_TIME_ZONE))),
    								trip.getTo().getLabel(), 
    								leg.getDriverName()
    								)
    						);
    				msg.setDeliveryMode(DeliveryMode.NOTIFICATION);
    				msg.addRecipient(event.getTraveller());
					publisherService.publish(null, msg);
    			} else {
    				// Notification of the driver is done by transport provider
    			}
    		} else {
    			// The booking is cancelled through the Trip Manager
        		bookingManager.removeBooking(event.getBookingRef(), event.getCancelReason(), event.isCancelledByDriver(), false);
    		}
    		
		} catch (ApplicationException e) {
			logger.error("Error cancelling booking: " + e.toString());
			// Do not rollback, just proceed with the cancelling, probably that was some inconsistency.
//			context.setRollbackOnly();
		}
    	
    }

}
