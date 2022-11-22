package eu.netmobiel.rideshare.model;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.PaymentState;

public class RideTest {

	private Ride referenceRide;
	
	@Before
    public void createReferenceRide() throws Exception {
		referenceRide = new Ride();
    	referenceRide.setDepartureTime(Instant.parse("2020-05-19T12:00:00Z"));
    	referenceRide.setArrivalTime(Instant.parse("2020-05-19T13:00:00Z"));
    }

	@Test
	public void testHasTemporalOverlap_Same() {
		Ride r = new Ride();
    	r.setDepartureTime(referenceRide.getDepartureTime());
    	r.setArrivalTime(referenceRide.getArrivalTime());

    	assertTrue(r.hasTemporalOverlap(referenceRide));
    	assertTrue(referenceRide.hasTemporalOverlap(r));
	}

	@Test
	public void testHasTemporalOverlap_PartBefore() {
		Ride r = new Ride();
    	r.setDepartureTime(referenceRide.getDepartureTime().minusSeconds(15 * 60));
    	r.setArrivalTime(referenceRide.getArrivalTime().minusSeconds(15 * 60));

    	assertTrue(r.hasTemporalOverlap(referenceRide));
    	assertTrue(referenceRide.hasTemporalOverlap(r));
	}

	@Test
	public void testHasTemporalOverlap_PartAfter() {
		Ride r = new Ride();
    	r.setDepartureTime(referenceRide.getDepartureTime().plusSeconds(15 * 60));
    	r.setArrivalTime(referenceRide.getArrivalTime().plusSeconds(15 * 60));

    	assertTrue(r.hasTemporalOverlap(referenceRide));
    	assertTrue(referenceRide.hasTemporalOverlap(r));
	}

	@Test
	public void testHasTemporalOverlap_Inside() {
		Ride r = new Ride();
    	r.setDepartureTime(referenceRide.getDepartureTime().plusSeconds(15 * 60));
    	r.setArrivalTime(referenceRide.getArrivalTime().minusSeconds(15 * 60));

    	assertTrue(r.hasTemporalOverlap(referenceRide));
    	assertTrue(referenceRide.hasTemporalOverlap(r));
	}

	@Test
	public void testHasTemporalOverlap_Outside() {
		Ride r = new Ride();
    	r.setDepartureTime(referenceRide.getDepartureTime().minusSeconds(15 * 60));
    	r.setArrivalTime(referenceRide.getArrivalTime().plusSeconds(15 * 60));

    	assertTrue(r.hasTemporalOverlap(referenceRide));
    	assertTrue(referenceRide.hasTemporalOverlap(r));
	}

	@Test
	public void testHasTemporalOverlap_IsBefore() {
		Ride r = new Ride();
    	r.setDepartureTime(referenceRide.getDepartureTime().minusSeconds(120 * 60));
    	r.setArrivalTime(referenceRide.getDepartureTime().minusSeconds(15 * 60));

    	assertFalse(r.hasTemporalOverlap(referenceRide));
    	assertFalse(referenceRide.hasTemporalOverlap(r));
	}

	@Test
	public void testHasTemporalOverlap_IsAfter() {
		Ride r = new Ride();
    	r.setDepartureTime(referenceRide.getArrivalTime().plusSeconds(15 * 60));
    	r.setArrivalTime(referenceRide.getArrivalTime().plusSeconds(120 * 60));

    	assertFalse(r.hasTemporalOverlap(referenceRide));
    	assertFalse(referenceRide.hasTemporalOverlap(r));
	}
	
	private static Ride createRide(Instant now) {
		Ride r = new Ride();
		r.setFrom(GeoLocation.fromString("Zieuwent, Kennedystraat::52.004166,6.517835"));
		r.setTo(GeoLocation.fromString("Catharina Parkeergarage Doetinchem::51.9670528,6.2894002"));
		r.setDepartureTime(now.plusSeconds(4 * 3600));
		r.setArrivalTime(r.getDepartureTime().plusSeconds(3600));
		r.setState(RideState.SCHEDULED);
		Booking b = new Booking();
		b.setPickup(GeoLocation.fromString("Zieuwent, R.K. Kerk::52.004485,6.519542"));
		b.setDropOff(GeoLocation.fromString("Slingeland hoofdingang::51.976426,6.285741"));
		b.setNrSeats(1);
		b.setFareInCredits(10);
		b.setDepartureTime(r.getDepartureTime().plusSeconds(10 * 60));
		b.setArrivalTime(r.getArrivalTime().minusSeconds(5 * 60));
		b.setState(BookingState.CONFIRMED);
		r.addBooking(b);
		return r;
	}

