package eu.netmobiel.commons.util;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.netmobiel.commons.exception.BadRequestException;

public class UrnHelperTest {
	private static final String TEST_SERVICE = "test";
	private static final String URN_PREFIX = UrnHelper.createUrnPrefix(TEST_SERVICE, UrnHelperTest.class);

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws BadRequestException {
		String urn = UrnHelper.createUrn(URN_PREFIX, 42L);
		assertTrue(UrnHelper.isUrn(urn));
		assertNull(UrnHelper.getId(null));
		assertEquals(42L, UrnHelper.getId("42").longValue());
		assertEquals(42L, UrnHelper.getId(URN_PREFIX, urn).longValue());
		assertEquals(TEST_SERVICE, UrnHelper.getService(urn));
		assertEquals(URN_PREFIX, UrnHelper.getPrefix(urn));
	}

}
