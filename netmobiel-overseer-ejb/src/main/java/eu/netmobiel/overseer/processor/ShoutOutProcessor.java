package eu.netmobiel.overseer.processor;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.annotation.security.RunAs;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.ApplicationException;
import eu.netmobiel.commons.model.NetMobielUserImpl;
import eu.netmobiel.commons.model.event.ShoutOutRequestedEvent;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.service.PublisherService;
import eu.netmobiel.profile.api.model.Profile;
import eu.netmobiel.profile.client.ProfileClient;

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
    private Logger logger;
    
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
    public void onShoutOutRequested(@Observes(during = TransactionPhase.IN_PROGRESS) ShoutOutRequestedEvent event) {
    	// We have a shout-out request
    	try {
			List<Profile> profiles = profileService.searchShoutOutProfiles(event.getPickup(), event.getDropOff(), DRIVER_MAX_RADIUS_METERS, DRIVER_NEIGHBOURING_RADIUS_METERS);
			if (! profiles.isEmpty()) {
				Message msg = new Message();
				msg.setContext(event.getTravellerTripRef());
				msg.setSubject("Rit gezocht!");
				msg.setBody( 
					MessageFormat.format("{0} zoekt vervoer op {1} (ergens tussen vertrek {2} en aankomst {3}) van {4} naar {5}. Wie kan helpen?",
							event.getTraveller().getGivenName(),
							formatDate(event.getDepartureTime()),
							formatTime(event.getDepartureTime()),
							formatTime(event.getArrivalTime()),
							event.getPickup().getLabel(), 
							event.getDropOff().getLabel() 
							));
				msg.setDeliveryMode(DeliveryMode.NOTIFICATION);
				profiles.forEach(profile -> msg.addRecipient(new NetMobielUserImpl(profile.getId(), profile.getFirstName(), profile.getLastName(), profile.getEmail())));
				publisherService.publish(null, msg);
			}

		} catch (ApplicationException e) {
			logger.error("Unable to obtain nearby driver profiles: " + e.toString());
		}
    }

}
