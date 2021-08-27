package eu.netmobiel.profile.repository;

import static org.junit.Assert.*;

import java.time.Instant;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.profile.model.Survey;
import eu.netmobiel.profile.model.SurveyProvider;
import eu.netmobiel.profile.model.SurveyTrigger;
import eu.netmobiel.profile.test.ProfileIntegrationTestBase;

@RunWith(Arquillian.class)
public class SurveyDaoIT extends ProfileIntegrationTestBase {
    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive archive = createDeploymentBase()
            .addClass(SurveyDao.class);
//		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private SurveyDao surveyDao;

	@SuppressWarnings("unused")
	@Inject
    private Logger log;

    @Override
	protected void insertData() throws Exception {
    }

    
    @Test
    public void createSurvey() throws Exception {
    	Survey s = new Survey();
    	s.setDelayHours(24);
    	s.setDisplayName("De eerste enquete");
    	s.setEndTime(Instant.parse("2022-08-31T12:00:00Z"));
    	s.setGroupRef("NB");
    	s.setProviderSurveyRef("QC1234");
    	s.setRemarks("Test survey");
    	s.setSequenceNr(1);
    	s.setStartTime(Instant.parse("2021-08-31T12:00:00Z"));
    	s.setSurveyId("ENQ-1");
    	s.setSurveyProvider(SurveyProvider.QUALTRICS);
    	s.setSurveyTrigger(SurveyTrigger.NEW_PROFILE);
    	surveyDao.save(s);
    	flush();
    	
    	Survey sdb = em.find(Survey.class, "ENQ-1");
    	assertNotNull(sdb);
    	assertEquals(s.getDelayHours(), sdb.getDelayHours());
    	assertEquals(s.getDisplayName(), sdb.getDisplayName());
    	assertEquals(s.getEndTime(), sdb.getEndTime());
    	assertEquals(s.getGroupRef(), sdb.getGroupRef());
    	assertEquals(s.getProviderSurveyRef(), sdb.getProviderSurveyRef());
    	assertEquals(s.getRemarks(), sdb.getRemarks());
    	assertEquals(s.getSequenceNr(), sdb.getSequenceNr());
    	assertEquals(s.getStartTime(), sdb.getStartTime());
    	assertEquals(s.getSurveyId(), sdb.getSurveyId());
    	assertEquals(s.getSurveyProvider(), sdb.getSurveyProvider());
    	assertEquals(s.getSurveyTrigger(), sdb.getSurveyTrigger());
    }

}
