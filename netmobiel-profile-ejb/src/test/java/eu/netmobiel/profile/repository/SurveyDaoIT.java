package eu.netmobiel.profile.repository;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Optional;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.profile.model.Survey;
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

    protected void createSurvey(String id, String start, String end, Integer delay, Integer interval) {
    	Survey s = new Survey();
    	s.setSurveyId(id);
    	s.setDisplayName("Dit is " + id);
    	s.setStartTime(start == null ? null :Instant.parse(start));
    	s.setEndTime(end == null ? null : Instant.parse(end));
    	s.setProviderSurveyRef("QC-" + id);
    	s.setRemarks("Opmerkingen over " + id);
    	s.setTakeDelayHours(delay);
    	s.setTakeIntervalHours(interval);
    	em.persist(s);
    }
    
    @Test
    public void createSurvey() throws Exception {
    	Survey s = new Survey();
    	s.setDisplayName("De eerste enquete");
    	s.setEndTime(Instant.parse("2022-08-31T12:00:00Z"));
    	s.setProviderSurveyRef("QC1234");
    	s.setRemarks("Test survey");
    	s.setStartTime(Instant.parse("2021-08-31T12:00:00Z"));
    	s.setSurveyId("ENQ-1");
    	s.setTakeDelayHours(24);
    	s.setTakeIntervalHours(7 * 24);
    	surveyDao.save(s);
    	flush();
    	
    	Survey sdb = em.find(Survey.class, "ENQ-1");
    	assertNotNull(sdb);
    	assertEquals(s.getDisplayName(), sdb.getDisplayName());
    	assertEquals(s.getEndTime(), sdb.getEndTime());
    	assertEquals(s.getProviderSurveyRef(), sdb.getProviderSurveyRef());
    	assertEquals(s.getRemarks(), sdb.getRemarks());
    	assertEquals(s.getStartTime(), sdb.getStartTime());
    	assertEquals(s.getSurveyId(), sdb.getSurveyId());
    	assertEquals(s.getTakeDelayHours(), sdb.getTakeDelayHours());
    	assertEquals(s.getTakeIntervalHours(), sdb.getTakeIntervalHours());
    }

    @Test
    public void findSurveyToTake_Simple() throws Exception {
    	createSurvey("ENQ-99", null, null, null, null);

    	Optional<Survey> result = surveyDao.findSurveyToTake(Instant.parse("2021-08-31T12:00:00Z"), Instant.parse("2021-08-31T18:00:00Z"));
    	assertTrue(result.isEmpty());

    	result = surveyDao.findSurveyToTake(Instant.parse("2021-09-02T18:00:00Z"), Instant.parse("2021-08-31T12:00:00Z"));
    	assertTrue(result.isPresent());
    }
    
    @Test
    public void findSurveyToTake() throws Exception {
    	createSurvey("ENQ-2", "2021-09-01T00:00:00Z", "2021-10-01T00:00:00Z", 24, 7 * 24);
    	Optional<Survey> result = surveyDao.findSurveyToTake(Instant.parse("2021-08-31T12:00:00Z"), Instant.parse("2021-08-31T18:00:00Z"));
    	assertTrue(result.isEmpty());

    	// Now too early
    	result = surveyDao.findSurveyToTake(Instant.parse("2021-08-31T03:00:00Z"), Instant.parse("2021-08-31T06:00:00Z"));
    	assertTrue(result.isEmpty());

    	
    	result = surveyDao.findSurveyToTake(Instant.parse("2021-09-01T12:00:00Z"), Instant.parse("2021-08-31T06:00:00Z"));
    	assertFalse(result.isEmpty());

    	result = surveyDao.findSurveyToTake(Instant.parse("2021-09-02T12:00:00Z"), Instant.parse("2021-08-31T06:00:00Z"));
    	assertFalse(result.isEmpty());

    	result = surveyDao.findSurveyToTake(Instant.parse("2021-09-03T12:00:00Z"), Instant.parse("2021-09-01T01:00:00Z"));
    	assertFalse(result.isEmpty());
    	
    	result = surveyDao.findSurveyToTake(Instant.parse("2021-09-12T12:00:00Z"), Instant.parse("2021-09-01T01:00:00Z"));
    	assertTrue(result.isEmpty());
    }
    
}
