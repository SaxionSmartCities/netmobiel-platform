package eu.netmobiel.rideshare.service;

import java.text.MessageFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.annotation.Created;
import eu.netmobiel.commons.annotation.Removed;
import eu.netmobiel.commons.util.Logging;
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

    @Inject
    private PublisherService publisherService;

    @Inject
	private Logger logger;

    protected Message createMessage(Booking booking, String subject, String messageText) {
		Ride ride = booking.getRide();
    	Message msg = new Message();
		msg.setContext(booking.getBookingRef());
		msg.setDeliveryMode(DeliveryMode.NOTIFICATION);
		msg.addRecipient(ride.getDriver());
		msg.setSubject(subject);
		msg.setBody(
				MessageFormat.format(messageText, 
						DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(ride.getDepartureTime().atZone(ZoneId.of(DEFAULT_TIME_ZONE))),
						ride.getTo().getLabel(), 
						booking.getPassenger().getGivenName()
						)
				);
		return msg;
    }

    public void onBookingCreated(@Observes(during = TransactionPhase.AFTER_SUCCESS) @Created Booking booking) {
		// Inform driver on new booking
		try {
			Message msg = null;
			if (booking.getState() == BookingState.REQUESTED) {
				msg = createMessage(booking, "Je hebt een passagier!", "Voor jouw rit op {0} naar {1} wil {2} graag met je mee.");
			} else if (booking.getState() == BookingState.CONFIRMED) {
				msg = createMessage(booking, "Je hebt een passagier!", "Voor jouw rit op {0} naar {1} rijdt {2} met je mee.");
			} else {
				throw new IllegalStateException("Unexpected booking state with booking " + booking.toString());
			}
			publisherService.publish(null, msg);
		} catch (Exception e) {
			logger.error("Unable to inform driver on new booking: " + e.toString());
		}
	}

	public void onBookingRemoved(@Observes(during = TransactionPhase.AFTER_SUCCESS) @Removed Booking booking) {
		// Inform driver about removal of a booking
		try {
			Message msg = createMessage(booking, "Passagier heeft geannuleerd.", "Voor jouw rit op {0} naar {1} rijdt {2} niet meer mee.");
			publisherService.publish(null, msg);
		} catch (Exception e) {
			logger.error("Unable to inform driver on cancelled booking: " + e.toString());
		}
	}
}