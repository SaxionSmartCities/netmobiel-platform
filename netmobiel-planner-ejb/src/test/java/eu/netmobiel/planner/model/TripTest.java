package eu.netmobiel.planner.model;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TripTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testUpdateTripState() {
		Trip trip = new Trip();
		trip.setState(TripState.PLANNING);
		Itinerary it = new Itinerary();
		trip.setItinerary(it);
		Leg leg1 = new Leg();
		it.getLegs().add(leg1);
		leg1.setState(TripState.BOOKING);
		Leg leg2 = new Leg();
		leg2.setState(TripState.SCHEDULED);
		it.getLegs().add(leg2);
		assertEquals(TripState.PLANNING, trip.getState());
		trip.updateTripState();
		assertEquals(TripState.BOOKING, trip.getState());
		
		leg1.setState(TripState.SCHEDULED);
		trip.updateTripState();
		assertEquals(TripState.SCHEDULED, trip.getState());
	}

}
