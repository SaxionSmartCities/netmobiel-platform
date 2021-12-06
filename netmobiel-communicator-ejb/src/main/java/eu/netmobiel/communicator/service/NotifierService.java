package eu.netmobiel.communicator.service;

import java.time.Instant;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.util.ExceptionUtil;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Envelope;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.repository.MessageDao;
import eu.netmobiel.firebase.messaging.FirebaseMessagingClient;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.service.ProfileManager;

/**
 * Bean class for Publisher enterprise bean. 
 */
@Stateless
@Logging
public class NotifierService {

    @Inject
    private Logger logger;

    @Inject
    private ProfileManager profileManager;
    

    @Inject
    private FirebaseMessagingClient firebaseMessagingClient;

    @Inject
    private MessageDao messageDao;

    /**
     * Sends notifications to each recipient in the message.
     * FIXME This method does not save the push time properly. Reason not known yet. DO NOT USE.
     * @param msg the message with the envelopes
     * @throws NotFoundException 
     */
    @Asynchronous
    public void sendNotification(Message msg) throws NotFoundException {
		// Send each user a notification, if required
		if (msg.getDeliveryMode() == DeliveryMode.NOTIFICATION || msg.getDeliveryMode() == DeliveryMode.ALL) {
			// Assure presence in persistence context
	    	Message msgdb = messageDao.loadGraph(msg.getId(), Message.MESSAGE_ENVELOPES_ENTITY_GRAPH)
	    			.orElseThrow(() -> new NotFoundException("No such message: " + msg.getId())); 
			for (Envelope env : msgdb.getEnvelopes()) {
				try {
					Profile profile = profileManager.getFlatProfileByManagedIdentity(env.getRecipient().getManagedIdentity());
					if (profile.getFcmToken() == null || profile.getFcmToken().isBlank()) {
						logger.warn(String.format("Cannot send push notification to %s (%s): No FCM token set", 
								profile.getManagedIdentity(), profile.getName()));  
					} else {
						firebaseMessagingClient.send(profile.getFcmToken(), msgdb);
						env.setPushTime(Instant.now());
					}
				} catch (Exception ex) {
					logger.error(String.format("Cannot send push notification to %s: %s", 
							env.getRecipient().getManagedIdentity(), String.join("\n\t", ExceptionUtil.unwindException(ex))));
				}
			}
		}
    }

}
