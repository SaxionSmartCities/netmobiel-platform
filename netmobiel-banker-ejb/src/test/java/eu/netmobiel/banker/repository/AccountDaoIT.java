package eu.netmobiel.banker.repository;


import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.banker.Resources;
import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.AccountType;
import eu.netmobiel.banker.model.User;
import eu.netmobiel.banker.repository.converter.InstantConverter;
import eu.netmobiel.banker.util.BankerUrnHelper;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.repository.AbstractDao;

@RunWith(Arquillian.class)
public class AccountDaoIT {
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
                .addPackages(true, BankerUrnHelper.class.getPackage())
                .addPackages(true, Account.class.getPackage())
                .addPackages(true, AbstractDao.class.getPackage())
                .addPackages(true, InstantConverter.class.getPackage())
            .addClass(UserDao.class)
            .addClass(AccountDao.class)
            .addClass(Resources.class)
            .addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
//		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private AccountDao accountDao;
    @Inject
    private UserDao userDao;

    @PersistenceContext(unitName = "pu-banker")
    private EntityManager em;
    
    @Inject
    private UserTransaction utx;
    
    @Inject
    private Logger log;
    
    @Before
    public void preparePersistenceTest() throws Exception {
        clearData();
        insertData();
        startTransaction();
    }
    
    private void clearData() throws Exception {
        utx.begin();
        em.joinTransaction();
        log.debug("Dumping old records...");
        em.createQuery("delete from Account").executeUpdate();
        em.createQuery("delete from User").executeUpdate();
        utx.commit();
    }

    private void insertData() throws Exception {
        utx.begin();
        em.joinTransaction();
        log.debug("Inserting records...");
    	List<User> users = new ArrayList<>();
        users.add(new User("U1", "A", "Family U1"));
        users.add(new User("U2", "B", "Family U2"));
        users.add(new User("U3", "C", "Family U3"));
        for (User user : users) {
			em.persist(user);
		}
        utx.commit();
        // clear the persistence context (first-level cache)
        em.clear();
    }

    private void startTransaction() throws Exception {
        utx.begin();
        em.joinTransaction();
    }

    @After
    public void commitTransaction() throws Exception {
        utx.commit();
    }
    
    private Account createAccount(User holder, String reference, AccountType type) {
    	Account acc = new Account();
    	acc.setAccountType(type);
    	acc.setHolder(holder);
    	acc.setReference(reference);
    	return acc;
    }
    
    private void dump(String subject, Collection<Account> accounts) {
    	accounts.forEach(m -> log.info(subject + ": " + m.toString()));
    }
    
    @Test
    public void saveAccount() {
    	User holder = userDao.findByManagedIdentity("U1").get();
		Account account= createAccount(holder, "account-1", AccountType.LIABILITY);
    	accountDao.save(account);
    	List<Account> actual = accountDao.findAll();
    	assertNotNull(actual);
    	assertEquals(1, actual.size());
    	dump("saveAccount", actual);
    }

    @Test
    public void findByReference() {
    	User holder = userDao.findByManagedIdentity("U1").get();
    	final String accref = "account-1"; 
		Account account= createAccount(holder, accref, AccountType.LIABILITY);
    	accountDao.save(account);
    	Account actual = accountDao.findByReference(accref);
    	assertNotNull(actual);
    	assertEquals(accref, actual.getReference());
    	dump("saveAccount", Collections.singletonList(actual));
    }

    @Test
    public void findByReference_NotFound() {
    	final String accref = "account-X";
    	try {
    		@SuppressWarnings("unused")
			Account actual = accountDao.findByReference(accref);
    		fail("Expected NoResultFoundException");
    	} catch (NoResultException ex) {
    		log.info("findByReference_NotFound: " + ex.toString());
    	}
    	
    }

    @Test
    public void listAccounts() {
    	User holder1 = userDao.findByManagedIdentity("U1").get();
    	User holder2 = userDao.findByManagedIdentity("U2").get();
    	final String accref1 = "account-1"; 
    	final String accref2 = "account-2"; 
    	accountDao.save(createAccount(holder2, accref2, AccountType.LIABILITY));
    	accountDao.save(createAccount(holder1, accref1, AccountType.LIABILITY));
    	PagedResult<Long> actual = accountDao.listAccounts(null, 0, 0);
    	assertNotNull(actual);
    	assertEquals(0, actual.getCount());
    	assertEquals(0, actual.getData().size());
    	assertEquals(2, actual.getTotalCount().intValue());

    
    	actual = accountDao.listAccounts(null, 1, 0);
    	assertNotNull(actual);
    	assertEquals(1, actual.getCount());
    	assertEquals(1, actual.getData().size());
    	assertNull(actual.getTotalCount());
    	List<Account> accounts = accountDao.fetch(actual.getData(), null, Account::getId);
    	// sorting by ref asc
    	assertEquals(accref1, accounts.get(0).getReference());

    	actual = accountDao.listAccounts(null, 1, 1);
    	assertNotNull(actual);
    	assertEquals(1, actual.getCount());
    	assertEquals(1, actual.getData().size());
    	assertNull(actual.getTotalCount());
    	accounts = accountDao.fetch(actual.getData(), null, Account::getId);
    	// sorting by ref asc
    	assertEquals(accref2, accounts.get(0).getReference());

    	actual = accountDao.listAccounts(holder1.getManagedIdentity(), 0, 0);
    	assertNotNull(actual);
    	assertEquals(0, actual.getCount());
    	assertEquals(0, actual.getData().size());
    	assertEquals(1, actual.getTotalCount().intValue());

    	actual = accountDao.listAccounts(holder1.getManagedIdentity(), 10, 0);
    	assertNotNull(actual);
    	assertEquals(1, actual.getCount());
    	accounts = accountDao.fetch(actual.getData(), null, Account::getId);
    	// sorting by ref asc
    	assertEquals(accref1, accounts.get(0).getReference());

    }

    @Test
    public void listAccounts_nonExistingUser() {
    	PagedResult<Long> actual = accountDao.listAccounts("some identity XXX", 0, 0);
    	assertNotNull(actual);
    	assertEquals(0, actual.getCount());
    	assertEquals(0, actual.getData().size());
    	assertEquals(0, actual.getTotalCount().intValue());

    
    	actual = accountDao.listAccounts("some identity XXX", 10, 0);
    	assertNotNull(actual);
    	assertEquals(0, actual.getCount());
    	assertEquals(0, actual.getData().size());
    	assertNull(actual.getTotalCount());
    }
}
