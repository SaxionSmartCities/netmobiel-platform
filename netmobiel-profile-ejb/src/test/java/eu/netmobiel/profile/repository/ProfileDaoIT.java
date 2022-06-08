package eu.netmobiel.profile.repository;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

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
import eu.netmobiel.profile.model.Profile_;
import eu.netmobiel.profile.model.RidesharePreferences;
import eu.netmobiel.profile.model.SearchPreferences;
import eu.netmobiel.profile.model.TraverseMode;
import eu.netmobiel.profile.model.UserRole;
import eu.netmobiel.profile.test.Fixture;
import eu.netmobiel.profile.test.ProfileIntegrationTestBase;

@RunWith(Arquillian.class)
public class ProfileDaoIT extends ProfileIntegrationTestBase {
    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive archive = createDeploymentBase()
            .addClass(ProfileDao.class)
            .addClass(RidesharePreferencesDao.class)
            .addClass(SearchPreferencesDao.class);
//		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private ProfileDao profileDao;
    @Inject
    private RidesharePreferencesDao ridesharePreferencesDao;
    @Inject
    private SearchPreferencesDao searchPreferencesDao;

	@Inject
    private Logger log;

    private Profile driver1;
    private Profile passenger1;


    @Override
	protected void insertData() throws Exception {
        driver1 = Fixture.createDriver1();
		em.persist(driver1);
		em.persist(driver1.getRidesharePreferences());

		passenger1 = Fixture.createPassenger1();
		em.persist(passenger1);
		em.persist(passenger1.getSearchPreferences());
    }

    
    @Test
    public void savePassenger() throws Exception {
    	log.debug("Start of test: savePassenger");
    	var birthDay = "1946-10-30";
    	var address = Fixture.createAddressLichtenvoorde();
    	var location = Fixture.placeThuisLichtenvoorde;
    	var path = "/images/my/image.png";
    	var phoneNr = "+31612345678";
    	var consent16 = true;
    	var p = Fixture.createPassenger2();
    	log.debug("Create passenger2 with address");
    	p.setDateOfBirth(LocalDate.parse(birthDay));
    	p.setHomeAddress(address);
    	p.setHomeLocation(location);
    	p.setImagePath(path);
    	p.setPhoneNumber(phoneNr);
    	p.getConsent().setOlderThanSixteen(consent16);
    	p.getNotificationOptions().setMessages(false);
    	p.getNotificationOptions().setTripReminders(false);
    	profileDao.save(p);
//    	p.getRidesharePreferences().setMaxMinutesDetour(30);
//    	p.getRidesharePreferences().getLuggageOptions().add(LuggageOption.PET);
    	p.getSearchPreferences().getAllowedTraverseModes().remove(TraverseMode.RAIL);
    	p.getSearchPreferences().setNumberOfPassengers(2);
    	p.getSearchPreferences().getLuggageOptions().add(LuggageOption.PET);
    	searchPreferencesDao.save(p.getSearchPreferences());
    	flush();
    	
    	log.debug("Fetch profile passenger2");
    	p = profileDao.loadGraph(p.getId(), Profile.DEFAULT_PROFILE_ENTITY_GRAPH).get();
    	
    	log.debug("Check properties");
    	assertNotNull(p.getEmail());
    	assertNotNull(p.getFamilyName());
    	assertNotNull(p.getGivenName());
    	assertNotNull(p.getManagedIdentity());
    	assertNotNull(p.getDateOfBirth());
    	assertEquals(birthDay, p.getDateOfBirth().format(DateTimeFormatter.ISO_DATE));
    	assertNotNull(p.getPhoneNumber());
    	assertEquals(phoneNr, p.getPhoneNumber());
    	
    	log.debug("Check address");
    	assertNotNull(p.getHomeAddress());
    	assertEquals(address.getCountryCode(), p.getHomeAddress().getCountryCode());
    	assertEquals(address.getLocality(), p.getHomeAddress().getLocality());
    	assertEquals(address.getStreet(), p.getHomeAddress().getStreet());
    	assertEquals(address.getHouseNumber(), p.getHomeAddress().getHouseNumber());
    	assertEquals(address.getPostalCode(), p.getHomeAddress().getPostalCode());
    	assertEquals(location, p.getHomeLocation());
    	
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

    	log.debug("Check rideshare preferences");
    	assertNull(p.getRidesharePreferences());
    	p.setRidesharePreferences(ridesharePreferencesDao.find(p.getId()).orElse(null));
    	assertNull(p.getRidesharePreferences());
    	
//    	assertEquals(30, p.getRidesharePreferences().getMaxMinutesDetour().intValue());
//    	log.debug("Check rideshare preferences - luggage");
//    	assertTrue(p.getRidesharePreferences().getLuggageOptions().contains(LuggageOption.PET));

    	log.debug("Check search preferences");
    	assertNull(p.getSearchPreferences());
    	p.setSearchPreferences(searchPreferencesDao.loadGraph(p.getId(), SearchPreferences.FULL_SEARCH_PREFS_ENTITY_GRAPH).orElse(null));
    	assertNotNull(p.getSearchPreferences());
    	assertEquals(2, p.getSearchPreferences().getNumberOfPassengers().intValue());
    	log.debug("Check search preferences - luggage");
    	assertTrue(p.getSearchPreferences().getLuggageOptions().contains(LuggageOption.PET));
    	assertFalse(p.getSearchPreferences().getAllowedTraverseModes().contains(TraverseMode.RAIL));
    	log.debug("End of test: savePassenger");
    }

