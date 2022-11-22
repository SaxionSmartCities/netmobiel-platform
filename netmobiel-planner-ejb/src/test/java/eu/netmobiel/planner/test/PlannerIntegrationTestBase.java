package eu.netmobiel.planner.test;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.security.auth.login.LoginContext;
import javax.transaction.UserTransaction;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;

import eu.netmobiel.commons.util.ExceptionUtil;
import eu.netmobiel.planner.Resources;
import eu.netmobiel.planner.event.TravelOfferEvent;
import eu.netmobiel.planner.filter.ShoutOutFilter;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.repository.converter.PlanTypeConverter;
import eu.netmobiel.planner.util.PlannerUrnHelper;

public abstract class PlannerIntegrationTestBase {
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
            .addPackage(PlannerUrnHelper.class.getPackage())
            .addPackages(true, PlanTypeConverter.class.getPackage())
            .addPackages(true, TripPlan.class.getPackage())
            .addPackages(true, ShoutOutFilter.class.getPackage())
            .addPackages(true, TravelOfferEvent.class.getPackage())
            .addPackages(true, Fixture.class.getPackage())
            .addClass(Resources.class)
            .addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml")
            // Add log4j will overrule the server settings
//            .addAsResource("log4j.properties")
            .addAsResource("import.sql")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
           	.addAsResource("test-setup.properties")
            .addAsResource("keycloak.json", "keycloak.json")
	        .addAsWebInfResource("jboss-deployment-structure.xml");
    }
	
    @PersistenceContext(unitName = "pu-planner")
    protected EntityManager em;
    
    @Inject
    protected UserTransaction utx;
    
    @Inject
    private Logger log;
    
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
		em.createNativeQuery("delete from guide_step").executeUpdate();
		em.createQuery("delete from Leg").executeUpdate();
		em.createQuery("delete from Stop").executeUpdate();
		em.createQuery("delete from Trip").executeUpdate();
		em.createQuery("delete from Itinerary").executeUpdate();
		em.createQuery("delete from PlannerReport").executeUpdate();
		em.createQuery("delete from TripPlan").executeUpdate();
		em.createQuery("delete from PlannerUser").executeUpdate();
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
