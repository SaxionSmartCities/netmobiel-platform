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

import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.model.Survey;
import eu.netmobiel.profile.model.SurveyInteraction;
import eu.netmobiel.profile.test.Fixture;
import eu.netmobiel.profile.test.ProfileIntegrationTestBase;

@RunWith(Arquillian.class)
public class SurveyInteractionDaoIT extends ProfileIntegrationTestBase {
    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive archive = createDeploymentBase()
            .addClass(SurveyInteractionDao.class);
//		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private SurveyInteractionDao surveyInteractionDao;

	@SuppressWarnings("unused")
	@Inject
    private Logger log;

	private Profile profile;
	private Survey survey;

	@Override
	protected void insertData() throws Exception {
		profile = Fixture.createPassenger1();
		em.persist(profile);
    	survey = createSurvey("QC-99", null, null, null, null);
    	em.persist(survey);
    }

    protected Survey createSurvey(String id, String start, String end, Integer delay, Integer interval) {
    	Survey s = new Survey();
    	s.setSurveyId(id);
    	s.setDisplayName("Dit is " + id);
    	s.setStartTime(start == null ? null :Instant.parse(start));
    	s.setEndTime(end == null ? null : Instant.parse(end));
    	s.setRemarks("Opmerkingen over " + id);
    	s.setTakeDelayHours(delay);
    	s.setTakeIntervalHours(interval);
    	em.persist(s);
    	return s;
    }
    
    @Test
    public void createSurveyInteraction() throws Exception {
		SurveyInteraction si = new SurveyInteraction(survey, profile);
    	surveyInteractionDao.save(si);
    	flush();
    	
    	SurveyInteraction sidb = em.createQuery("from SurveyInteraction where id = :id", SurveyInteraction.class)
    			.setParameter("id", si.getId())
    			.getSingleResult();
    	assertNotNull(sidb);
    	assertNotNull(sidb.getInvitationTime());
    	assertEquals(si.getInvitationCount(), sidb.getInvitationCount());
    	assertEquals(si.getRedirectCount(), sidb.getRedirectCount());
    	assertEquals(si.getRedirectTime(), sidb.getRedirectTime());
    	assertEquals(si.getSubmitTime(), sidb.getSubmitTime());
    }

    @Test
    public void findSurveyInteraction() throws Exception {

    	Optional<SurveyInteraction> result = surveyInteractionDao.findInteraction(survey, profile);
    	assertFalse(result.isPresent());

    	SurveyInteraction si = new SurveyInteraction(survey, profile);
    	surveyInteractionDao.save(si);
    	flush();

    	result = surveyInteractionDao.findInteraction(survey, profile);
    	assertTrue(result.isPresent());
    }
    
}
