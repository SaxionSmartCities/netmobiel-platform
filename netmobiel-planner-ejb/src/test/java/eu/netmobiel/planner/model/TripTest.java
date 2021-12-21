package eu.netmobiel.planner.model;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PaymentState;

public class TripTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	private static Trip createTrip(Instant now) {
		Itinerary it = new Itinerary();
    	it.setStops(new ArrayList<>());
    	it.setLegs(new ArrayList<>());

		Leg leg1 = new Leg();
		leg1.setDuration(30 * 3600);
    	Stop stop1 = new Stop(GeoLocation.fromString("Zieuwent, R.K. Kerk::52.004485,6.519542"));
    	stop1.setDepartureTime(now.plusSeconds(4 * 3600));
    	leg1.setFrom(stop1);
    	Stop stop2 = new Stop(GeoLocation.fromString("Ruurlo, Station::52.081233,6.45004"));
    	stop2.setArrivalTime(stop1.getDepartureTime().plusSeconds(leg1.getDuration()));
    	// Set one hour waiting time
    	stop2.setDepartureTime(stop2.getArrivalTime().plusSeconds(60 * 60));
    	leg1.setTo(stop2);
    	it.getLegs().add(leg1);
    	it.getStops().add(stop1);
    	it.getStops().add(stop2);

		Leg leg2 = new Leg();
		leg2.setDuration(40 * 3600);
    	leg2.setFrom(stop2);
    	Stop stop3 = new Stop(GeoLocation.fromString("Catharina Parkeergarage Doetinchem::51.9670528,6.2894002"));
    	stop3.setArrivalTime(stop2.getDepartureTime().plusSeconds(leg2.getDuration()));
    	leg2.setTo(stop3);
    	it.getLegs().add(leg2);
    	it.getStops().add(stop3);
    	
    	it.getLegs().forEach(leg -> leg.setTraverseMode(TraverseMode.BUS));
    	it.updateCharacteristics();

    	Trip trip = new Trip();
        trip.setCreationTime(now);
        trip.setArrivalTimeIsPinned(false);
        trip.setFrom(stop1.getLocation());
        trip.setTo(stop3.getLocation());
    	trip.setItinerary(it);
    	
