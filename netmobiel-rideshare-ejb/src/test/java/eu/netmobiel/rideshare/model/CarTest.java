package eu.netmobiel.rideshare.model;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CarTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testUnformatPlate() {
		String plate1 = "52-PH-VD";
		String plate1Raw = "52PHVD";
		String plate2 = " 5 2 - P H - V D ";
		String plate2Raw = "52PHVD";
		assertEquals(plate1Raw, Car.unformatPlate(plate1));
		assertEquals(plate2Raw, Car.unformatPlate(plate2));
	}

}
