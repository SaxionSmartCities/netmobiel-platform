package eu.netmobiel.planner.service;

import static org.junit.Assert.*;

import java.time.Instant;

import javax.enterprise.event.Event;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.netmobiel.commons.exception.ApplicationException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.planner.model.PlanType;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.model.User;
import eu.netmobiel.planner.repository.OpenTripPlannerDao;
import eu.netmobiel.planner.repository.OtpClusterDao;
import eu.netmobiel.planner.repository.TripPlanDao;
import eu.netmobiel.planner.test.Fixture;
import eu.netmobiel.rideshare.service.RideManager;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;

public class TripPlanManagerTest {

	@SuppressWarnings("unused")
	private Logger log = LoggerFactory.getLogger(TripPlanManagerTest.class);
	
	@Tested
	private TripPlanManager tested;
	
	@Injectable
	private Logger logger;
	
	@Injectable
    private TripPlanDao tripPlanDao;

	@Injectable
    private OtpClusterDao otpClusterDao;
	
	@Injectable
    private OpenTripPlannerDao otpDao;

	@Injectable
    private Event<TripPlan> shoutOutRequestedEvent;

	@Injectable
    private RideManager rideManager;

	@Injectable
	private User traveller;
	
	@Before
	public void setUp() throws Exception {
		traveller = new User("ID1", "Pietje", "Puk", "pietje@puk.me");
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testListTrips() {
		PlanType planType = PlanType.REGULAR;
		Instant since = Instant.parse("2020-06-24T00:00:00Z");
		Instant until = Instant.parse("2020-06-25T00:00:00Z");
		Boolean inProgressOnly = true;
		SortDirection sortDir = SortDirection.ASC;
		Integer maxResults = 9;
		Integer offset = 1;
		new Expectations() {{
			tripPlanDao.findTripPlans(traveller, planType, since, until, inProgressOnly, sortDir, 0, 0);
			result = PagedResult.empty();
		}};
		try {
			tested.listTripPlans(traveller, planType, since, until, inProgressOnly, sortDir, maxResults, offset);
		} catch (ApplicationException ex) {
			fail("Unexpected exception: " + ex);
		}
		new Verifications() {{
			// Verify call to DAO. No results returned, so no second call.
			tripPlanDao.findTripPlans(traveller, planType, since, until, inProgressOnly, sortDir, 0, 0);
			times = 1;
//			tripDao.findTrips(traveller, state, since, until, deletedToo, sortDir, maxResults, offset);
//			times = 1;
		}};
	}

	@Test
	public void testListShoutOuts() {
		GeoLocation location = Fixture.placeCentrumDoetinchem;
		Instant start = Instant.parse("2020-06-24T00:00:00Z");
		Integer depArrRadius = 10000;
		Integer travelRadius = 30000;
		Integer maxResults = 9;
		Integer offset = 1;
		new Expectations() {{
			tripPlanDao.findShoutOutPlans(location, start, depArrRadius, travelRadius, 0, 0);
			result = PagedResult.empty();
		}};
		tested.listShoutOuts(location, start, depArrRadius, travelRadius, maxResults, offset);
		new Verifications() {{
			// Verify call to DAO. No results returned, so no second call.
			tripPlanDao.findShoutOutPlans(location, start, depArrRadius, travelRadius, 0, 0);
			times = 1;
//    			tripDao.findTrips(traveller, state, since, until, deletedToo, sortDir, maxResults, offset);
//    			times = 1;
		}};
	}
}