    	return trip;
	}

	@Test
	public void testNextState_TransitPlanning() {
		Instant now = Instant.parse("2021-12-17T21:00:00Z");
		Trip trip = createTrip(now);
		TripState state = trip.nextStateWithLegsToo(now);
		assertEquals(TripState.SCHEDULED, state);
	}

	@Test
	public void testNextState_Cancelled() {
		Instant now = Instant.parse("2021-12-17T21:00:00Z");
		Trip trip = createTrip(now);
		trip.getLastLeg().setBookingRequired(true);
		trip.getLastLeg().setState(TripState.CANCELLED);
		TripState state = trip.nextStateWithLegsToo(now);
		assertEquals(TripState.CANCELLED, state);
	}

	@Test
	public void testNextState_RidesharePlanning() {
		Instant now = Instant.parse("2021-12-17T21:00:00Z");
		Trip trip = createTrip(now);
		trip.getLastLeg().setBookingRequired(true);

		TripState state = trip.nextStateWithLegsToo(now);
		assertEquals(TripState.BOOKING, state);
		
		trip.getLastLeg().setBookingId("urn:nb:rs:booking:123");
		state = trip.nextStateWithLegsToo(now);
		assertEquals(TripState.BOOKING, state);
		
		trip.getLastLeg().setBookingConfirmed(false);
		state = trip.nextStateWithLegsToo(now);
		assertEquals(TripState.BOOKING, state);

		trip.getLastLeg().setBookingConfirmed(true);
		state = trip.nextStateWithLegsToo(now);
		assertEquals(TripState.SCHEDULED, state);
	}

	@Test
	public void testNextState_Departing() {
		Instant now = Instant.parse("2021-12-17T21:00:00Z");
		Trip trip = createTrip(now);
		TripState state = trip.nextStateWithLegsToo(trip.getFirstLeg().getStartTime().minus(Leg.DEPARTING_PERIOD).minusSeconds(1));
		assertEquals(TripState.SCHEDULED, state);

		state = trip.nextStateWithLegsToo(trip.getFirstLeg().getStartTime().minus(Leg.DEPARTING_PERIOD));
		assertEquals(TripState.DEPARTING, state);

		state = trip.nextStateWithLegsToo(trip.getFirstLeg().getStartTime().minus(Leg.DEPARTING_PERIOD).plusSeconds(1));
		assertEquals(TripState.DEPARTING, state);
	}

	@Test
	public void testNextState_InTransit() {
		Instant now = Instant.parse("2021-12-17T21:00:00Z");
		Trip trip = createTrip(now);
		TripState state = trip.nextStateWithLegsToo(trip.getFirstLeg().getStartTime().minusSeconds(1));
		assertEquals(TripState.DEPARTING, state);

		state = trip.nextStateWithLegsToo(trip.getFirstLeg().getStartTime());
		assertEquals(TripState.IN_TRANSIT, state);

		state = trip.nextStateWithLegsToo(trip.getFirstLeg().getStartTime().plusSeconds(1));
		assertEquals(TripState.IN_TRANSIT, state);

		state = trip.nextStateWithLegsToo(trip.getLastLeg().getStartTime().minusSeconds(1));
		assertEquals(TripState.IN_TRANSIT, state);

		state = trip.nextStateWithLegsToo(trip.getLastLeg().getStartTime());
		assertEquals(TripState.IN_TRANSIT, state);

		state = trip.nextStateWithLegsToo(trip.getLastLeg().getStartTime().plusSeconds(1));
		assertEquals(TripState.IN_TRANSIT, state);
	}

	@Test
	public void testNextState_Arriving() {
		Instant now = Instant.parse("2021-12-17T21:00:00Z");
		Trip trip = createTrip(now);
		TripState state = trip.nextStateWithLegsToo(trip.getLastLeg().getEndTime().minusSeconds(1));
		assertEquals(TripState.IN_TRANSIT, state);

		state = trip.nextStateWithLegsToo(trip.getLastLeg().getEndTime());
		assertEquals(TripState.ARRIVING, state);

		state = trip.nextStateWithLegsToo(trip.getLastLeg().getEndTime().plusSeconds(1));
		assertEquals(TripState.ARRIVING, state);

	}

	@Test
	public void testNextState_WithoutValidating() {
		Instant now = Instant.parse("2021-12-17T21:00:00Z");
		Trip trip = createTrip(now);

		TripState state = trip.nextStateWithLegsToo(trip.getLastLeg().getEndTime().plus(Leg.ARRIVING_PERIOD).minusSeconds(1));
		assertEquals(TripState.ARRIVING, state);

		state = trip.nextStateWithLegsToo(trip.getLastLeg().getEndTime().plus(Leg.ARRIVING_PERIOD));
		assertEquals(TripState.COMPLETED, state);

		state = trip.nextStateWithLegsToo(trip.getLastLeg().getEndTime().plus(Leg.ARRIVING_PERIOD).plusSeconds(1));
		assertEquals(TripState.COMPLETED, state);
	}

	@Test
	public void testNextState_WithValidating() {
		Instant now = Instant.parse("2021-12-17T21:00:00Z");
		Trip trip = createTrip(now);
		trip.getLastLeg().setPaymentState(PaymentState.RESERVED);
		trip.getLastLeg().setConfirmationRequested(true);
		trip.getLastLeg().setConfirmationByProviderRequested(true);
		
		TripState state = trip.nextStateWithLegsToo(trip.getLastLeg().getEndTime().plus(Leg.ARRIVING_PERIOD).minusSeconds(1));
		assertEquals(TripState.ARRIVING, state);

		state = trip.nextStateWithLegsToo(trip.getLastLeg().getEndTime().plus(Leg.ARRIVING_PERIOD));
		assertEquals(TripState.VALIDATING, state);

		trip.getLastLeg().setPaymentState(PaymentState.CANCELLED);
		state = trip.nextStateWithLegsToo(trip.getLastLeg().getEndTime().plus(Leg.ARRIVING_PERIOD));
		assertEquals(TripState.COMPLETED, state);

		trip.getLastLeg().setPaymentState(PaymentState.PAID);
		state = trip.nextStateWithLegsToo(trip.getLastLeg().getEndTime().plus(Leg.ARRIVING_PERIOD));
		assertEquals(TripState.COMPLETED, state);
		
	}

}
