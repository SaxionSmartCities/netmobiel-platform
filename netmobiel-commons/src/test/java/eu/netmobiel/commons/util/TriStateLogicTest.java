package eu.netmobiel.commons.util;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TriStateLogicTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testAnd() {
		assertNull(TriStateLogic.and(null, null));
		assertFalse(TriStateLogic.and(Boolean.FALSE, null));
		assertTrue(TriStateLogic.and(Boolean.TRUE, null));

		assertFalse(TriStateLogic.and(null, Boolean.FALSE));
		assertFalse(TriStateLogic.and(Boolean.FALSE, Boolean.FALSE));
		assertFalse(TriStateLogic.and(Boolean.TRUE, Boolean.FALSE));

		assertTrue(TriStateLogic.and(null, Boolean.TRUE));
		assertFalse(TriStateLogic.and(Boolean.FALSE, Boolean.TRUE));
		assertTrue(TriStateLogic.and(Boolean.TRUE, Boolean.TRUE));
	}

	@Test
	public void testOr() {
		assertNull(TriStateLogic.or(null, null));
		assertFalse(TriStateLogic.or(Boolean.FALSE, null));
		assertTrue(TriStateLogic.or(Boolean.TRUE, null));

		assertFalse(TriStateLogic.or(null, Boolean.FALSE));
		assertFalse(TriStateLogic.or(Boolean.FALSE, Boolean.FALSE));
		assertTrue(TriStateLogic.or(Boolean.TRUE, Boolean.FALSE));

		assertTrue(TriStateLogic.or(null, Boolean.TRUE));
		assertTrue(TriStateLogic.or(Boolean.FALSE, Boolean.TRUE));
		assertTrue(TriStateLogic.or(Boolean.TRUE, Boolean.TRUE));
	}
}
