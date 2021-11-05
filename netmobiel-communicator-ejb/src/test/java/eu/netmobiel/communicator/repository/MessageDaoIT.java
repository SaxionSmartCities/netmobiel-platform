package eu.netmobiel.communicator.repository;


import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Envelope;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.test.CommunicatorIntegrationTestBase;
import eu.netmobiel.communicator.test.Fixture;

@RunWith(Arquillian.class)
public class MessageDaoIT extends CommunicatorIntegrationTestBase {
    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive archive = createDeploymentBase()
            .addClass(MessageDao.class);
//		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private MessageDao messageDao;
    
    @Inject
    private Logger log;
    
    @SuppressWarnings("unused")
	private void dump(String subject, Collection<Message> messages) {
    	messages.forEach(m -> log.info(subject + ": " + m.toString()));
    }
    
    @Test
    public void saveMessage() {
		Message message = Fixture.createMessage("Het is tijd om te vertrekken voor Trip P2.1", "Trip P2.1", "De reis begint", DeliveryMode.MESSAGE, "2020-02-12T12:00:00Z", null, 
        		new Envelope("Trip P2.1", convP2_1));
    	messageDao.save(message);
    	List<Message> actual = em.createQuery("select m from Message m where m.body = :body", Message.class)
        		.setParameter("body", message.getBody())
        		.getResultList();
    	assertNotNull(actual);
    	assertEquals(1, actual.size());
    }

    @Test
    public void listMessages_All() {
    	PagedResult<Long> messageIds = messageDao.listMessages(null, null, null, null, null, 100, 0);
    	List<Message> messages = messageDao.loadGraphs(messageIds.getData(), null, Message::getId);
    	Long expCount = em.createQuery("select count(m) from Message m", Long.class).getSingleResult();
    	assertEquals("All messages present", Math.toIntExact(expCount) , messages.size());
    	assertNull("No total count", messageIds.getTotalCount());
    	assertEquals("Offset matches", 0, messageIds.getOffset());
    	assertEquals("maxResults matches", 100, messageIds.getResultsPerPage());
    	assertEquals("Result count matches", expCount.longValue(), messageIds.getCount());
    }

    @Test
    public void listMessages_AllCount() {
    	PagedResult<Long> messageIds = messageDao.listMessages(null, null, null, null, null, 100, 0);
    	Long expCount = em.createQuery("select count(m) from Message m", Long.class).getSingleResult();
    	assertNull("No total count", messageIds.getTotalCount());
    	assertEquals("All messages present", expCount.longValue() , messageIds.getCount());
    	assertEquals("Offset matches", 0, messageIds.getOffset());
    	assertEquals("maxResults matches", 100, messageIds.getResultsPerPage());
    }

    @Test
    public void listMessages_ByParticipant() {
    	final String participant = userP1.getManagedIdentity();
    	PagedResult<Long> messageIds = messageDao.listMessages(participant, null, null, null, null, 100, 0);
    	List<Message> messages = messageDao.loadGraphs(messageIds.getData(), Message.LIST_MY_MESSAGES_ENTITY_GRAPH, Message::getId);
    	for (Message message : messages) {
			// The participant is one of the recipients or is the sender of the message
        	Set<String> recipients = message.getEnvelopes().stream().map(env -> env.getConversation().getOwner().getManagedIdentity()).collect(Collectors.toSet());
        	assertTrue("Must be sender of recipient", recipients.contains(participant));
		}
    	Long expCount = em.createQuery(
        		"select count(m) from Message m join m.envelopes env where env.conversation.owner.managedIdentity = :participant",
        		Long.class)
        		.setParameter("participant", participant)
        		.getSingleResult();
    	assertEquals("Count must match", Math.toIntExact(expCount), messages.size());
    }

    @Test
    public void listMessages_Context() {
    	final String context = convP2_1.getContexts().iterator().next();
    	PagedResult<Long> messageIds = messageDao.listMessages(null, context, null, null, null, 100, 0);
//    	List<Message> messages = messageDao.loadGraphs(messageIds.getData(), null, Message::getId);
    	Long expCount = em.createQuery(
        		"select count(m) from Message m join m.envelopes env where env.context = :context or m.context = :context",
        		Long.class)
        		.setParameter("context", context)
        		.getSingleResult();
    	assertEquals("Message count should match", expCount.longValue(), messageIds.getCount());
    }

