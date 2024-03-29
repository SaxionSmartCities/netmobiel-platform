package eu.netmobiel.communicator.test;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;

import eu.netmobiel.communicator.model.CommunicatorUser;
import eu.netmobiel.communicator.model.Conversation;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Envelope;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.model.UserRole;

public class Fixture {


	private Fixture() {
		// No instances allowed
	}

	public static CommunicatorUser createUser(String identity, String givenName, String familyName, String email) {
		return new CommunicatorUser(identity, givenName, familyName, email);
	}

    public static Message createMessage(String body, String context, DeliveryMode mode, String creationTimeIso, Conversation sender, Envelope... rcpEnvelopes) {
    	Instant creationTime = Instant.parse(creationTimeIso);
    	Message.MessageBuilder mb = Message.create()
    	.withBody(body)
    	.withContext(context)
    	.withCreatedTime(creationTime)
    	.withDeliveryMode(mode);
    	Arrays.stream(rcpEnvelopes)
    			.forEach(env -> mb.addEnvelope(env));
    	if (sender != null) {
    		// Chat message
    		mb.withSender(context, sender);
    	}
    	return mb.buildMessage();
    }

    public static Envelope createEnvelope(String context, String pushTimeIso, String ackTimeIso, Conversation recipient) {
    	Instant pushTime = pushTimeIso != null ? Instant.parse(pushTimeIso) : null; 
    	Instant ackTime = ackTimeIso != null ? Instant.parse(ackTimeIso) : null; 
    	Envelope env = new Envelope(context, recipient, pushTime, ackTime);
    	return env;
    }

    public static Conversation createConversation(CommunicatorUser owner, UserRole ownerRole, String topic, String creationTimeIso, String archiveTimeIso, String... contexts) {
    	Instant creationTime = Instant.parse(creationTimeIso);
    	Instant archiveTime = archiveTimeIso != null ? Instant.parse(archiveTimeIso) : null; 
    	Conversation c = new Conversation(owner, ownerRole, null, topic, creationTime);
    	c.setArchivedTime(archiveTime);
    	c.setContexts(new HashSet<>(Arrays.asList(contexts)));
    	return c;
    }
}
