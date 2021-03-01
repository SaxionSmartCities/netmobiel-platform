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
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.commons.exception.DuplicateEntryException;
import eu.netmobiel.commons.exception.NotFoundException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.commons.model.NetMobielUserImpl;
import eu.netmobiel.commons.model.PagedResult;

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

	@After
	public void removeDisposableUser() {
		try {
	    	Optional<NetMobielUser> user = keycloakDao.findUserByEmail(DISPOSABLE_ACCOUNT.getEmail());
	    	if (user.isPresent()) {
	       		keycloakDao.removeUser(user.get().getManagedIdentity());
	    	}
		} catch (Exception ex) {
       		log.error("Error cleaning up: " + ex.toString());
	    }
	}
	
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
    	String mid = keycloakDao.addUser(DISPOSABLE_ACCOUNT);
    	assertNotNull(mid);
    	try {
        	mid = keycloakDao.addUser(DISPOSABLE_ACCOUNT);
        	fail("Expected: " + DuplicateEntryException.class.getSimpleName());
    	} catch (DuplicateEntryException ex) {
       		log.debug("Anticipated exception: " + ex.toString());
    	} catch (Exception ex) {
   			fail("Unexpected exception: " + ex.toString());
    	}
    }

    @Test
    public void disableUser() throws Exception {
    	String mid = keycloakDao.addUser(DISPOSABLE_ACCOUNT);
    	assertNotNull(mid);
    	keycloakDao.disableUser(mid);
    }

    @Test
    public void updateUser() throws Exception {
    	try {
	    	String mid = keycloakDao.addUser(DISPOSABLE_ACCOUNT);
	    	assertNotNull(mid);
	    	Optional<NetMobielUser> ouser = keycloakDao.getUser(mid);
	    	assertTrue(ouser.isPresent());
	    	NetMobielUserImpl user = (NetMobielUserImpl) ouser.get();
	    	user.setGivenName("Klukkluk");
	    	keycloakDao.updateUser(user);
	    	ouser = keycloakDao.getUser(mid);
	    	assertEquals("Klukkluk", ouser.get().getGivenName());
    	} catch (Exception ex) {
    		log.error("Error updating Keycloak user", ex);
    		fail("Exception: " + ex.toString());
    		
    	}
    	
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
    	String mid = keycloakDao.addUser(DISPOSABLE_ACCOUNT);
   		keycloakDao.removeUser(mid);
   		Optional<NetMobielUser> user = keycloakDao.getUser(mid);
   		assertFalse(user.isPresent());
    }

    @Test
    public void countUsers() throws Exception {
    	PagedResult<NetMobielUser> pr = keycloakDao.listUsers(Cursor.COUNTING_CURSOR);
   		assertNotNull(pr.getTotalCount());
   		assertTrue(pr.getTotalCount() > 0);
   		assertTrue(pr.getData().isEmpty());
   		log.debug("countUsers in Keycloak: " + pr.getTotalCount());
    }
}
