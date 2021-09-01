package eu.netmobiel.profile.model;

import static org.junit.Assert.*;

import java.time.Instant;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SurveyTest {

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
	public void testCanBeTaken() {
		Survey s = createSurvey("QC-2", "2021-09-01T00:00:00Z", "2021-10-01T00:00:00Z", 24, 7 * 24);
		Instant triggerTime = Instant.parse("2021-08-31T00:00:00Z");
		assertFalse(s.canBeTaken(triggerTime, Instant.parse("2021-08-31T00:00:00Z")));
		triggerTime = s.getStartTime().plusSeconds(HOUR * 24);
		assertFalse(s.canBeTaken(triggerTime, triggerTime.minusSeconds(HOUR)));
		assertFalse(s.canBeTaken(triggerTime, triggerTime.plusSeconds(HOUR * (s.getTakeDelayHours() - 1))));
		assertTrue(s.canBeTaken(triggerTime, triggerTime.plusSeconds(HOUR * (s.getTakeDelayHours() + 1))));
		assertTrue(s.canBeTaken(triggerTime, triggerTime.plusSeconds(HOUR * (s.getTakeDelayHours() + s.getTakeIntervalHours() - 1))));
		assertFalse(s.canBeTaken(triggerTime, triggerTime.plusSeconds(HOUR * (s.getTakeDelayHours() + s.getTakeIntervalHours() + 1))));

		triggerTime = s.getEndTime().minusSeconds(HOUR * 24 * 3);
		assertTrue(s.canBeTaken(triggerTime, triggerTime.plusSeconds(HOUR * (s.getTakeDelayHours() + 1))));
		// Remaining window is smaller than interval length
		assertFalse(s.canBeTaken(triggerTime, triggerTime.plusSeconds(HOUR * (s.getTakeDelayHours() + s.getTakeIntervalHours() - 1))));
		assertFalse(s.canBeTaken(triggerTime, s.getEndTime().plusSeconds(HOUR * 1)));
	}

	@Test
	public void testTimeLeftToTake() {
		Survey s = createSurvey("QC-2", "2021-09-01T00:00:00Z", "2021-10-01T00:00:00Z", 24, 7 * 24);
		Instant triggerTime = Instant.parse("2021-08-31T00:00:00Z");
		assertTrue(s.timeLeftToTake(triggerTime, triggerTime.plusSeconds(s.getTakeDelayHours() * HOUR)) <= s.getTakeIntervalHours());
		assertTrue(s.timeLeftToTake(triggerTime, triggerTime.plusSeconds(s.getTakeDelayHours() * HOUR).plusSeconds(1)) < s.getTakeIntervalHours());

		triggerTime = s.getEndTime().minusSeconds(HOUR * 24 * 3);
		assertTrue(s.timeLeftToTake(triggerTime, s.getEndTime().minusSeconds(24 * HOUR)) <= 24);
	}
}
