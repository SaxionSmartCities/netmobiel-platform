package eu.netmobiel.profile.test;

import java.io.File;

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
import eu.netmobiel.profile.Resources;
import eu.netmobiel.profile.annotation.ProfileDatabase;
import eu.netmobiel.profile.filter.ProfileFilter;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.model.Profile_;
import eu.netmobiel.profile.repository.converter.LuggageOptionConverter;
import eu.netmobiel.profile.util.ProfileUrnHelper;

public abstract class ProfileIntegrationTestBase {
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
                .addPackages(true, ProfileDatabase.class.getPackage())
                .addPackages(true, ProfileUrnHelper.class.getPackage())
                .addPackages(true, ProfileFilter.class.getPackage())
                .addPackages(true, ProfileFilter.class.getPackage())
                .addPackages(true, Profile.class.getPackage())
                .addPackages(true, Profile_.class.getPackage())
//                .addPackages(true, AbstractDao.class.getPackage())
                .addPackages(true, LuggageOptionConverter.class.getPackage())
                .addPackages(true, Fixture.class.getPackage())
                .addClass(Resources.class)
//                .addAsResource("log4j.properties")
    	        .addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml");
    }
	
    @PersistenceContext(unitName = "pu-profilesvc")
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
//		Properties props = new Properties();
//		try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
//				.getResourceAsStream("test-setup.properties")) {
//			props.load(inputStream);
//		}
//		LoginContextFactory factory = new LoginContextFactory("keycloak.json");
//		loginContextDriver = factory.createDirectGrantLoginContext(
//				props.getProperty(DRIVER_USERNAME),
//				props.getProperty(DRIVER_PASSWORD), null);
//		loginContextDriver.login();
	}

	protected void preparePassengerLogin() throws Exception {
//		Properties props = new Properties();
//		try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
//				.getResourceAsStream("test-setup.properties")) {
//			props.load(inputStream);
//		}
//		LoginContextFactory factory = new LoginContextFactory("keycloak.json");
//		loginContextPassenger = factory.createDirectGrantLoginContext(
//				props.getProperty(PASSENGER_USERNAME),
//				props.getProperty(PASSENGER_PASSWORD), null);
//		loginContextPassenger.login();
	}

	protected void clearData() throws Exception {
		utx.begin();
		em.joinTransaction();
		log.debug("Dumping old records...");
		em.createQuery("delete from SurveyInteraction").executeUpdate();
		em.createQuery("delete from Survey").executeUpdate();
		em.createQuery("delete from Place").executeUpdate();
		em.createQuery("delete from Compliment").executeUpdate();
		em.createQuery("delete from Review").executeUpdate();
		em.createQuery("delete from Delegation").executeUpdate();
		em.createQuery("delete from SearchPreferences").executeUpdate();
		em.createQuery("delete from RidesharePreferences").executeUpdate();
		em.createQuery("delete from Profile").executeUpdate();
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
