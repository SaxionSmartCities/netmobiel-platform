package eu.netmobiel.rideshare.model;

import static org.junit.Assert.*;

import java.time.Instant;

import org.junit.Before;
import org.junit.Test;

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
}
