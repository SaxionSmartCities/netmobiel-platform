package eu.netmobiel.profile.repository;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.profile.model.LuggageOption;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.model.TraverseMode;
import eu.netmobiel.profile.test.Fixture;
import eu.netmobiel.profile.test.ProfileIntegrationTestBase;

@RunWith(Arquillian.class)
public class ProfileDaoIT extends ProfileIntegrationTestBase {
    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive archive = createDeploymentBase()
            .addClass(ProfileDao.class);
//		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private ProfileDao profileDao;

	@Inject
    private Logger log;

    private Profile driver1;
    private Profile passenger1;


    protected void insertData() throws Exception {
        driver1 = Fixture.createDriver1();
		em.persist(driver1);

		passenger1= Fixture.createPassenger1();
		em.persist(passenger1);
    }

    
    @Test
    public void savePassenger() throws Exception {
    	log.debug("Start of test: savePassenger");
    	var birthDay = "1946-10-30";
    	var token = "Hup-234";
    	var address = Fixture.createAddressLichtenvoorde();
    	var path = "/images/my/image.png";
    	var phoneNr = "+31612345678";
    	var consent16 = true;
    	var p = Fixture.createPassenger2();
    	log.debug("Create passenger2 with address");
    	p.setDateOfBirth(LocalDate.parse(birthDay));
    	p.setFcmToken(token);
    	p.addAddress(address);
    	p.setHomeAddress(address);
    	p.setImagePath(path);
    	p.setPhoneNumber(phoneNr);
    	p.getConsent().setOlderThanSixteen(consent16);
    	p.getNotificationOptions().setMessages(false);
    	p.getNotificationOptions().setTripReminders(false);
    	p.getRidesharePreferences().setMaxMinutesDetour(30);
    	p.getRidesharePreferences().getLuggageOptions().add(LuggageOption.PET);
    	p.getSearchPreferences().getAllowedTraverseModes().remove(TraverseMode.RAIL);
    	p.getSearchPreferences().setNumberOfPassengers(2);
    	p.getSearchPreferences().getLuggageOptions().add(LuggageOption.WALKER);
    	profileDao.save(p);
    	flush();
    	
    	log.debug("Fetch full profile passenger2");
    	p = profileDao.loadGraph(p.getId(), Profile.FULL_PROFILE_ENTITY_GRAPH).get();
    	
    	log.debug("Check properties");
    	assertNotNull(p.getEmail());
    	assertNotNull(p.getFamilyName());
    	assertNotNull(p.getGivenName());
    	assertNotNull(p.getManagedIdentity());
    	assertNotNull(p.getDateOfBirth());
    	assertEquals(birthDay, p.getDateOfBirth().format(DateTimeFormatter.ISO_DATE));
    	assertNotNull(p.getFcmToken());
    	assertEquals(token, p.getFcmToken());
    	assertNotNull(p.getPhoneNumber());
    	assertEquals(phoneNr, p.getPhoneNumber());
    	
    	log.debug("Check address");
    	assertNotNull(p.getHomeAddress());
    	assertEquals(address.getCountryCode(), p.getHomeAddress().getCountryCode());
    	assertEquals(address.getLocality(), p.getHomeAddress().getLocality());
    	assertEquals(address.getLocation(), p.getHomeAddress().getLocation());
    	assertEquals(address.getStreet(), p.getHomeAddress().getStreet());
    	assertEquals(address.getHouseNumber(), p.getHomeAddress().getHouseNumber());
    	assertEquals(address.getPostalCode(), p.getHomeAddress().getPostalCode());
    	
    	log.debug("Check consent");
    	assertNotNull(p.getConsent());
    	assertEquals(consent16, p.getConsent().isOlderThanSixteen());

    	log.debug("Check notification options");
    	assertNotNull(p.getNotificationOptions());
    	assertEquals(false, p.getNotificationOptions().isMessages());
    	assertEquals(true, p.getNotificationOptions().isShoutouts());
    	assertEquals(true, p.getNotificationOptions().isTripConfirmations());
    	assertEquals(false, p.getNotificationOptions().isTripReminders());
    	assertEquals(true, p.getNotificationOptions().isTripUpdates());
    	/**
    	 * What is happening: Everything is fetched in a single query (full profile), except for the luggage options.
    	 */
    	log.debug("Check rideshare preferences");
    	assertNotNull(p.getRidesharePreferences());
    	assertEquals(30, p.getRidesharePreferences().getMaxMinutesDetour().intValue());
    	log.debug("Check rideshare preferences - luggage");
    	assertTrue(p.getRidesharePreferences().getLuggageOptions().contains(LuggageOption.PET));

    	log.debug("Check search preferences");
    	assertNotNull(p.getSearchPreferences());
    	assertEquals(2, p.getSearchPreferences().getNumberOfPassengers().intValue());
    	log.debug("Check search preferences - luggage");
    	assertTrue(p.getSearchPreferences().getLuggageOptions().contains(LuggageOption.WALKER));
    	assertFalse(p.getSearchPreferences().getAllowedTraverseModes().contains(TraverseMode.RAIL));
    	log.debug("End of test: savePassenger");
    }

