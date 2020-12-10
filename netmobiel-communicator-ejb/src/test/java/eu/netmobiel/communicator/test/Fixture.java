package eu.netmobiel.communicator.test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import eu.netmobiel.communicator.model.CommunicatorUser;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Envelope;
import eu.netmobiel.communicator.model.Message;

public class Fixture {


	private Fixture() {
		// No instances allowed
	}

	public static CommunicatorUser createUser(String identity, String givenName, String familyName, String email) {
		return new CommunicatorUser(identity, givenName, familyName, email);
	}

    public static Message createMessage(String body, String context, String subject, DeliveryMode mode, String creationTimeIso, CommunicatorUser sender, String ackTimeIso, CommunicatorUser... recipients) {
    	Instant creationTime = Instant.parse(creationTimeIso);
    	Instant ackTime = ackTimeIso != null ? Instant.parse(ackTimeIso) : null; 
    	Message m = new Message();
    	m.setBody(body);
    	m.setContext(context);
    	m.setCreationTime(creationTime);
    	m.setSender(sender);
    	m.setDeliveryMode(mode);
    	m.setSubject(subject);
    	List<Envelope> envelopes = Arrays.stream(recipients)
    			.map(rcp -> new Envelope(m, rcp, mode == DeliveryMode.ALL || mode == DeliveryMode.NOTIFICATION ? creationTime : null, ackTime))
    			.collect(Collectors.toList()); 
    	m.setEnvelopes(envelopes);
    	return m;
    }

}
