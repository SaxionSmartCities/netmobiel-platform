package eu.netmobiel.banker.test;

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

import eu.netmobiel.banker.Resources;
import eu.netmobiel.banker.annotation.BankerDatabase;
import eu.netmobiel.banker.exception.BalanceInsufficientException;
import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.repository.converter.InstantConverter;
import eu.netmobiel.banker.util.BankerUrnHelper;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.commons.util.ExceptionUtil;

public abstract class BankerIntegrationTestBase {
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
                .addPackages(true, BankerDatabase.class.getPackage())
                .addPackages(true, BankerUrnHelper.class.getPackage())
                .addPackages(true, BalanceInsufficientException.class.getPackage())
                .addPackages(true, Account.class.getPackage())
                .addPackages(true, AbstractDao.class.getPackage())
                .addPackages(true, InstantConverter.class.getPackage())
                .addPackages(true, Fixture.class.getPackage())
                .addClass(Resources.class)
            	.addAsResource("test-setup.properties")
                .addAsResource("keycloak.json", "keycloak.json")
//                .addAsResource("log4j.properties")
    	        .addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml")
    	        .addAsWebInfResource("jboss-deployment-structure.xml");
    }
	
    @PersistenceContext(unitName = "pu-banker")
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
		utx.commit();
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
		em.createQuery("delete from Donation").executeUpdate();
		em.createQuery("delete from CharityUserRole").executeUpdate();
		em.createQuery("delete from Charity").executeUpdate();
		em.createQuery("delete from AccountingEntry").executeUpdate();
		em.createQuery("delete from AccountingTransaction").executeUpdate();
		em.createQuery("delete from Balance").executeUpdate();
		em.createQuery("delete from PaymentBatch").executeUpdate();
		em.createQuery("delete from WithdrawalRequest").executeUpdate();
		em.createQuery("delete from Account").executeUpdate();
		em.createQuery("delete from Ledger").executeUpdate();
		em.createQuery("delete from DepositRequest").executeUpdate();
		em.createQuery("delete from BankerUser").executeUpdate();
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
