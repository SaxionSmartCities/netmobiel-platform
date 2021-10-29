package eu.netmobiel.communicator.repository;


import static org.junit.Assert.*;

import java.util.Optional;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.communicator.model.CommunicatorUser;
import eu.netmobiel.communicator.model.Conversation;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Envelope;
import eu.netmobiel.communicator.test.CommunicatorIntegrationTestBase;
import eu.netmobiel.communicator.test.Fixture;

@RunWith(Arquillian.class)
public class ConversationDaoIT extends CommunicatorIntegrationTestBase {
    @Deployment
    public static Archive<?> createTestArchive() {
    	try {
	        WebArchive archive = createDeploymentBase()
	            .addClass(ConversationDao.class);
	//		System.out.println(archive.toString(true));
			return archive;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
    }

    @Inject
    private ConversationDao dao;

    @SuppressWarnings("unused")
	@Inject
    private Logger log;
    
    private CommunicatorUser userP1;
    private CommunicatorUser userP2;
    private CommunicatorUser userC1;
    private CommunicatorUser userC2;
    
    private Conversation convP1_1;
//    private Conversation convP1_2;
//    private Conversation convP2_1;
    private Conversation convC1_1;
    private Conversation convC2_1;
    
    @Override
	protected void insertData() throws Exception {
        userP1 = Fixture.createUser("P1", "passagier", "FN P1", null);
        em.persist(userP1);
        userP2 = Fixture.createUser("P2", "passagier", "FN P2", null);
        em.persist(userP2);
        userC1 = Fixture.createUser("C1", "chauffeur", "FN C1", null);
        em.persist(userC1);
        userC2 = Fixture.createUser("C2", "chauffeur", "FN C2", null);
        em.persist(userC2);

        convP1_1 = Fixture.createConversation(userP1, "Topic P1.1", "2020-02-10T13:00:00Z", null, "Trip Plan P1", "Trip P1");
        em.persist(convP1_1);
//    	convP1_2 = Fixture.createConversation(userP1, "Topic P1.2", "2020-02-10T14:00:00Z", null, "Trip Plan P2");
//        em.persist(convP1_2);
//    	convP2_1 = Fixture.createConversation(userP2, "Topic P2.1", "2020-02-10T15:00:00Z", null, "context");
//        em.persist(convP2_1);
    	convC1_1 = Fixture.createConversation(userC1, "Topic C1.1", "2020-02-10T16:00:00Z", null, "Trip Plan P1", "Ride C1", "Booking C1");
        em.persist(convC1_1);
    	convC2_1 = Fixture.createConversation(userC2, "Topic C2.1", "2020-02-10T16:00:00Z", null, "Trip Plan P1", "Ride C2", "Booking C2");
        em.persist(convC2_1);
       
        em.persist(Fixture.createMessage("P1 zoekt een rit van A naar B", "Trip Plan P1", "Rit gezocht", DeliveryMode.MESSAGE, "2020-02-11T13:00:00Z", null, 
        		new Envelope(convC1_1), new Envelope(convC2_1)));
        em.persist(Fixture.createMessage("Je hebt een aanbod van C1", "Booking C1", "Rit aangeboden", DeliveryMode.MESSAGE, "2020-02-11T14:25:00Z", null, 
        		new Envelope("Trip Plan P1", convP1_1)));
        em.persist(Fixture.createMessage("Je hebt een rit aangeboden aan P1", "Ride C1", "Rit aangeboden", DeliveryMode.MESSAGE, "2020-02-11T14:25:00Z", null, 
        		new Envelope(convC1_1)));
        em.persist(Fixture.createMessage("C1 heeft het aanbod geannuleerd", "Booking C1", "Aanbod geannuleerd", DeliveryMode.MESSAGE, "2020-02-11T14:45:00Z", null, 
        		new Envelope("Trip Plan P1", convP1_1)));
        em.persist(Fixture.createMessage("Je hebt het aanbod aan P1 geannuleerd", "Ride C1", "Aanbod geannuleerd", DeliveryMode.MESSAGE, "2020-02-11T15:30:00Z", null, 
        		new Envelope(convC1_1)));
        em.persist(Fixture.createMessage("Je hebt een aanbod van C2", "Booking C2", "Rit aangeboden", DeliveryMode.MESSAGE, "2020-02-11T14:25:00Z", null, 
        		new Envelope("Trip Plan P1", convP1_1)));
        em.persist(Fixture.createMessage("Je hebt een rit aangeboden aan P1", "Ride C2", "Rit aangeboden", DeliveryMode.MESSAGE, "2020-02-11T14:25:00Z", null, 
        		new Envelope(convC2_1)));
        em.persist(Fixture.createMessage("P1 heeft je aanbod geaccepteerd", "Trip P1", "Rit geaccepteerd", DeliveryMode.MESSAGE, "2020-02-11T14:25:00Z", null, 
        		new Envelope("Ride C2", convC2_1)));
        em.persist(Fixture.createMessage("Kun je 10 minuten eerder langskomen?", "Trip P1", "Persoonlijk bericht van P1", DeliveryMode.MESSAGE, "2020-02-11T14:25:00Z", null, 
        		new Envelope("Ride C2", convC2_1)));
        em.persist(Fixture.createMessage("Is prima!", "Ride C2", "Persoonlijk bericht van C1", DeliveryMode.MESSAGE, "2020-02-11T14:25:00Z", null, 
        		new Envelope("Trip P1", convP1_1)));
    }

    @Test
    public void findConversation() throws Exception {
    	Optional<Conversation> optCv = dao.findByContextAndOwner("Trip Plan P1", userP1);
    	assertTrue(optCv.isPresent());
    	assertEquals(convP1_1.getId(), optCv.get().getId());
    	
    	optCv = dao.findByContextAndOwner("Trip Plan P1000", userP1);
    	assertFalse(optCv.isPresent());

    	optCv = dao.findByContextAndOwner("Trip Plan P1", userP2);
    	assertFalse(optCv.isPresent());

    	optCv = dao.findByContextAndOwner("Trip P1", userP1);
    	assertTrue(optCv.isPresent());
    	assertEquals(convP1_1.getId(), optCv.get().getId());
    }
}
