package eu.netmobiel.rideshare.service;

import java.text.MessageFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.annotation.Created;
import eu.netmobiel.commons.annotation.Removed;
import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.communicator.model.Conversation;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.service.PublisherService;
import eu.netmobiel.rideshare.model.Booking;
import eu.netmobiel.rideshare.model.BookingState;
import eu.netmobiel.rideshare.model.Ride;

/**
 * Helper class to send notifications to the driver. 
 * The reason to use the CDI events for publishing notifications is to facilitate testing.
 * The test setup will use its own observers and test the receipt of the correct event. The actual publishing is 
 * difficult to test in an integration test due to use of external services.
 * 
 * @author Jaap Reitsma
 *
 */
@ApplicationScoped
@Logging
public class NotificationHelper {
	private static final String DEFAULT_TIME_ZONE = "Europe/Amsterdam";
	private static final String DEFAULT_LOCALE = "nl-NL";

    @Inject
    private PublisherService publisherService;

    @SuppressWarnings("unused")
	@Inject
	private Logger logger;

    private Locale defaultLocale;
    
    @PostConstruct
    public void initialize() {
    	defaultLocale = Locale.forLanguageTag(DEFAULT_LOCALE);
    }

    protected Message createMessage(Booking booking, Conversation driverConversation, String subject, String messageText) {
		Ride ride = booking.getRide();
    	Message msg = new Message();
		msg.setContext(booking.getRide().getUrn());
		msg.setDeliveryMode(DeliveryMode.NOTIFICATION);
		msg.addRecipient(driverConversation, booking.getUrn());
		msg.setSubject(subject);
		msg.setBody(
				MessageFormat.format(messageText, 
						DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(defaultLocale).format(ride.getDepartureTime().atZone(ZoneId.of(DEFAULT_TIME_ZONE))),
						ride.getTo().getLabel(), 
						booking.getPassenger().getGivenName()
						)
				);
		return msg;
    }

    public void onBookingCreated(@Observes(during = TransactionPhase.AFTER_SUCCESS) @Created Booking booking) throws BusinessException {
		// Inform driver on new booking
		Message msg = null;
		// Add the booking context to the ride context
		Conversation driverConv = publisherService.lookupConversation(booking.getRide().getDriver(), booking.getRide().getUrn());
		publisherService.addConversationContexts(driverConv, new String[] { booking.getUrn() });
		if (booking.getState() == BookingState.PROPOSED) {
			// No message is needed, because is is the drive who created the proposal
		} else if (booking.getState() == BookingState.REQUESTED) {
			msg = createMessage(booking, driverConv, "Je hebt een passagier!", 
					"Voor jouw rit op {0} naar {1} wil {2} graag met je mee.");
		} else if (booking.getState() == BookingState.CONFIRMED) {
			msg = createMessage(booking, driverConv, "Je hebt een passagier!", 
					"Voor jouw rit op {0} naar {1} rijdt {2} met je mee.");
		} else {
			throw new IllegalStateException("Unexpected booking state with booking " + booking.toString());
		}
		if (msg != null) {
			publisherService.publish(null, msg);
		}
	}

	public void onBookingRemoved(@Observes(during = TransactionPhase.AFTER_SUCCESS) @Removed Booking booking) throws BusinessException {
		// Inform driver about removal of a booking
		Message msg = null;
		Conversation driverConv = publisherService.lookupConversation(booking.getRide().getDriver(), booking.getRide().getUrn());
		if (booking.getState() == BookingState.PROPOSED) {
			msg = createMessage(booking, driverConv, "Passagier heeft geannuleerd.", 
					"{2} heeft een andere oplossing gevonden voor de rit op {0} naar {1}. Bedankt voor je aanbod!");
		} else {
			msg = createMessage(booking, driverConv, "Passagier heeft geannuleerd.", 
					"Voor jouw rit op {0} naar {1} rijdt {2} niet meer mee.");
		}
		publisherService.publish(null, msg);
	}
}
