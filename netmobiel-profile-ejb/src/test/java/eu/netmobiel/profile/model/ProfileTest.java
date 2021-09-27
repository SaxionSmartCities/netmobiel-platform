package eu.netmobiel.profile.model;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ProfileTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void createProfile_Default() {
		Profile p = new Profile();
		p.initializeChildren();
		assertNotNull(p.getConsent());
//		assertNull(p.getHomeAddress());
//		assertNull(p.getHomeLocation());
		assertNotNull(p.getNotificationOptions());
		assertEquals(UserRole.PASSENGER, p.getUserRole());
		assertNotNull(p.getSearchPreferences());
		assertNull(p.getRidesharePreferences());
	}

	@Test
	public void createProfile_NetMobielPassenger() {
		Profile p = new Profile(null, "Jaap", "Reitsma", "j.reitsma@saxion.nl", UserRole.PASSENGER);
		p.initializeChildren();
		assertNotNull(p.getConsent());
//		assertNull(p.getHomeAddress());
//		assertNull(p.getHomeLocation());
		assertNotNull(p.getNotificationOptions());
		assertNotNull(p.getSearchPreferences());
		assertNull(p.getRidesharePreferences());
	}

	@Test
	public void createProfile_NetMobielDriver() {
		Profile p = new Profile(null, "Jaap", "Reitsma", "j.reitsma@saxion.nl", UserRole.DRIVER);
		p.initializeChildren();
		assertNotNull(p.getConsent());
//		assertNull(p.getHomeAddress());
//		assertNull(p.getHomeLocation());
		assertNotNull(p.getNotificationOptions());
		assertNull(p.getSearchPreferences());
		assertNotNull(p.getRidesharePreferences());
	}

	@Test
	public void createProfile_NetMobielBoth() {
		Profile p = new Profile(null, "Jaap", "Reitsma", "j.reitsma@saxion.nl", UserRole.BOTH);
		p.initializeChildren();
		assertNotNull(p.getConsent());
//		assertNull(p.getHomeAddress());
//		assertNull(p.getHomeLocation());
		assertNotNull(p.getNotificationOptions());
		assertNotNull(p.getSearchPreferences());
		assertNotNull(p.getRidesharePreferences());
	}
}
