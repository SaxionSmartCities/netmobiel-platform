package eu.netmobiel.communicator.test;

import java.io.File;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.commons.util.ExceptionUtil;
import eu.netmobiel.communicator.Resources;
import eu.netmobiel.communicator.annotation.CommunicatorDatabase;
import eu.netmobiel.communicator.filter.MessageFilter;
import eu.netmobiel.communicator.model.CommunicatorUser;
import eu.netmobiel.communicator.model.Conversation;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Envelope;
import eu.netmobiel.communicator.model.UserRole;
import eu.netmobiel.communicator.repository.converter.DeliveryModeConverter;
import eu.netmobiel.communicator.util.CommunicatorUrnHelper;

public abstract class CommunicatorIntegrationTestBase {
	
    public static WebArchive createDeploymentBase() {
    	File[] deps = Maven.configureResolver()
    			.loadPomFromFile("pom.xml")
    			.importCompileAndRuntimeDependencies() 
    			.resolve()
    			.withTransitivity()
    			.asFile();
        return ShrinkWrap.create(WebArchive.class, "test.war")
                .addAsLibraries(deps)
                .addPackages(true, CommunicatorDatabase.class.getPackage())
                .addPackages(true, CommunicatorUrnHelper.class.getPackage())
                .addPackages(true, MessageFilter.class.getPackage())
                .addPackages(true, Envelope.class.getPackage())
                .addPackages(true, AbstractDao.class.getPackage())
                .addPackages(true, DeliveryModeConverter.class.getPackage())
                .addPackages(true, Fixture.class.getPackage())
                .addClass(Resources.class)
//                .addAsResource("log4j.properties")
    	        .addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml");
    }
	
    @PersistenceContext(unitName = "pu-communicator")
    protected EntityManager em;
    
    @Inject
    protected UserTransaction utx;
    
    @Inject
    private Logger log;
    
    protected CommunicatorUser userP1;
    protected CommunicatorUser userP2;
    protected CommunicatorUser userC1;
    protected CommunicatorUser userC2;
    
    protected Conversation convP1_1;
    protected Conversation convP1_2;
    protected Conversation convP2_1;
    protected Conversation convC1_1;
    protected Conversation convC1_2;
    protected Conversation convC2_1;

    private boolean expectFailure;
    
	@Before
	public void prepareTest() throws Exception {
		expectFailure = false;
		clearData();
		prepareData();
		startTransaction();
	}
	
	@After
	public void finishTest() throws Exception {
		try {
			if (!expectFailure) {
				commitTransaction();
			}
		} catch (Exception ex) {
			log.error(String.join("\n", ExceptionUtil.unwindException(ex)));
			throw ex;	
		}
	}

	public void expectFailure() {
		this.expectFailure = true;
	}

	protected void commitTransaction() throws Exception {
		if (em.isJoinedToTransaction()) {
			utx.commit();
		}
	}

	protected void clearData() throws Exception {
		utx.begin();
		em.joinTransaction();
		log.debug("Dumping old records...");
        em.createQuery("delete from Envelope").executeUpdate();
        em.createQuery("delete from Message").executeUpdate();
        em.createQuery("delete from Conversation").executeUpdate();
        em.createQuery("delete from CommunicatorUser").executeUpdate();
		utx.commit();
	}

	protected void prepareData() throws Exception {
		utx.begin();
		em.joinTransaction();
		log.debug("Inserting records...");

		insertData();

		utx.commit();
		// clear the persistence context (first-level cache)
		em.clear();
	}

