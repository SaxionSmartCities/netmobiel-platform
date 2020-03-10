package eu.netmobiel.communicator.repository;


import static org.junit.Assert.*;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.communicator.Resources;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Envelope;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.model.User;
import eu.netmobiel.communicator.repository.EnvelopeDao;
import eu.netmobiel.communicator.repository.converter.DeliveryModeConverter;
import eu.netmobiel.communicator.util.CommunicatorUrnHelper;

@RunWith(Arquillian.class)
public class EnvelopeDaoIT {
    @Deployment
    public static Archive<?> createTestArchive() {
    	File[] deps = Maven.configureResolver()
				.loadPomFromFile("pom.xml")
				.importCompileAndRuntimeDependencies() 
				.resolve()
				.withTransitivity()
				.asFile();
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "test.war")
                .addAsLibraries(deps)
                .addPackages(true, CommunicatorUrnHelper.class.getPackage())
                .addPackages(true, Envelope.class.getPackage())
                .addPackages(true, AbstractDao.class.getPackage())
                .addPackages(true, DeliveryModeConverter.class.getPackage())
            .addClass(UserDao.class)
            .addClass(EnvelopeDao.class)
            .addClass(Resources.class)
            .addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
//		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private EnvelopeDao envelopeDao;
    @Inject
    private UserDao userDao;

    @PersistenceContext(unitName = "pu-communicator")
    private EntityManager em;
    
    @Inject
    private UserTransaction utx;
    
    @Inject
    private Logger log;
    
    @Before
    public void preparePersistenceTest() throws Exception {
        clearData();
        insertData();
        startTransaction();
    }
    
    private void clearData() throws Exception {
        utx.begin();
        em.joinTransaction();
        log.debug("Dumping old records...");
        em.createQuery("delete from Envelope").executeUpdate();
        em.createQuery("delete from Message").executeUpdate();
        em.createQuery("delete from User").executeUpdate();
        utx.commit();
    }

    private void insertData() throws Exception {
        utx.begin();
        em.joinTransaction();
        log.debug("Inserting records...");
    	List<User> users = new ArrayList<>();
        users.add(new User("A1", "user", "FN A1"));
        users.add(new User("A2", "user", "FN A2"));
        users.add(new User("A3", "user", "FN A3"));
        for (User user : users) {
			em.persist(user);
		}
    	List<Envelope> envelopes = new ArrayList<>();
    	envelopes.addAll(createEnvelopes("Body M0", "Context 1", "Subject 1", "2020-02-11T13:00:00Z", "A1", "2020-02-11T15:00:00Z", "A2", "A3"));
    	envelopes.addAll(createEnvelopes("Body M1", "Context 1", "Subject 1", "2020-02-11T14:25:00Z", "A1", null, "A2", "A3"));
        envelopes.addAll(createEnvelopes("Body M2", "Context 1", "Subject 1", "2020-02-12T11:00:00Z", "A2", null, "A1", "A3"));
        envelopes.addAll(createEnvelopes("Body M3", "Context 2", "Subject 2", "2020-02-13T12:00:00Z", "A1", null, "A2", "A3"));
        envelopes.addAll(createEnvelopes("Body M4", "Context 1", "Subject 1", "2020-02-13T13:00:00Z", "A2", null, "A1", "A3"));
        envelopes.addAll(createEnvelopes("Body M5", "Context 3", "Subject 3", "2020-02-13T14:00:00Z", "A1", null, "A2", "A3"));
        envelopes.addAll(createEnvelopes("Body M6", "Context 2", "Subject 2", "2020-02-13T15:00:00Z", "A1", null, "A2", "A3"));
        for (Envelope envelope : envelopes) {
			em.persist(envelope);
		}
        utx.commit();
        // clear the persistence context (first-level cache)
        em.clear();
    }

    private void startTransaction() throws Exception {
        utx.begin();
        em.joinTransaction();
    }

    @After
    public void commitTransaction() throws Exception {
        utx.commit();
    }
    