	@Test
	public void testNextState_Scheduled() {
		Instant now = Instant.parse("2021-12-17T21:00:00Z");
		Ride ride = createRide(now);
		RideState state = ride.nextState(now);
		assertEquals(RideState.SCHEDULED, state);
	}

	@Test
	public void testNextState_Cancelled() {
		Instant now = Instant.parse("2021-12-17T21:00:00Z");
		Ride ride = createRide(now);
		ride.setState(RideState.CANCELLED);
		RideState state = ride.nextState(now);
		assertEquals(RideState.CANCELLED, state);
	}

	@Test
	public void testNextState_Departing() {
		Instant now = Instant.parse("2021-12-17T21:00:00Z");
		Ride ride = createRide(now);
		RideState state = ride.nextState(ride.getDepartureTime().minus(Ride.DEPARTING_PERIOD).minusSeconds(1));
		assertEquals(RideState.SCHEDULED, state);

		state = ride.nextState(ride.getDepartureTime().minus(Ride.DEPARTING_PERIOD));
		assertEquals(RideState.DEPARTING, state);

		state = ride.nextState(ride.getDepartureTime().minus(Ride.DEPARTING_PERIOD).plusSeconds(1));
		assertEquals(RideState.DEPARTING, state);
	}

	@Test
	public void testNextState_InTransit() {
		Instant now = Instant.parse("2021-12-17T21:00:00Z");
		Ride ride = createRide(now);
		RideState state = ride.nextState(ride.getDepartureTime().minusSeconds(1));
		assertEquals(RideState.DEPARTING, state);

		state = ride.nextState(ride.getDepartureTime());
		assertEquals(RideState.IN_TRANSIT, state);

		state = ride.nextState(ride.getDepartureTime().plusSeconds(1));
		assertEquals(RideState.IN_TRANSIT, state);

		state = ride.nextState(ride.getArrivalTime().minusSeconds(1));
		assertEquals(RideState.IN_TRANSIT, state);

		state = ride.nextState(ride.getArrivalTime());
		assertNotEquals(RideState.IN_TRANSIT, state);

		state = ride.nextState(ride.getArrivalTime().plusSeconds(1));
		assertNotEquals(RideState.IN_TRANSIT, state);
	}

	@Test
	public void testNextState_Arriving() {
		Instant now = Instant.parse("2021-12-17T21:00:00Z");
		Ride ride = createRide(now);
		RideState state = ride.nextState(ride.getArrivalTime().minusSeconds(1));
		assertEquals(RideState.IN_TRANSIT, state);

		state = ride.nextState(ride.getArrivalTime());
		assertEquals(RideState.ARRIVING, state);

		state = ride.nextState(ride.getArrivalTime().plusSeconds(1));
		assertEquals(RideState.ARRIVING, state);

	}

	@Test
	public void testNextState_WithoutValidating() {
		Instant now = Instant.parse("2021-12-17T21:00:00Z");
		Ride ride = createRide(now);
		ride.getBookings().clear();
		RideState state = ride.nextState(ride.getArrivalTime().plus(Ride.ARRIVING_PERIOD).minusSeconds(1));
		assertEquals(RideState.ARRIVING, state);

		state = ride.nextState(ride.getArrivalTime().plus(Ride.ARRIVING_PERIOD));
		assertEquals(RideState.COMPLETED, state);

		state = ride.nextState(ride.getArrivalTime().plus(Ride.ARRIVING_PERIOD).plusSeconds(1));
		assertEquals(RideState.COMPLETED, state);
	}

	@Test
	public void testNextState_WithValidating() {
		Instant now = Instant.parse("2021-12-17T21:00:00Z");
		Ride ride = createRide(now);
		Optional<Booking> optb = ride.getConfirmedBooking();
		assertTrue(optb.isPresent());
		Booking b = optb.get();
		
		RideState state = ride.nextState(ride.getArrivalTime().plus(Ride.ARRIVING_PERIOD).minusSeconds(1));
		assertEquals(RideState.ARRIVING, state);

		state = ride.nextState(ride.getArrivalTime().plus(Ride.ARRIVING_PERIOD));
		assertEquals(RideState.VALIDATING, state);

		b.setPaymentState(PaymentState.CANCELLED);
		state = ride.nextState(ride.getArrivalTime().plus(Ride.ARRIVING_PERIOD));
		assertEquals(RideState.COMPLETED, state);

		b.setPaymentState(PaymentState.PAID);
		state = ride.nextState(ride.getArrivalTime().plus(Ride.ARRIVING_PERIOD));
		assertEquals(RideState.COMPLETED, state);
		
	}

}