	protected void insertData() throws Exception {
        userP1 = Fixture.createUser("P1", "passagier", "FN P1", null);
        em.persist(userP1);
        userP2 = Fixture.createUser("P2", "passagier", "FN P2", null);
        em.persist(userP2);
        userC1 = Fixture.createUser("C1", "chauffeur", "FN C1", null);
        em.persist(userC1);
        userC2 = Fixture.createUser("C2", "chauffeur", "FN C2", null);
        em.persist(userC2);

        convP1_1 = Fixture.createConversation(userP1, UserRole.PASSENGER, "Topic P1.1", "2020-02-10T13:00:00Z", null, "Trip Plan P1.1", "Trip P1.1");
        em.persist(convP1_1);
    	convP1_2 = Fixture.createConversation(userP1, UserRole.PASSENGER, "Topic P1.2", "2020-02-10T14:00:00Z", null, "Trip Plan P1.2");
        em.persist(convP1_2);
    	convP2_1 = Fixture.createConversation(userP2, UserRole.PASSENGER, "Topic P2.1", "2020-02-10T15:00:00Z", null, "Trip P2.1");
        em.persist(convP2_1);
    	convC1_1 = Fixture.createConversation(userC1, UserRole.DRIVER, "Topic C1.1", "2020-02-10T16:00:00Z", "2020-02-26T16:00:00Z", "Trip Plan P1.1", "Ride C1.1", "Booking C1.1.1");
        em.persist(convC1_1);
    	convC2_1 = Fixture.createConversation(userC2, UserRole.DRIVER, "Topic C2.1", "2020-02-10T16:00:00Z", null, "Trip Plan P1.1", "Ride C2.1", "Booking C2.1.1");
        em.persist(convC2_1);
    	convC1_2 = Fixture.createConversation(userC1, UserRole.DRIVER, "Topic C1.2", "2020-02-10T17:00:00Z", null, "Ride C1.2", "Booking C1.2.1");
        em.persist(convC1_2);
       
        em.persist(Fixture.createMessage("P1 zoekt een rit van A naar B", "Trip Plan P1.1", "Rit gezocht", DeliveryMode.MESSAGE, "2020-02-11T13:00:00Z", null, 
        		new Envelope(convC1_1), new Envelope(convC2_1)));
        em.persist(Fixture.createMessage("Je hebt een aanbod van C1", "Booking C1.1.1", "Rit aangeboden", DeliveryMode.MESSAGE, "2020-02-11T14:25:00Z", null, 
        		new Envelope("Trip Plan P1.1", convP1_1)));
        em.persist(Fixture.createMessage("Je hebt een rit aangeboden aan P1", "Ride C1.1", "Rit aangeboden", DeliveryMode.NOTIFICATION, "2020-02-11T14:25:00Z", null, 
        		new Envelope(convC1_1)));
        em.persist(Fixture.createMessage("C1 heeft het aanbod geannuleerd", "Booking C1.1.1", "Aanbod geannuleerd", DeliveryMode.MESSAGE, "2020-02-11T14:45:00Z", null, 
        		new Envelope("Trip Plan P1.1", convP1_1)));
        em.persist(Fixture.createMessage("Je hebt het aanbod aan P1 geannuleerd", "Ride C1.1", "Aanbod geannuleerd", DeliveryMode.NOTIFICATION, "2020-02-11T15:30:00Z", null, 
        		new Envelope(convC1_1)));
        em.persist(Fixture.createMessage("Je hebt een aanbod van C2", "Booking C2.1.1", "Rit aangeboden", DeliveryMode.MESSAGE, "2020-02-11T14:25:00Z", null, 
        		new Envelope("Trip Plan P1.1", convP1_1)));
        em.persist(Fixture.createMessage("Je hebt een rit aangeboden aan P1", "Ride C2.1", "Rit aangeboden", DeliveryMode.NOTIFICATION, "2020-02-11T14:25:00Z", null, 
        		new Envelope(convC2_1)));
        em.persist(Fixture.createMessage("P1 heeft je aanbod geaccepteerd", "Trip P1.1", "Rit geaccepteerd", DeliveryMode.MESSAGE, "2020-02-11T14:25:00Z", null, 
        		new Envelope("Ride C2.1", convC2_1)));
        em.persist(Fixture.createMessage("Kun je 10 minuten eerder langskomen?", "Trip P1.1", "Persoonlijk bericht van P1", DeliveryMode.ALL, "2020-02-11T14:25:00Z", convP1_1, 
        		new Envelope("Ride C2.1", convC2_1)));
        em.persist(Fixture.createMessage("Is prima!", "Ride C2.1", "Persoonlijk bericht van C1", DeliveryMode.ALL, "2020-02-11T14:25:00Z", convC2_1, 
        		new Envelope("Trip P1.1", convP1_1)));

        em.persist(Fixture.createMessage("P1 zoekt een rit van C naar D", "Trip Plan P1.2", "Rit gezocht", DeliveryMode.MESSAGE, "2020-02-11T13:30:00Z", null, 
        		new Envelope(convC1_1)));

        em.persist(Fixture.createMessage("P2 rijdt met je mee", "Trip P2.1", "Meerijden bevestigd", DeliveryMode.MESSAGE, "2020-02-11T14:25:00Z", null, 
        		new Envelope("Ride C1.2", convC1_2)));
        
    }

	protected void startTransaction() throws Exception {
		utx.begin();
		em.joinTransaction();
	}

	protected void flush() {
		try {
			utx.commit();
			// clear the persistence context (first-level cache)
			em.clear();
			utx.begin();
			em.joinTransaction();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

}