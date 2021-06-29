package eu.netmobiel.communicator.repository;


import static org.junit.Assert.*;

import java.time.Instant;
import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.commons.report.NumericReportValue;
import eu.netmobiel.communicator.model.CommunicatorUser;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Envelope;
import eu.netmobiel.communicator.test.CommunicatorIntegrationTestBase;
import eu.netmobiel.communicator.test.Fixture;

@RunWith(Arquillian.class)
public class EnvelopeDaoIT extends CommunicatorIntegrationTestBase {
    @Deployment
    public static Archive<?> createTestArchive() {
    	try {
	        WebArchive archive = createDeploymentBase()
	            .addClass(EnvelopeDao.class);
	//		System.out.println(archive.toString(true));
			return archive;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
    }

    @Inject
    private EnvelopeDao envelopeDao;

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

        em.persist(Fixture.createMessage("Body M7", "Context 4", "Subject 4", DeliveryMode.NOTIFICATION, "2020-04-21T15:00:00Z", user1, "2020-04-21T16:00:00Z", user2, user3));
        em.persist(Fixture.createMessage("Body M8", "Context 5", "Subject 5", DeliveryMode.NOTIFICATION, "2020-04-21T16:00:00Z", user1, null, user2, user3));
        em.persist(Fixture.createMessage("Body M9", "Context 5", "Subject 5", DeliveryMode.ALL, "2020-04-21T17:00:00Z", user1, "2020-04-21T20:00:00Z", user2, user3));
    }

    protected void testReportQuery(String queryName) throws Exception {
    	flush();
		List<NumericReportValue> results = envelopeDao.reportCount(queryName, Instant.parse("2020-01-01T00:00:00Z"), Instant.parse("2021-01-01T00:00:00Z"));
		assertNotNull(results);
		for (NumericReportValue r : results) {
			log.debug(String.format("%s %s", queryName, r.toString()));
		}
    }

    @Test
    public void listReportMessagesReceived() throws Exception {
    	testReportQuery(Envelope.ACT_1_MESSAGES_RECEIVED_COUNT);
    }
    @Test
    public void listReportNotificationsReceived() throws Exception {
    	testReportQuery(Envelope.ACT_2_NOTIFICATIONS_RECEIVED_COUNT);
    }
    @Test
    public void listReportMessagesRead() throws Exception {
    	testReportQuery(Envelope.ACT_3_MESSAGES_READ_COUNT);
    }
    @Test
    public void listReportNotificationsRead() throws Exception {
    	testReportQuery(Envelope.ACT_4_NOTIFICATIONS_READ_COUNT);
    }
}
