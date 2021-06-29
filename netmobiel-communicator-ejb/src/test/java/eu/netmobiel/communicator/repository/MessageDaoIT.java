package eu.netmobiel.communicator.repository;


import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Collection;
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
import eu.netmobiel.communicator.model.CommunicatorUser;
import eu.netmobiel.communicator.model.DeliveryMode;
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
    
    private CommunicatorUser user1;
    private CommunicatorUser user2;
    private CommunicatorUser user3;
    
    @Override
	protected void insertData() throws Exception {
        user1 = Fixture.createUser("A1", "user", "FN A1", null);
        em.persist(user1);
        user2 = Fixture.createUser("A2", "user", "FN A2", null);
        em.persist(user2);
        user3 = Fixture.createUser("A3", "user", "FN A3", null);
        em.persist(user3);
        
        em.persist(Fixture.createMessage("Body M0", "Context 1", "Subject 1", DeliveryMode.MESSAGE, "2020-02-11T13:00:00Z", user1, "2020-02-11T15:00:00Z", user2, user3));
        em.persist(Fixture.createMessage("Body M1", "Context 1", "Subject 1", DeliveryMode.MESSAGE, "2020-02-11T14:25:00Z", user1, null, user2, user3));
        em.persist(Fixture.createMessage("Body M2", "Context 1", "Subject 1", DeliveryMode.MESSAGE, "2020-02-12T11:00:00Z", user2, null, user1, user3));
        em.persist(Fixture.createMessage("Body M3", "Context 2", "Subject 2", DeliveryMode.MESSAGE, "2020-02-13T12:00:00Z", user1, null, user2, user3));
        em.persist(Fixture.createMessage("Body M4", "Context 1", "Subject 1", DeliveryMode.MESSAGE, "2020-02-13T13:00:00Z", user2, null, user1, user3));
        em.persist(Fixture.createMessage("Body M5", "Context 3", "Subject 3", DeliveryMode.MESSAGE, "2020-02-13T14:00:00Z", user1, null, user2, user3));
        em.persist(Fixture.createMessage("Body M6", "Context 2", "Subject 2", DeliveryMode.MESSAGE, "2020-02-13T15:00:00Z", user1, null, user2, user3));

    }

    private void dump(String subject, Collection<Message> messages) {
    	messages.forEach(m -> log.info(subject + ": " + m.toString()));
    }
    
    @Test
    public void saveMessage() {
		em.persist(new CommunicatorUser("A11"));
		em.persist(new CommunicatorUser("A12"));
		em.persist(new CommunicatorUser("A13"));
		Message message = Fixture.createMessage("Body B", "Context C", "Subject S", DeliveryMode.MESSAGE, "2020-02-11T14:25:00Z", user3, null, user1, user2);
    	messageDao.save(message);
    	List<Message> actual = em.createQuery("select m from Message m where m.sender = :sender", Message.class)
        		.setParameter("sender", user3)
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
    }

    @Test
    public void listMessages_AllCount() {
    	PagedResult<Long> messageIds = messageDao.listMessages(null, null, null, null, null, 0, 0);
    	Long expCount = em.createQuery("select count(m) from Message m", Long.class).getSingleResult();
    	assertEquals("All messages present", expCount , messageIds.getTotalCount());
    	assertEquals("Offset matches", 0, messageIds.getOffset());
    	assertEquals("maxResults matches", 0, messageIds.getResultsPerPage());
    }

    @Test
    public void listMessages_ByParticipant() {
    	final String participant = "A3";
    	PagedResult<Long> messageIds = messageDao.listMessages(participant, null, null, null, null, 100, 0);
    	List<Message> messages = messageDao.loadGraphs(messageIds.getData(), Message.LIST_MY_MESSAGES_ENTITY_GRAPH, Message::getId);
    	for (Message message : messages) {
			// The participant is one of the recipients or is the sender of the message
        	Set<String> recipients = message.getEnvelopes().stream().map(env -> env.getRecipient().getManagedIdentity()).collect(Collectors.toSet());
        	assertTrue("Must be sender of recipient", participant.equals(message.getSender().getManagedIdentity()) || recipients.contains(participant));
		}
    	Long expCount = em.createQuery(
        		"select count(m) from Message m join m.envelopes env where env.recipient.managedIdentity = :participant or m.sender.managedIdentity = :participant",
        		Long.class)
        		.setParameter("participant", participant)
        		.getSingleResult();
    	assertEquals("Count must match", Math.toIntExact(expCount), messages.size());
    }

    @Test
    public void listMessages_Context() {
    	final String context = "Context 1";
    	PagedResult<Long> messageIds = messageDao.listMessages(null, context, null, null, null, 100, 0);
    	List<Message> messages = messageDao.loadGraphs(messageIds.getData(), null, Message::getId);
    	Set<String> bodies = messages.stream().map(m -> m.getBody()).collect(Collectors.toSet());
    	assertTrue("Body M0 present", bodies.contains("Body M0"));
    	assertTrue("Body M1 present", bodies.contains("Body M1"));
    	assertTrue("Body M2 present", bodies.contains("Body M2"));
    	assertTrue("Body M4 present", bodies.contains("Body M4"));
    	assertEquals("Only 4 message bodies", 4, bodies.size());
    }

    @Test
    public void listMessages_Since() {
    	final Instant since = Instant.parse("2020-02-13T14:00:00Z");
    	PagedResult<Long> messageIds = messageDao.listMessages(null, null, since, null, null, 100, 0);
    	List<Message> messages = messageDao.loadGraphs(messageIds.getData(), null, Message::getId);
    	Set<String> bodies = messages.stream().map(m -> m.getBody()).collect(Collectors.toSet());
    	assertEquals("Only 2 messages", 2, messages.size());
    	assertEquals("Only 2 message bodies", 2, bodies.size());
    	assertTrue("Body M5 present", bodies.contains("Body M5"));
    	assertTrue("Body M6 present", bodies.contains("Body M6"));
    }

    @Test
    public void listMessages_Until() {
    	final Instant until = Instant.parse("2020-02-13T12:00:00Z");
    	PagedResult<Long> messageIds = messageDao.listMessages(null, null, null, until, null, 100, 0);
    	List<Message> messages = messageDao.loadGraphs(messageIds.getData(), null, Message::getId);
//    	dump("listMessages_Until", messages);
    	Set<String> bodies = messages.stream().map(m -> m.getBody()).collect(Collectors.toSet());
    	assertEquals("Only 3 message bodies", 3, bodies.size());
    	assertEquals("Only 3 messages", 3, messages.size());
    	assertTrue("Body M0 present", bodies.contains("Body M0"));
    	assertTrue("Body M1 present", bodies.contains("Body M1"));
    	assertTrue("Body M2 present", bodies.contains("Body M2"));
    }

    private void prepareDeliveryModes() {
        em.persist(Fixture.createMessage("Body M7", "Context 4", "Subject 4", DeliveryMode.NOTIFICATION, "2020-04-21T15:00:00Z", user1, "2020-04-21T16:00:00Z", user2, user3));
        em.persist(Fixture.createMessage("Body M8", "Context 5", "Subject 5", DeliveryMode.NOTIFICATION, "2020-04-21T16:00:00Z", user1, null, user2, user3));
        em.persist(Fixture.createMessage("Body M9", "Context 5", "Subject 5", DeliveryMode.ALL, "2020-04-21T17:00:00Z", user1, "2020-04-21T20:00:00Z", user2, user3));
        flush();
    }

    @Test
    public void listMessages_DeliveryModes_Default() {
    	prepareDeliveryModes();
    	PagedResult<Long> messageIds = messageDao.listMessages("A3", null, null, null, null, 100, 0);
    	List<Message> messages = messageDao.loadGraphs(messageIds.getData(), null, Message::getId);
    	dump("listMessages_DeliveryModes", messages);
    	Set<DeliveryMode> modes = messages.stream().map(m -> m.getDeliveryMode()).collect(Collectors.toSet());
    	assertTrue("MESSAGE present", modes.contains(DeliveryMode.MESSAGE));
    	assertTrue("NOTIFICATION present", modes.contains(DeliveryMode.NOTIFICATION));
    	assertTrue("ALL present", modes.contains(DeliveryMode.ALL));
    }

    @Test
    public void listMessages_DeliveryModes_All() {
    	prepareDeliveryModes();
    	PagedResult<Long> messageIds = messageDao.listMessages("A3", null, null, null, DeliveryMode.ALL, 100, 0);
    	List<Message> messages = messageDao.loadGraphs(messageIds.getData(), null, Message::getId);
    	dump("listMessages_DeliveryModes_All", messages);
    	Set<DeliveryMode> modes = messages.stream().map(m -> m.getDeliveryMode()).collect(Collectors.toSet());
    	assertTrue("MESSAGE present", modes.contains(DeliveryMode.MESSAGE));
    	assertTrue("NOTIFICATION present", modes.contains(DeliveryMode.NOTIFICATION));
    	assertTrue("ALL present", modes.contains(DeliveryMode.ALL));
    }

    @Test
    public void listMessages_DeliveryModes_MessageOnly() {
    	prepareDeliveryModes();
    	PagedResult<Long> messageIds = messageDao.listMessages("A3", null, null, null, DeliveryMode.MESSAGE, 100, 0);
    	List<Message> messages = messageDao.loadGraphs(messageIds.getData(), null, Message::getId);
    	dump("listMessages_DeliveryModes_MessageOnly", messages);
    	Set<DeliveryMode> modes = messages.stream().map(m -> m.getDeliveryMode()).collect(Collectors.toSet());
    	assertTrue("MESSAGE present", modes.contains(DeliveryMode.MESSAGE));
    	assertFalse("NOTIFICATION present", modes.contains(DeliveryMode.NOTIFICATION));
    	assertTrue("ALL present", modes.contains(DeliveryMode.ALL));
    }

    @Test
    public void listMessages_DeliveryModes_NotificationOnly() {
    	prepareDeliveryModes();
    	PagedResult<Long> messageIds = messageDao.listMessages("A3", null, null, null, DeliveryMode.NOTIFICATION, 100, 0);
    	List<Message> messages = messageDao.loadGraphs(messageIds.getData(), null, Message::getId);
    	dump("listMessages_DeliveryModes_NotificationOnly", messages);
    	Set<DeliveryMode> modes = messages.stream().map(m -> m.getDeliveryMode()).collect(Collectors.toSet());
    	assertFalse("MESSAGE present", modes.contains(DeliveryMode.MESSAGE));
    	assertTrue("NOTIFICATION present", modes.contains(DeliveryMode.NOTIFICATION));
    	assertTrue("ALL present", modes.contains(DeliveryMode.ALL));
    }

    @Test
    public void listConversation() {
    	final String recipient = "A3";
    	PagedResult<Long> messageIds = messageDao.listConversations(recipient, 100, 0);
    	List<Message> messages = messageDao.loadGraphs(messageIds.getData(), null, Message::getId);
//    	dump("listConversation", messages);
    	Set<String> bodies = messages.stream().map(m -> m.getBody()).collect(Collectors.toSet());
    	assertEquals("Only 3 message bodies", 3, bodies.size());
    	assertEquals("Only 3 messages", 3, messages.size());
    	assertTrue("Body M6 present", bodies.contains("Body M6"));
    	assertTrue("Body M5 present", bodies.contains("Body M5"));
    	assertTrue("Body M4 present", bodies.contains("Body M4"));
    }

    @Test
    public void listConversation_MixedDeliveryMode() {
    	prepareDeliveryModes();
    	final String recipient = "A3";
    	PagedResult<Long> messageIds = messageDao.listConversations(recipient, 100, 0);
    	List<Message> messages = messageDao.loadGraphs(messageIds.getData(), null, Message::getId);
//    	dump("listConversation", messages);
    	Set<String> bodies = messages.stream().map(m -> m.getBody()).collect(Collectors.toSet());
    	assertEquals("Only 4 message bodies", 4, bodies.size());
    	assertEquals("Only 4 messages", 4, messages.size());
    	assertTrue("Body M9 present", bodies.contains("Body M9"));
    	assertTrue("Body M6 present", bodies.contains("Body M6"));
    	assertTrue("Body M5 present", bodies.contains("Body M5"));
    	assertTrue("Body M4 present", bodies.contains("Body M4"));
    }

}