    @Test
    public void getProfile() throws Exception {
    	Profile p = profileDao.find(passenger1.getId()).get();
    	Optional<Profile> optp = profileDao.loadGraph(passenger1.getId(), Profile.DEFAULT_PROFILE_ENTITY_GRAPH);
    	assertFalse(optp.isEmpty());
    	p = optp.get();
    	assertTrue(profileDao.isLoaded(p, Profile_.CONSENT));
    	assertTrue(profileDao.isLoaded(p, Profile_.HOME_ADDRESS));
    	assertTrue(profileDao.isLoaded(p, Profile_.NOTIFICATION_OPTIONS));
    }

    @Test
    public void mergeChanges() throws Exception {
    	log.debug("Start of test: mergeChanges");
    	var p = Fixture.createPassenger2();
    	log.debug("Create passenger2");
    	profileDao.save(p);
    	searchPreferencesDao.save(p.getSearchPreferences());
    	flush();

    	log.debug("Fetch passenger2, standard");
    	p = profileDao.find(p.getId()).get();
    	p = profileDao.loadGraph(p.getId(), Profile.DEFAULT_PROFILE_ENTITY_GRAPH).get();
    	assertFalse(p.getConsent().isOlderThanSixteen());
    	log.debug("Set consent flag passenger 2");
    	p.getConsent().setOlderThanSixteen(true);
    	flush();
    	
    	log.debug("Fetch passenger2, standard, check consent");
    	p = profileDao.loadGraph(p.getId(), Profile.DEFAULT_PROFILE_ENTITY_GRAPH).get();
    	assertTrue(p.getConsent().isOlderThanSixteen());
    	
    	log.debug("... and change searchPreferences");
    	SearchPreferences sp = searchPreferencesDao.loadGraph(p.getId(), SearchPreferences.FULL_SEARCH_PREFS_ENTITY_GRAPH).orElse(null);
    	assertNotNull(sp);
    	sp.getAllowedTraverseModes().remove(TraverseMode.BUS);
    	sp.getAllowedTraverseModes().remove(TraverseMode.RAIL);
    	flush();

    	log.debug("Fetch passenger2, full profile");
    	p = profileDao.loadGraph(p.getId(), Profile.DEFAULT_PROFILE_ENTITY_GRAPH).get();
    	var address = Fixture.createAddressLichtenvoorde();
    	log.debug("Add home address to passenger2");
    	p.setHomeAddress(address);
    	p.setHomeLocation(Fixture.placeThuisLichtenvoorde);
    	flush();

    	p = profileDao.loadGraph(p.getId(), Profile.DEFAULT_PROFILE_ENTITY_GRAPH).get();
    	assertEquals(address.getLocality(), p.getHomeAddress().getLocality());
    	log.debug("... and check searchPreferences");
    	sp = searchPreferencesDao.loadGraph(p.getId(), SearchPreferences.FULL_SEARCH_PREFS_ENTITY_GRAPH).orElse(null);
    	assertNotNull(sp);
    	assertFalse(sp.getAllowedTraverseModes().contains(TraverseMode.BUS));
    	assertFalse(sp.getAllowedTraverseModes().contains(TraverseMode.RAIL));
    	flush();
    	
    	// Role change, become a driver too
    	assertEquals(UserRole.PASSENGER, p.getUserRole());
    	p.setRidesharePreferences(ridesharePreferencesDao.loadGraph(p.getId(), RidesharePreferences.FULL_RIDESHARE_PREFS_ENTITY_GRAPH).orElse(null));
    	assertNull(p.getRidesharePreferences());
    	
    	p = profileDao.loadGraph(p.getId(), Profile.DEFAULT_PROFILE_ENTITY_GRAPH).get();
    	p.addRidesharePreferences();
    	ridesharePreferencesDao.save(p.getRidesharePreferences());
    	flush();
    	
    	p.setUserRole(UserRole.BOTH);
    	p.getRidesharePreferences().setMaxDistanceDetour(30000);
//    	p.getRidesharePreferences().getLuggageOptions().clear();
    	p.getRidesharePreferences().getLuggageOptions().add(LuggageOption.PET);
    	profileDao.merge(p);
    	ridesharePreferencesDao.merge(p.getRidesharePreferences());
    	flush();

    	p = profileDao.loadGraph(p.getId(), Profile.DEFAULT_PROFILE_ENTITY_GRAPH).get();
    	assertEquals(UserRole.BOTH, p.getUserRole());
    	assertNull(p.getRidesharePreferences());
    	p.setRidesharePreferences(ridesharePreferencesDao.loadGraph(p.getId(), RidesharePreferences.FULL_RIDESHARE_PREFS_ENTITY_GRAPH).orElse(null));
    	assertNotNull(p.getRidesharePreferences());
    	assertEquals(30000, p.getRidesharePreferences().getMaxDistanceDetour().intValue());
//    	log.debug("Check rideshare preferences - luggage");
    	assertTrue(p.getRidesharePreferences().getLuggageOptions().contains(LuggageOption.PET));
    	
    	ridesharePreferencesDao.remove(p.getRidesharePreferences());
    	p.removeRidesharePreferences();
    	p.setUserRole(UserRole.PASSENGER);
    	flush();
    	    	
    	p = profileDao.loadGraph(p.getId(), Profile.DEFAULT_PROFILE_ENTITY_GRAPH).get();
    	assertEquals(UserRole.PASSENGER, p.getUserRole());
    	p.setRidesharePreferences(ridesharePreferencesDao.loadGraph(p.getId(), RidesharePreferences.FULL_RIDESHARE_PREFS_ENTITY_GRAPH).orElse(null));
    	assertNull(p.getRidesharePreferences());

    	Long count = em.createQuery("select count(rp) from RidesharePreferences rp where id = :id", Long.class)
    			.setParameter("id", p.getId())
    			.getSingleResult();
    	assertEquals(0L, count.longValue());
    	log.debug("End of test: mergeChanges");
    }

