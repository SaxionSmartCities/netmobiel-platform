package eu.netmobiel.profile.repository;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Optional;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.commons.exception.DuplicateEntryException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.NetMobielUserImpl;

@RunWith(Arquillian.class)
public class KeycloakDaoIT {
    @Deployment
    public static Archive<?> createTestArchive() {
    	File[] deps = Maven.configureResolver()
    			.loadPomFromFile("pom.xml")
    			.importCompileAndRuntimeDependencies() 
    			.resolve()
    			.withTransitivity()
    			.asFile();
    	WebArchive archive = ShrinkWrap.create(WebArchive.class, "test.war")
                .addAsLibraries(deps)
                .addClass(KeycloakDao.class);
//                .addAsResource("log4j.properties")
//    	        .addAsWebInfResource("jboss-deployment-structure.xml");
//		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private KeycloakDao keycloakDao;

	@Inject
    private Logger log;

	private static final String IDM_OTTO = "a8ee130f-23c6-4b26-bb79-b84a3799216d"; 
	private static final NetMobielUser DISPOSABLE_ACCOUNT = 
			new NetMobielUserImpl(null, "Pipo", "de Clown", "pipo.de.clown@nietbestaanddomein.nl");
	
    @Test
    public void getUser_Otto() throws Exception {
    	log.debug("Calling Keycloak for Otto");
    	Optional<NetMobielUser> otto = keycloakDao.getUser(IDM_OTTO);
    	log.debug("Keycloak returned from call");
    	assertTrue(otto.isPresent());
    	assertEquals("Otto", otto.get().getGivenName());
    	assertEquals("Normalverbraucher", otto.get().getFamilyName());
    	assertEquals("otto1971@hotmail.com", otto.get().getEmail());
    }

    @Test
    public void getUser_NoneFound() throws Exception {
    	Optional<NetMobielUser> user = keycloakDao.getUser("NoSuchUser");
    	assertFalse(user.isPresent());
    }

    @Test
    public void addUser() throws Exception {
    	String mid = keycloakDao.addUser(DISPOSABLE_ACCOUNT, true);
    	log.debug("Keycloak adduser returned: " + mid);
    	assertNotNull(mid);
    	try {
        	mid = keycloakDao.addUser(DISPOSABLE_ACCOUNT, false);
        	fail("Expected: " + DuplicateEntryException.class.getSimpleName());
    	} catch (DuplicateEntryException ex) {
       		log.debug("Anticipated exception: " + ex.toString());
    	} catch (Exception ex) {
   			fail("Unexpected exception: " + ex.toString());
    	}
    }

    @Test
    public void disableUser() throws Exception {
    	String mid = keycloakDao.addUser(DISPOSABLE_ACCOUNT, true);
    	log.debug("Keycloak adduser returned: " + mid);
    	assertNotNull(mid);
    	keycloakDao.disableUser(mid);
    }

    @Test
    public void removeUser_NotFound() throws Exception {
    	try {
    		keycloakDao.removeUser("NoSuchUser");
        	fail("Expected: " + NotFoundException.class.getSimpleName());
    	} catch (NotFoundException ex) {
       		log.debug("Anticipated exception: " + ex.toString());
    	} catch (Exception ex) {
   			fail("Unexpected exception: " + ex.toString());
    	}
    }

    @Test
    public void removeUser() throws Exception {
    	String mid = keycloakDao.addUser(DISPOSABLE_ACCOUNT, true);
   		keycloakDao.removeUser(mid);
   		Optional<NetMobielUser> user = keycloakDao.getUser(mid);
   		assertFalse(user.isPresent());
    }
}