    private void assertContainsAllEnvelopes(Collection<Envelope> expectedEnvelopes, Collection<Envelope> retrievedEnvelopes) {
        assertEquals(expectedEnvelopes.size(), retrievedEnvelopes.size());
        final Set<String> actualRecipients = new HashSet<String>();
        for (Envelope env : retrievedEnvelopes) {
            log.debug("* " + env.toString());
            actualRecipients.add(env.getRecipient().getManagedIdentity());
        }
        final List<String> expectedRecipients = expectedEnvelopes.stream().map(env -> env.getRecipient().getManagedIdentity()).collect(Collectors.toList());  
        assertTrue(actualRecipients.containsAll(expectedRecipients));
    }
    
    private List<Envelope> findAllEnvelopesSentBy(String sender) {
        log.debug("Selecting (using JPQL)...");
        List<Envelope> envelopes = em.createQuery(
        		"select env from Envelope env where env.message.sender.managedIdentity = :sender order by env.message.creationTime desc",
        		Envelope.class)
        		.setParameter("sender", sender)
        		.getResultList();
        log.debug("Found " + envelopes.size() + " envelopes (using JPQL)");
        return envelopes;
    }
    
    private List<Envelope> createEnvelopes(String body, String context, String subject,String creationTimeIso, String sender, String ackTimeIso, String... recipients) {
    	Instant creationTime = Instant.parse(creationTimeIso);
    	Instant ackTime = ackTimeIso != null ? Instant.parse(ackTimeIso) : null; 
    	Message m = new Message();
    	m.setBody(body);
    	m.setContext(context);
    	m.setCreationTime(creationTime);
    	m.setSender(userDao.findByManagedIdentity(sender).get());
    	m.setDeliveryMode(DeliveryMode.MESSAGE);
    	m.setSubject(subject);
    	return Arrays.stream(recipients)
    			.map(rcp -> new Envelope(m, userDao.findByManagedIdentity(rcp).get(), ackTime))
    			.collect(Collectors.toList());
    }
    
    private void dump(String subject, Collection<Envelope> envelopes) {
    	envelopes.forEach(e -> log.info(subject + ": " + e.toString()));
    }
    
    @Test
    public void saveEnvelopes() {
		em.persist(new User("A11"));
		em.persist(new User("A12"));
		em.persist(new User("A13"));
    	List<Envelope> envelopes = createEnvelopes("Body B", "Context C", "Subject S", "2020-02-11T14:25:00Z", "A11", null, "A12", "A13");
    	envelopeDao.saveAll(envelopes);
    	List<Envelope> actualEnvelopes = findAllEnvelopesSentBy("A11");
    	assertContainsAllEnvelopes(envelopes, actualEnvelopes);
    }

    @Test
    public void listEnvelopes_All() {
    	PagedResult<Long> envelopeIds = envelopeDao.listEnvelopes(null, null, null, null, 100, 0);
    	List<Envelope> envelopes = envelopeDao.fetch(envelopeIds.getData(), null);
    	Long expCount = em.createQuery("select count(env) from Envelope env", Long.class).getSingleResult();
    	assertEquals("All envelopes present", Math.toIntExact(expCount) , envelopes.size());
    	assertNull("No total count", envelopeIds.getTotalCount());
    	assertEquals("Offset matches", 0, envelopeIds.getOffset());
    	assertEquals("maxResults matches", 100, envelopeIds.getResultsPerPage());
    }

    @Test
    public void listEnvelopes_AllCount() {
    	PagedResult<Long> envelopeIds = envelopeDao.listEnvelopes(null, null, null, null, 0, 0);
    	Long expCount = em.createQuery("select count(env) from Envelope env", Long.class).getSingleResult();
    	assertEquals("All envelopes present", expCount , envelopeIds.getTotalCount());
    	assertEquals("Offset matches", 0, envelopeIds.getOffset());
    	assertEquals("maxResults matches", 0, envelopeIds.getResultsPerPage());
    }

