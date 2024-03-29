package eu.netmobiel.firebase.messaging;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.exception.SystemException;
import eu.netmobiel.commons.model.NetMobielMessage;

/**
 * Client for Google Firebase messaging.
 * 
 * @author Jaap Reitsma
 *
 */
@ApplicationScoped
public class FirebaseMessagingClient {
	public final static String SYSTEM_USER_NAME = "Netmobiel";
	/**
	 * Maximum lifetime of a Firebase Cloud Messaging Token 
	 */
	public static final int MAX_TTL_FCM_TOKEN = 60;
	
    @Inject
    private Logger log;

    /**
     * The path to the service account file for Firebird.
     */
    @Resource(lookup = "java:global/firebase/credentialsPath")
    private String firebaseCredentialsPath;

    /**
     * The app 
     */
    private FirebaseApp app;
    /**
     * The messaging client
     */
    private FirebaseMessaging client;

    /**
     * Initializes the Firebase Messaging client. 
     */
    @PostConstruct
    void initialize() {
    	try (final InputStream is = Files.newInputStream(Paths.get(firebaseCredentialsPath))) {
        	FirebaseOptions options = new FirebaseOptions.Builder()
        		    .setCredentials(GoogleCredentials.fromStream(is))
        		    .setDatabaseUrl("https://netmobiel-push.firebaseio.com/")
        		    .build();

        	app = FirebaseApp.initializeApp(options);
        	client = FirebaseMessaging.getInstance(app);
    	} catch (IOException ex) {
    		log.error("Error opening " + firebaseCredentialsPath, ex);
		}
    }

    /**
     * Finalizes the client. Prabably not needed, just to be sure.
     */
    @PreDestroy
    void cleanup() {
    	if (app != null) {
        	app.delete();
    	}
    	app = null;
    	client = null;
    }

	/**
	 * Checks whether the FCM token is probably state. 
	 * @see https://firebase.google.com/docs/cloud-messaging/manage-tokens
	 * @return if true don't use this token any more.
	 */
	public static boolean isFcmTokenProbablyStale(Instant tokenTimestamp) {
		boolean stale = false;
		if (tokenTimestamp != null) {
			Duration untouched = Duration.between(tokenTimestamp, Instant.now());
			stale = untouched.toDays() >= MAX_TTL_FCM_TOKEN;
		}
		return stale;
	}
	
    /**
     * Checks the state of the client. When the client is missing an exception is thrown.
     */
    private void sanityCheck() {
    	if (client == null) {
    		throw new IllegalStateException("Firebase has not properly been initialized, cannot send messages.");
    	}
    }

    private static String getTitle(NetMobielMessage msg) {
    	return msg.getSender() != null ? msg.getSender().getName() : SYSTEM_USER_NAME;
    }
    
    /**
     * Sends a single message to a recipient.
     * @param firebaseToken the firebase token of the recipient.
     * @param msg The message to send.
     * @throws NotFoundException 
     */
    public void send(String firebaseToken, NetMobielMessage msg) throws NotFoundException, BadRequestException {
    	send(firebaseToken, msg, false);
    }

    protected Map<String, String> createCustomDataMap(NetMobielMessage msg) {
    	Map<String, String> map = new LinkedHashMap<>();
        map.put("messageRef", msg.getUrn());
    	return map;
    }

