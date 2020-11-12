package eu.netmobiel.banker.test;

import java.io.File;
import java.io.InputStream;
import java.time.Instant;
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
import eu.netmobiel.banker.model.AccountType;
import eu.netmobiel.banker.model.Balance;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.Ledger;
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
    private Logger log;
    
	//  private AccessToken driverAccessToken;
    protected LoginContext loginContextDriver;
    protected LoginContext loginContextPassenger;
  
    protected Ledger ledger;
    protected Account bankingReserve;
    protected Account account1;
    protected Account account2;
    protected Account account3;
    protected Account account4;
    protected Balance balance1;
    protected Balance balance2;
    protected Balance balance3;
    protected Balance balance4;

    protected BankerUser user1;
    protected BankerUser user2;
    protected BankerUser user3;
    protected BankerUser user4;

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
		em.createQuery("delete from DepositRequest").executeUpdate();
		em.createQuery("delete from WithdrawalRequest").executeUpdate();
		em.createQuery("delete from PaymentBatch").executeUpdate();
		em.createQuery("delete from Donation").executeUpdate();
		em.createQuery("delete from CharityUserRole").executeUpdate();
		em.createQuery("delete from Charity").executeUpdate();
		em.createQuery("delete from AccountingEntry").executeUpdate();
		em.createQuery("delete from AccountingTransaction").executeUpdate();
		em.createQuery("delete from Balance").executeUpdate();
		em.createQuery("delete from Account").executeUpdate();
		em.createQuery("delete from Ledger").executeUpdate();
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

	protected void prepareBasicLedger() {
        ledger = Fixture.createLedger("ledger-1", "2020-01-01T01:00:00Z", null);
        em.persist(ledger);
    	bankingReserve = Account.newInstant("banking-reserve", "De Kluis", AccountType.ASSET, Instant.parse("2020-07-01T00:00:00Z"));
        em.persist(bankingReserve);
    	account1 = Fixture.createLiabilityAccount("PLA-1", "account 1", Instant.parse("2020-07-01T00:00:00Z"));
    	account2 = Fixture.createLiabilityAccount("PLA-2", "account 2", Instant.parse("2020-09-01T00:00:00Z")); 
    	account3 = Fixture.createLiabilityAccount("PLA-3", "account 3", Instant.parse("2020-09-15T00:00:00Z")); 
    	account4 = Fixture.createLiabilityAccount("PLA-4", "account 4 closed", Instant.parse("2020-07-01T00:00:00Z")); 
    	account4.setClosedTime(Instant.parse("2020-07-31T00:00:00Z"));
        em.persist(account1);
        em.persist(account2);
        em.persist(account3);
        em.persist(account4);
        balance1 = new Balance(ledger, account1, 100); 
        balance2 = new Balance(ledger, account2, 200); 
        balance3 = new Balance(ledger, account3, 0); 
        balance4 = new Balance(ledger, account4, 300); 
        em.persist(balance1);
        em.persist(balance2);
        em.persist(balance3);
        em.persist(balance4);
	}
	
	protected void createAndAssignUsers() {
        user1 = Fixture.createUser("U1", "A", "Family U1", null);
        user2 = Fixture.createUser("U2", "B", "Family U2", null);
        user3 = Fixture.createUser("U3", "C", "Family U3", null); 
        user4 = Fixture.createUser("U4", "D", "Family U4", null); 
    	em.persist(user1);
    	em.persist(user2);
    	em.persist(user3);
    	em.persist(user4);
        user1.setPersonalAccount(account1);
        user2.setPersonalAccount(account2);
        user3.setPersonalAccount(account3);
        user4.setPersonalAccount(account4);
	}
}