    @Test
    public void removeProfile() throws Exception {
    	log.debug("Start of test: removeProfile");
    	var p = Fixture.createPassenger2();
    	var address = Fixture.createAddressLichtenvoorde();
    	p.setHomeAddress(address);
    	p.setHomeLocation(Fixture.placeThuisLichtenvoorde);
    	log.debug("Create passenger2");
    	profileDao.save(p);
    	searchPreferencesDao.save(p.getSearchPreferences());
    	log.debug("Add favorite for passenger2");
    	p.addPlace(Fixture.createPlace(Fixture.createAddressHengelo(), Fixture.placeThuisHengelo));
    	flush();

    	log.debug("Fetch profile full");
    	p = profileDao.loadGraph(p.getId(), Profile.DEFAULT_PROFILE_ENTITY_GRAPH).get();
    	p.setRidesharePreferences(ridesharePreferencesDao.find(p.getId()).orElse(null));
    	if (p.getRidesharePreferences() != null) {
    		ridesharePreferencesDao.remove(p.getRidesharePreferences());
    	}
    	p.setSearchPreferences(searchPreferencesDao.find(p.getId()).orElse(null));
    	if (p.getSearchPreferences() != null) {
    		searchPreferencesDao.remove(p.getSearchPreferences());
    	}
    	log.debug("Remove profile");
    	profileDao.remove(p);
    	flush();
    	Optional<Profile> opt_p = profileDao.find(p.getId());
    	assertFalse(opt_p.isPresent());
    	log.debug("End of test: removeProfile");
    }

