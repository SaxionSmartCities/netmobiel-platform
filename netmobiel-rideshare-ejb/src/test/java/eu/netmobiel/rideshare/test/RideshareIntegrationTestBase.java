package eu.netmobiel.rideshare.test;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.security.auth.login.LoginContext;
import javax.transaction.UserTransaction;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.rideshare.Resources;
import eu.netmobiel.rideshare.annotation.RideshareDatabase;
import eu.netmobiel.rideshare.model.RideTemplate;
import eu.netmobiel.rideshare.model.RideTemplate_;
import eu.netmobiel.rideshare.repository.RideDao;
import eu.netmobiel.rideshare.repository.converter.BookingStateConverter;
import eu.netmobiel.rideshare.util.RideshareUrnHelper;

public abstract class RideshareIntegrationTestBase {
	protected static final String SECURITY_DOMAIN  = "other";
	protected static final String DRIVER_USERNAME = "driverUsername";
	protected static final String DRIVER_PASSWORD = "driverPassword";
	protected static final String PASSENGER_USERNAME = "passengerUsername";
	protected static final String PASSENGER_PASSWORD = "passengerPassword";
	
    public static WebArchive createDeploymentBase() { 
    	File[] deps = Maven.configureResolver()
    			.loadPomFromFile("pom.xml")
    			.importCompileAndRuntimeDependencies() 
    			.resolve()
    			.withTransitivity()
    			.asFile();
        return ShrinkWrap.create(WebArchive.class, "test.war")
                .addAsLibraries(deps)
                .addPackages(true, RideshareDatabase.class.getPackage())
                .addPackages(true, RideshareUrnHelper.class.getPackage())
                .addPackages(true, RideTemplate.class.getPackage())
                .addPackages(true, RideTemplate_.class.getPackage())
                .addPackages(true, AbstractDao.class.getPackage())
                .addPackages(true, BookingStateConverter.class.getPackage())
                .addPackages(true, Fixture.class.getPackage())
                .addPackages(true, RideDao.class.getPackage())
                .addClass(Resources.class)
            	.addAsResource("test-setup.properties")
                .addAsResource("keycloak.json", "keycloak.json")
//                .addAsResource("log4j.properties")
    	        .addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml")
    	        .addAsWebInfResource("jboss-deployment-structure.xml");
    }
	
    @PersistenceContext(unitName = "pu-rideshare")
    protected EntityManager em;
    
    @Inject
    protected UserTransaction utx;
    
    @Inject
    protected Logger log;
    
	//  private AccessToken driverAccessToken;
    protected LoginContext loginContext;
  
	@Before
	public void prepareTest() throws Exception {
		prepareLogin();
		clearData();
		prepareData();
		startTransaction();
	}

	@After
	public void finishTest() throws Exception {
		commitTransaction();
	}

	protected void commitTransaction() throws Exception {
		utx.commit();
		loginContext.logout();
	}

	protected void prepareLogin() throws Exception {
		Properties props = new Properties();
		try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("test-setup.properties")) {
			props.load(inputStream);
		}
		LoginContextFactory factory = new LoginContextFactory("keycloak.json");
		loginContext = factory.createDirectGrantLoginContext(
				props.getProperty(DRIVER_USERNAME),
				props.getProperty(DRIVER_PASSWORD), null);
		loginContext.login();
	}

	protected void clearData() throws Exception {
		utx.begin();
		em.joinTransaction();
		log.debug("Dumping old records...");
		em.createQuery("delete from Booking").executeUpdate();
		em.createQuery("delete from Leg").executeUpdate();
		em.createQuery("delete from Stop").executeUpdate();
		em.createQuery("delete from Ride").executeUpdate();
		em.createQuery("delete from RideTemplate").executeUpdate();
		em.createQuery("delete from Car").executeUpdate();
		em.createQuery("delete from User").executeUpdate();
		utx.commit();
	}

	protected void prepareData() throws Exception {
		utx.begin();
		em.joinTransaction();
		log.debug("Inserting records...");

		insertData();

		utx.commit();
		// clear the persistence context (first-level cache)
		em.clear();
	}

	protected abstract void insertData() throws Exception;

	protected void startTransaction() throws Exception {
		utx.begin();
		em.joinTransaction();
	}

}