    /**
     * Sends a single message to a recipient.
     * @param firebaseToken the firebase token of the recipient.
     * @param msg The message to send.
     * @param dryRun a boolean indicating whether to perform a dry run (validation only) of the send.
     * @throws NotFoundException The token was not registered at FCM
     * @throws BadRequestException 
     */
    public void send(String firebaseToken, NetMobielMessage msg, boolean dryRun) throws NotFoundException, BadRequestException {
    	sanityCheck();
    	if (firebaseToken == null || firebaseToken.isBlank()) {
    		throw new IllegalArgumentException("No FCM token set");
    	}
    	// This registration token comes from the client FCM SDKs.
	    // See documentation on defining a message payload.
	    Notification notification = Notification.builder()
	    		.setTitle(getTitle(msg))
	    		.setBody(msg.getBody())
	    		.build();
	    Message message = Message.builder()
		        .setToken(firebaseToken)
			    .setNotification(notification)
		        .putAllData(createCustomDataMap(msg))
		        .build();
		try {
		    // Send a message to the device corresponding to the provided registration token.
			String response = client.send(message, dryRun);
		    // Response is a message ID string.
			if (log.isDebugEnabled()) {
				log.debug("Message sent: " + response);
			}
		} catch (FirebaseMessagingException e) {
	    	if ("registration-token-not-registered".equals(e.getErrorCode())) {
	    		throw new NotFoundException("Registration token not registered");
	    	} else if ("invalid-argument".equals(e.getErrorCode())) {
	    		throw new BadRequestException("Registration token is invalid");
	    	}
			throw new SystemException("Failed to send message, error code " + e.getErrorCode(), e);
		}
    }

    /**
     * Sends a single message to multiple recipients (at most 500). NOT TESTED. Has invalid response, 
     * should check for fail responses and delete invalid FCM tokens.
     * @param firebaseTokens the firebase tokens of the recipients.
     * @param msg The message to send.
     */
    public void send(Collection<String> firebaseTokens, NetMobielMessage msg) {
    	send(firebaseTokens, msg, false);
    }

    /**
     * Sends a single message to multiple recipients (at most 500).
     * @param firebaseTokens the firebase tokens of the recipients.
     * @param msg The message to send.
     * @param dryRun a boolean indicating whether to perform a dry run (validation only) of the send.
     */
    public void send(Collection<String> firebaseTokens, NetMobielMessage msg, boolean dryRun) {
    	sanityCheck();
	    Notification notification = Notification.builder()
	    		.setTitle(getTitle(msg))
	    		.setBody(msg.getBody())
	    		.build();
    	MulticastMessage message = MulticastMessage.builder()
        	    .addAllTokens(firebaseTokens)
    		    .setNotification(notification)
		        .putAllData(createCustomDataMap(msg))
    	    .build();
		try {
	    	BatchResponse response = client.sendMulticast(message, dryRun);
	    	// See the BatchResponse reference documentation for the contents of response.
			if (log.isDebugEnabled()) {
				log.debug(String.format("Messages sent: #%d, failed #%d", response.getSuccessCount(), response.getFailureCount()));
			}
		} catch (FirebaseMessagingException e) {
			throw new SystemException("Failed to send batch message, error code " + e.getErrorCode(), e);
		}
    }

    /**
     * Publishes a single message to a topic.
     * @param fcmTopic the name of the FCM topic to which the message should be sent.
     * @param msg The message to send.
     */
    public void publish(String fcmTopic, NetMobielMessage msg) {
    	publish(fcmTopic, msg, false);
    }

    /**
     * Sends a single message to a topic.
     * @param fcmTopic the name of the FCM topic to which the message should be sent.
     * @param msg The message to send.
     * @param dryRun a boolean indicating whether to perform a dry run (validation only) of the send.
     */
    public void publish(String fcmTopic, NetMobielMessage msg, boolean dryRun) {
    	sanityCheck();
    	if (fcmTopic == null || fcmTopic.isBlank()) {
    		throw new IllegalArgumentException("No FCM topic set");
    	}
    	// The topic name can be optionally prefixed with "/topics/".
	    Notification notification = Notification.builder()
	    		.setTitle(msg.getSender() != null ? msg.getSender().getName() : null)
	    		.setBody(msg.getBody())
	    		.build();
	    Message message = Message.builder()
		        .setTopic(fcmTopic)
			    .setNotification(notification)
		        .putAllData(createCustomDataMap(msg))
		        .build();
		try {
	    	// Send a message to the devices subscribed to the provided topic.
	    	String response = client.send(message, dryRun);
	    	// Response is a message ID string.
			if (log.isDebugEnabled()) {
				log.debug(String.format("Message sent to topic %s: %s", fcmTopic, response));
			}
		} catch (FirebaseMessagingException e) {
			throw new SystemException("Failed to send topic message, error code " + e.getErrorCode(), e);
		}
    }
}
