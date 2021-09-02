package eu.netmobiel.profile.model;

import static org.junit.Assert.*;

import java.time.Instant;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.netmobiel.profile.test.Fixture;

public class SurveyInteractionTest {
    private static final Logger logger = LoggerFactory.getLogger(SurveyInteractionTest.class);

	private static final int HOUR = 3600;
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
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
    	return s;
    }
    
	@Test
	public void testGetExpirationTime() {
		Survey s = createSurvey("QC-2", "2021-09-01T00:00:00Z", "2021-10-01T00:00:00Z", 24, 7 * 24);
		Profile p = Fixture.createPassenger1();
		SurveyInteraction si = new SurveyInteraction(s, p, Instant.parse("2021-09-02T00:00:00Z"));
		logger.debug("ExpirationTime: " + si.getExpirationTime());
		assertNotNull(si.getExpirationTime());
		assertTrue(si.getExpirationTime().equals(si.getTriggerTime().plusSeconds(s.getTakeDelayHours() * HOUR).plusSeconds(s.getTakeIntervalHours() * HOUR)));
		assertTrue(si.getExpirationTime().isBefore(s.getEndTime()));

		si = new SurveyInteraction(s, p, Instant.parse("2021-09-29T00:00:00Z"));
		logger.debug("ExpirationTime: " + si.getExpirationTime());
		assertNotNull(si.getExpirationTime());
		assertTrue(si.getExpirationTime().equals(s.getEndTime()));

		s.setEndTime(null);
		logger.debug("ExpirationTime: " + si.getExpirationTime());
		assertNotNull(si.getExpirationTime());
		assertTrue(si.getExpirationTime().equals(si.getTriggerTime().plusSeconds(s.getTakeDelayHours() * HOUR).plusSeconds(s.getTakeIntervalHours() * HOUR)));

		s.setTakeDelayHours(0);
		logger.debug("ExpirationTime: " + si.getExpirationTime());
		assertNotNull(si.getExpirationTime());
		assertTrue(si.getExpirationTime().equals(si.getTriggerTime().plusSeconds(s.getTakeDelayHours() * HOUR).plusSeconds(s.getTakeIntervalHours() * HOUR)));

		s.setTakeIntervalHours(null);
		logger.debug("ExpirationTime: " + si.getExpirationTime());
		assertNull(si.getExpirationTime());

		s.setEndTime(Instant.parse("2021-10-01T00:00:00Z"));
		logger.debug("ExpirationTime: " + si.getExpirationTime());
		assertNotNull(si.getExpirationTime());
		assertTrue(si.getExpirationTime().equals(s.getEndTime()));

	}

	@Test
	public void testIsExpired() {
		Survey s = createSurvey("QC-2", "2021-09-01T00:00:00Z", "2021-10-01T00:00:00Z", 0, null);
		Profile p = Fixture.createPassenger1();
		SurveyInteraction si = new SurveyInteraction(s, p, Instant.parse("2021-09-02T00:00:00Z"));
		logger.debug("ExpirationTime: " + si.getExpirationTime());

		assertFalse(si.isExpired(s.getEndTime().minusSeconds(1 *HOUR)));
		assertFalse(si.isExpired(s.getEndTime()));
		assertTrue(si.isExpired(s.getEndTime().plusSeconds(1 * HOUR)));
	}
}