    @Test
    public void listMessages_Since_Until() {
    	final Instant since = Instant.parse("2020-02-11T14:24:00Z");
    	final Instant until = since;
    	PagedResult<Long> sinceMessageIds = messageDao.listMessages(null, null, since, null, null, 100, 0);
    	Long expCountSince = em.createQuery(
        		"select count(m) from Message m where m.createdTime >= :since",
        		Long.class)
        		.setParameter("since", since)
        		.getSingleResult();
    	assertEquals("Message since count should match", expCountSince.longValue(), sinceMessageIds.getCount());

    	PagedResult<Long> untilMessageIds = messageDao.listMessages(null, null, null, until, null, 100, 0);
    	Long expCountUntil = em.createQuery(
        		"select count(m) from Message m where m.createdTime < :until",
        		Long.class)
        		.setParameter("until", until)
        		.getSingleResult();
    	assertEquals("Message until count should match", expCountUntil.longValue(), untilMessageIds.getCount());

    	Long expCountTotal = em.createQuery(
        		"select count(m) from Message m",
        		Long.class)
        		.getSingleResult();
    	assertEquals("Message total count should match", expCountTotal.longValue(), sinceMessageIds.getCount() + untilMessageIds.getCount());
    }

    @Test
    public void listMessages_DeliveryModes() {
    	PagedResult<Long> defaultMessageIds = messageDao.listMessages(null, null, null, null, null, 100, 0);

    	PagedResult<Long> allMessageIds = messageDao.listMessages(null, null, null, null, DeliveryMode.ALL, 100, 0);
    	PagedResult<Long> msgMessageIds = messageDao.listMessages(null, null, null, null, DeliveryMode.MESSAGE, 100, 0);
    	PagedResult<Long> notMessageIds = messageDao.listMessages(null, null, null, null, DeliveryMode.NOTIFICATION, 100, 0);

    	Long expCountTotal = em.createQuery(
        		"select count(m) from Message m",
        		Long.class)
        		.getSingleResult();
    	Long expCountAll = em.createQuery(
        		"select count(m) from Message m where m.deliveryMode = eu.netmobiel.communicator.model.DeliveryMode.ALL",
        		Long.class)
        		.getSingleResult();
    	Long expCountMsg = em.createQuery(
        		"select count(m) from Message m where m.deliveryMode in :deliverySet",
        		Long.class)
    			.setParameter("deliverySet", EnumSet.of(DeliveryMode.ALL, DeliveryMode.MESSAGE))
        		.getSingleResult();
    	Long expCountNot = em.createQuery(
        		"select count(m) from Message m where m.deliveryMode in :deliverySet",
        		Long.class)
    			.setParameter("deliverySet", EnumSet.of(DeliveryMode.ALL, DeliveryMode.NOTIFICATION))
        		.getSingleResult();
    	log.info(String.format("Delivery mode Count: Total: %d; Msg: %d; Not: %d", expCountTotal.longValue(), expCountMsg.longValue(), expCountNot.longValue()));
    	assertEquals("Message total count should match", expCountTotal.longValue(), defaultMessageIds.getCount());
    	assertEquals("Message all count should match", expCountTotal.longValue(), allMessageIds.getCount());
    	assertEquals("Message count should match", expCountMsg.longValue(), msgMessageIds.getCount());
    	assertEquals("Notification count should match", expCountNot.longValue(), notMessageIds.getCount());
    	// Prevent counting the ALL messages twice.
    	assertEquals("Message total count should match", expCountTotal.longValue(), msgMessageIds.getCount() + notMessageIds.getCount() - expCountAll.longValue());
    	
    }


    @Test
    public void listConversation() {
    	log.info("Test lookup of conversations with most recent message");
    	final String participant = userC1.getManagedIdentity();
    	PagedResult<Long> archMessageIds = messageDao.listTopMessagesByConversations(participant, false, true, 100, 0);
    	List<Message> messages = messageDao.loadGraphs(archMessageIds.getData(), Message.LIST_MY_MESSAGES_ENTITY_GRAPH, Message::getId);
    	dump("Archived Top Messages", messages);
    	Long expArchCount = em.createQuery(
        		"select count(c) from Conversation c where c.owner.managedIdentity = :participant and c.archivedTime is not null",
        		Long.class)
        		.setParameter("participant", participant)
        		.getSingleResult();
    	assertEquals("Archived count must match", expArchCount.longValue(), archMessageIds.getCount());

    	PagedResult<Long> actualMessageIds = messageDao.listTopMessagesByConversations(participant, true, false, 100, 0);
    	Long expActualCount = em.createQuery(
        		"select count(c) from Conversation c where c.owner.managedIdentity = :participant and c.archivedTime is null",
        		Long.class)
        		.setParameter("participant", participant)
        		.getSingleResult();
    	assertEquals("Actual count must match", expActualCount.longValue(), actualMessageIds.getCount());

    	PagedResult<Long> messageIds = messageDao.listTopMessagesByConversations(participant, false, false, 100, 0);
    	Long expCount = em.createQuery(
        		"select count(c) from Conversation c where c.owner.managedIdentity = :participant",
        		Long.class)
        		.setParameter("participant", participant)
        		.getSingleResult();
    	assertEquals("Total count must match", expCount.longValue(), messageIds.getCount());
    	assertEquals("Total count must match", expCount.longValue(), archMessageIds.getCount() + actualMessageIds.getCount());
    }


}