    @Test
    public void listEnvelopes_ByRecipient() {
    	final String recipient = "A3";
    	PagedResult<Long> envelopeIds = envelopeDao.listEnvelopes(recipient, null, null, null, 100, 0);
    	List<Envelope> envelopes = envelopeDao.fetch(envelopeIds.getData(), null);
    	Set<String> recipients = envelopes.stream().map(env -> env.getRecipient().getManagedIdentity()).collect(Collectors.toSet());
    	assertEquals("Only 1 recipient", 1, recipients.size());
    	assertTrue("Must be the requested recipient", recipients.contains(recipient));
    	Long expCount = em.createQuery(
        		"select count(env) from Envelope env where env.recipient.managedIdentity = :recipient",
        		Long.class)
        		.setParameter("recipient", recipient)
        		.getSingleResult();
    	assertEquals("Count must match", Math.toIntExact(expCount), envelopes.size());
    }

    @Test
    public void listEnvelopes_Context() {
    	final String context = "Context 1";
    	PagedResult<Long> envelopeIds = envelopeDao.listEnvelopes(null, context, null, null, 100, 0);
    	List<Envelope> envelopes = envelopeDao.fetch(envelopeIds.getData(), null);
    	Set<String> bodies = envelopes.stream().map(env -> env.getMessage().getBody()).collect(Collectors.toSet());
    	assertTrue("Body M0 present", bodies.contains("Body M0"));
    	assertTrue("Body M1 present", bodies.contains("Body M1"));
    	assertTrue("Body M2 present", bodies.contains("Body M2"));
    	assertTrue("Body M4 present", bodies.contains("Body M4"));
    	assertEquals("Only 4 message bodies", 4, bodies.size());
    }

    @Test
    public void listEnvelopes_Since() {
    	final Instant since = Instant.parse("2020-02-13T14:00:00Z");
    	PagedResult<Long> envelopeIds = envelopeDao.listEnvelopes(null, null, since, null, 100, 0);
    	List<Envelope> envelopes = envelopeDao.fetch(envelopeIds.getData(), null);
    	Set<String> bodies = envelopes.stream().map(env -> env.getMessage().getBody()).collect(Collectors.toSet());
    	assertEquals("Only 2 message bodies", 2, bodies.size());
    	assertEquals("Only 4 envelopes", 4, envelopes.size());
    	assertTrue("Body M5 present", bodies.contains("Body M5"));
    	assertTrue("Body M6 present", bodies.contains("Body M6"));
    }

    @Test
    public void listEnvelopes_Until() {
    	final Instant until = Instant.parse("2020-02-13T12:00:00Z");
    	PagedResult<Long> envelopeIds = envelopeDao.listEnvelopes(null, null, null, until, 100, 0);
    	List<Envelope> envelopes = envelopeDao.fetch(envelopeIds.getData(), null);
//    	dump("listEnvelopes_Until", envelopes);
    	Set<String> bodies = envelopes.stream().map(env -> env.getMessage().getBody()).collect(Collectors.toSet());
    	assertEquals("Only 3 message bodies", 3, bodies.size());
    	assertEquals("Only 6 envelopes", 6, envelopes.size());
    	assertTrue("Body M0 present", bodies.contains("Body M0"));
    	assertTrue("Body M1 present", bodies.contains("Body M1"));
    	assertTrue("Body M2 present", bodies.contains("Body M2"));
    }

    @Test
    public void listConversation() {
    	final String recipient = "A3";
    	PagedResult<Long> envelopeIds = envelopeDao.listConversations(recipient, 100, 0);
    	List<Envelope> envelopes = envelopeDao.fetch(envelopeIds.getData(), null);
    	dump("listConversation", envelopes);
    	Set<String> bodies = envelopes.stream().map(env -> env.getMessage().getBody()).collect(Collectors.toSet());
    	assertEquals("Only 3 message bodies", 3, bodies.size());
    	assertEquals("Only 3 envelopes", 3, envelopes.size());
    	assertTrue("Body M6 present", bodies.contains("Body M6"));
    	assertTrue("Body M5 present", bodies.contains("Body M5"));
    	assertTrue("Body M4 present", bodies.contains("Body M4"));
    }
}
