package eu.netmobiel.planner.model;

import static org.junit.Assert.*;

import java.time.Instant;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PaymentState;

public class LegTest {
    @SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(LegTest.class);

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	private static Leg createLeg(Instant now) {
		Leg leg = new Leg();
		leg.setDuration(3600);
    	Stop stopFrom = new Stop(GeoLocation.fromString("Zieuwent, R.K. Kerk::52.004485,6.519542"));
    	stopFrom.setDepartureTime(now.plusSeconds(4 * 3600));
    	leg.setFrom(stopFrom);
    	Stop stopTo = new Stop(GeoLocation.fromString("Ruurlo, Station::52.081233,6.45004"));
    	stopTo.setArrivalTime(stopFrom.getDepartureTime().plusSeconds(leg.getDuration()));
    	leg.setTo(stopTo);
    	return leg;
	}

	@Test
	public void testNextState_TransitPlanning() {
		Instant now = Instant.parse("2021-12-17T21:00:00Z");
		Leg leg = createLeg(now);
		TripState state = leg.nextState(now);
		assertEquals(TripState.SCHEDULED, state);
	}

	@Test
	public void testNextState_Cancelled() {
		Instant now = Instant.parse("2021-12-17T21:00:00Z");
		Leg leg = createLeg(now);
		leg.setBookingRequired(true);
		leg.setState(TripState.CANCELLED);
		TripState state = leg.nextState(now);
		assertEquals(TripState.CANCELLED, state);
	}

	@Test
	public void testNextState_RidesharePlanning() {
		Instant now = Instant.parse("2021-12-17T21:00:00Z");
		Leg leg = createLeg(now);
		leg.setBookingRequired(true);

		TripState state = leg.nextState(now);
		assertEquals(TripState.BOOKING, state);
		
		leg.setBookingId("urn:nb:rs:booking:123");
		state = leg.nextState(now);
		assertEquals(TripState.BOOKING, state);
		
		leg.setBookingConfirmed(false);
		state = leg.nextState(now);
		assertEquals(TripState.BOOKING, state);

		leg.setBookingConfirmed(true);
		state = leg.nextState(now);
		assertEquals(TripState.SCHEDULED, state);
	}

	@Test
	public void testNextState_Departing() {
		Instant now = Instant.parse("2021-12-17T21:00:00Z");
		Leg leg = createLeg(now);
		TripState state = leg.nextState(leg.getStartTime().minus(Leg.DEPARTING_PERIOD).minusSeconds(1));
		assertEquals(TripState.SCHEDULED, state);

		state = leg.nextState(leg.getStartTime().minus(Leg.DEPARTING_PERIOD));
		assertEquals(TripState.DEPARTING, state);

		state = leg.nextState(leg.getStartTime().minus(Leg.DEPARTING_PERIOD).plusSeconds(1));
		assertEquals(TripState.DEPARTING, state);
	}

	@Test
	public void testNextState_inTransit() {
		Instant now = Instant.parse("2021-12-17T21:00:00Z");
		Leg leg = createLeg(now);
		TripState state = leg.nextState(leg.getStartTime().minusSeconds(1));
		assertEquals(TripState.DEPARTING, state);

		state = leg.nextState(leg.getStartTime());
		assertEquals(TripState.IN_TRANSIT, state);

		state = leg.nextState(leg.getStartTime().plusSeconds(1));
		assertEquals(TripState.IN_TRANSIT, state);
	}

	@Test
	public void testNextState_Arriving() {
		Instant now = Instant.parse("2021-12-17T21:00:00Z");
		Leg leg = createLeg(now);
		TripState state = leg.nextState(leg.getEndTime().minusSeconds(1));
		assertEquals(TripState.IN_TRANSIT, state);

		state = leg.nextState(leg.getEndTime());
		assertEquals(TripState.ARRIVING, state);

		state = leg.nextState(leg.getEndTime().plusSeconds(1));
		assertEquals(TripState.ARRIVING, state);
	}

	@Test
	public void testNextState_WithoutValidating() {
		Instant now = Instant.parse("2021-12-17T21:00:00Z");
		Leg leg = createLeg(now);

		TripState state = leg.nextState(leg.getEndTime().plus(Leg.ARRIVING_PERIOD).minusSeconds(1));
		assertEquals(TripState.ARRIVING, state);

		state = leg.nextState(leg.getEndTime().plus(Leg.ARRIVING_PERIOD));
		assertEquals(TripState.COMPLETED, state);

		state = leg.nextState(leg.getEndTime().plus(Leg.ARRIVING_PERIOD).plusSeconds(1));
		assertEquals(TripState.COMPLETED, state);
	}

	@Test
	public void testNextState_WithValidating() {
		Instant now = Instant.parse("2021-12-17T21:00:00Z");
		Leg leg = createLeg(now);
		leg.setPaymentState(PaymentState.RESERVED);
		leg.setConfirmationRequested(true);
		leg.setConfirmationByProviderRequested(true);
		
		TripState state = leg.nextState(leg.getEndTime().plus(Leg.ARRIVING_PERIOD).minusSeconds(1));
		assertEquals(TripState.ARRIVING, state);

		state = leg.nextState(leg.getEndTime().plus(Leg.ARRIVING_PERIOD));
		assertEquals(TripState.VALIDATING, state);

		leg.setPaymentState(PaymentState.CANCELLED);
		state = leg.nextState(leg.getEndTime().plus(Leg.ARRIVING_PERIOD));
		assertEquals(TripState.COMPLETED, state);

		leg.setPaymentState(PaymentState.PAID);
		state = leg.nextState(leg.getEndTime().plus(Leg.ARRIVING_PERIOD));
		assertEquals(TripState.COMPLETED, state);
		
	}
}
