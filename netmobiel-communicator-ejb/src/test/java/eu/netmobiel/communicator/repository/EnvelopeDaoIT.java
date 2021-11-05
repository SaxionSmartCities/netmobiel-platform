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
import eu.netmobiel.communicator.model.Envelope;
import eu.netmobiel.communicator.test.CommunicatorIntegrationTestBase;

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