    @Test
    public void shoutOutCircles() throws Exception {
    	var carla1 = profileDao.find(driver1.getId()).get();
    	var addrCarla1 = Fixture.createAddressLichtenvoorde();
    	carla1.setHomeAddress(addrCarla1);
    	carla1.setHomeLocation(Fixture.placeThuisLichtenvoorde);
    	var carla2 = Fixture.createDriver2();
    	var addrCarla2 = Fixture.createAddressHengelo();
    	carla2.setHomeAddress(addrCarla2);
    	carla2.setHomeLocation(Fixture.placeThuisHengelo);
    	profileDao.save(carla2);
    	ridesharePreferencesDao.save(carla2.getRidesharePreferences());
    	flush();
    	var simon1 = passenger1;
    	carla1 = profileDao.loadGraph(carla1.getId(), Profile.DEFAULT_PROFILE_ENTITY_GRAPH).get();
    	assertNotNull(carla1.getHomeAddress());
    	assertNotNull(carla1.getHomeLocation());
    	
    	carla2 = profileDao.loadGraph(carla2.getId(), Profile.DEFAULT_PROFILE_ENTITY_GRAPH).get();
    	assertNotNull(carla2.getHomeAddress());
    	assertNotNull(carla2.getHomeLocation());
    	// So now , we have two drivers: Lichtenvoorde (carla1) and Hengelo (carla2).
    	// We want a ride from Zieuwent to Doetinchem. Lichtenvoorde is nearby, Hengelo is not.
    	
    	List<Profile> drivers = profileDao.searchShoutOutProfiles(simon1, Fixture.placeZieuwent, Fixture.placeSlingeland, 30000, 10000);
    	assertNotNull(drivers);
    	assertEquals(1, drivers.size());
    	assertEquals(carla1, drivers.get(0));

    	// Other way around should also be possible
    	
    	drivers = profileDao.searchShoutOutProfiles(simon1, Fixture.placeSlingeland, Fixture.placeZieuwent, 30000, 10000);
    	assertNotNull(drivers);
    	assertEquals(1, drivers.size());
    	assertEquals(carla1, drivers.get(0));

    	// If carla asks then she does not find her own profile
    	drivers = profileDao.searchShoutOutProfiles(carla1, Fixture.placeSlingeland, Fixture.placeZieuwent, 30000, 10000);
    	assertNotNull(drivers);
    	assertEquals(0, drivers.size());

    	drivers = profileDao.searchShoutOutProfiles(simon1, Fixture.placeZieuwent, Fixture.placeSlingeland, 10000, 10000);
    	assertNotNull(drivers);
    	assertEquals(0, drivers.size());

    	drivers = profileDao.searchShoutOutProfiles(simon1, Fixture.placeSlingeland, Fixture.placeZieuwent, 10000, 10000);
    	assertNotNull(drivers);
    	assertEquals(0, drivers.size());
    }
}