    @Test
    public void mergeChanges() throws Exception {
    	log.debug("Start of test: mergeChanges");
    	var p = Fixture.createPassenger2();
    	log.debug("Create passenger2");
    	profileDao.save(p);
    	flush();

    	log.debug("Fetch passenger2, standard");
    	p = profileDao.find(p.getId()).get();
    	assertFalse(p.getConsent().isOlderThanSixteen());
    	log.debug("Set consent flag passenger 2");
    	p.getConsent().setOlderThanSixteen(true);
    	flush();
    	
    	log.debug("Fetch passenger2, standard, check consent");
    	p = profileDao.find(p.getId()).get();
    	assertTrue(p.getConsent().isOlderThanSixteen());
    	
    	log.debug("... and change searchPreferences");
    	p.getSearchPreferences().getAllowedTraverseModes().remove(TraverseMode.BUS);
    	p.getSearchPreferences().getAllowedTraverseModes().remove(TraverseMode.RAIL);
    	flush();

    	log.debug("Fetch passenger2, full profile");
    	p = profileDao.loadGraph(p.getId(), Profile.FULL_PROFILE_ENTITY_GRAPH).get();
    	var address = Fixture.createAddressLichtenvoorde();
    	log.debug("Add home address to passenger2");
//    	em.persist(address);
    	p.addAddress(address);
    	p.setHomeAddress(address);
    	flush();

    	p = profileDao.loadGraph(p.getId(), Profile.FULL_PROFILE_ENTITY_GRAPH).get();
    	log.debug("... and check searchPreferences");
    	p.getSearchPreferences().getAllowedTraverseModes().remove(TraverseMode.BUS);
    	assertFalse(p.getSearchPreferences().getAllowedTraverseModes().contains(TraverseMode.BUS));
    	assertFalse(p.getSearchPreferences().getAllowedTraverseModes().contains(TraverseMode.RAIL));
    	log.debug("End of test: mergeChanges");
    }

    @Test
    public void removeProfile() throws Exception {
    	log.debug("Start of test: removeProfile");
    	var p = Fixture.createPassenger2();
    	log.debug("Create passenger2");
    	profileDao.save(p);
    	var address = Fixture.createAddressLichtenvoorde();
    	log.debug("Add Address for passenger2");
    	em.persist(address);
    	p.addAddress(address);
    	p.setHomeAddress(address);
    	flush();

    	/**
    	 * What happens: With a standard load, 4 queries are fired: Fetch profile, fetch search preferences, 
    	 * fetch rideshare preferences, fetch addresses.
    	 * With full profile load, everything is fetched in a single query.
    	 * The remove issues automatically queries to remove all child items, also for collection elements. 
    	 */
    	log.debug("Fetch profile full");
    	p = profileDao.loadGraph(p.getId(), Profile.FULL_PROFILE_ENTITY_GRAPH).get();
    	log.debug("Remove profile");
    	profileDao.remove(p);
    	log.debug("End of test: removeProfile");
    }

    @Test
    public void shoutOutCircles() throws Exception {
    	var carla1 = profileDao.find(driver1.getId()).get();
    	var addrCarla1 = Fixture.createAddressLichtenvoorde();
    	carla1.addAddress(addrCarla1);
    	carla1.setHomeAddress(addrCarla1);
    	var carla2 = Fixture.createDriver2();
    	var addrCarla2 = Fixture.createAddressHengelo();
    	carla2.addAddress(addrCarla2);
    	carla2.setHomeAddress(addrCarla2);
    	profileDao.save(carla2);
    	flush();

    	carla1 = profileDao.loadGraph(carla1.getId(), Profile.HOME_PROFILE_ENTITY_GRAPH).get();
    	assertNotNull(carla1.getHomeAddress());
    	
    	carla2 = profileDao.loadGraph(carla2.getId(), Profile.HOME_PROFILE_ENTITY_GRAPH).get();
    	assertNotNull(carla2.getHomeAddress());
    	// So now , we have two drivers: Lichtenvoorde (carla1) and Hengelo (carla2).
    	// We want a ride from Zieuwent to Doetinchem. Lichtenvoorde is nearby, Hengelo is not.
    	
    	List<Profile> drivers = profileDao.searchShoutOutProfiles(Fixture.placeZieuwent, Fixture.placeSlingeland, 30000, 10000);
    	assertNotNull(drivers);
    	assertEquals(1, drivers.size());
    	assertEquals(carla1, drivers.get(0));

    	// Other way around should also be possible
    	
    	drivers = profileDao.searchShoutOutProfiles(Fixture.placeSlingeland, Fixture.placeZieuwent, 30000, 10000);
    	assertNotNull(drivers);
    	assertEquals(1, drivers.size());
    	assertEquals(carla1, drivers.get(0));
    	
    	drivers = profileDao.searchShoutOutProfiles(Fixture.placeZieuwent, Fixture.placeSlingeland, 10000, 10000);
    	assertNotNull(drivers);
    	assertEquals(0, drivers.size());

    	drivers = profileDao.searchShoutOutProfiles(Fixture.placeSlingeland, Fixture.placeZieuwent, 10000, 10000);
    	assertNotNull(drivers);
    	assertEquals(0, drivers.size());
    }
}
