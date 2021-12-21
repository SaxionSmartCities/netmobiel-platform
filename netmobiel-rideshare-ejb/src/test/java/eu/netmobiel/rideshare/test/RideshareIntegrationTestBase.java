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

import eu.netmobiel.commons.util.ExceptionUtil;
import eu.netmobiel.rideshare.Resources;
import eu.netmobiel.rideshare.annotation.RideshareDatabase;
import eu.netmobiel.rideshare.filter.RideFilter;
import eu.netmobiel.rideshare.model.RideTemplate;
import eu.netmobiel.rideshare.model.RideTemplate_;
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
                .addPackages(true, BookingStateConverter.class.getPackage())
                .addPackages(true, RideFilter.class.getPackage())
                .addPackages(true, Fixture.class.getPackage())
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
    protected LoginContext loginContextDriver;
    protected LoginContext loginContextPassenger;

    private boolean expectFailure;

    public boolean isSecurityRequired() {
    	return false;
    }
	
    public void prepareSecurity() throws Exception {
		prepareDriverLogin();
		preparePassengerLogin();
	}

	public void finishSecurity() throws Exception {
		loginContextDriver.logout();
		loginContextPassenger.logout();
	}

	@Before
	public void prepareTest() throws Exception {
		expectFailure = false;
		if (isSecurityRequired()) {
			prepareSecurity();
		}
		clearData();
		prepareData();
		startTransaction();
	}

	@After
	public void finishTest() throws Exception {
		try {
			if (!expectFailure) {
				commitTransaction();
			}
			if (isSecurityRequired()) {
				finishSecurity();
			}
		} catch (Exception ex) {
			log.error(String.join("\n", ExceptionUtil.unwindException(ex)));
			throw ex;	
		}
	}

	public void expectFailure() {
		this.expectFailure = true;
	}

	protected void commitTransaction() throws Exception {
		if (em.isJoinedToTransaction()) {
			utx.commit();
		}
	}

	protected void prepareDriverLogin() throws Exception {
		Properties props = new Properties();
		try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("test-setup.properties")) {
			props.load(inputStream);
		}
		LoginContextFactory factory = new LoginContextFactory("keycloak.json");
		loginContextDriver = factory.createDirectGrantLoginContext(
				props.getProperty(DRIVER_USERNAME),
				props.getProperty(DRIVER_PASSWORD), null);
		loginContextDriver.login();
	}

	protected void preparePassengerLogin() throws Exception {
		Properties props = new Properties();
		try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("test-setup.properties")) {
			props.load(inputStream);
		}
		LoginContextFactory factory = new LoginContextFactory("keycloak.json");
		loginContextPassenger = factory.createDirectGrantLoginContext(
				props.getProperty(PASSENGER_USERNAME),
				props.getProperty(PASSENGER_PASSWORD), null);
		loginContextPassenger.login();
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
		em.createQuery("delete from RideshareUser").executeUpdate();
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

	protected void flush() throws Exception {
		utx.commit();
		// clear the persistence context (first-level cache)
		em.clear();
		utx.begin();
		em.joinTransaction();
	}
}
