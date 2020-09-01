package eu.netmobiel.banker.repository;


import static org.junit.Assert.*;

import java.io.File;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
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
import eu.netmobiel.banker.model.AccountingEntry;
import eu.netmobiel.banker.model.AccountingEntryType;
import eu.netmobiel.banker.model.AccountingTransaction;
import eu.netmobiel.banker.model.Balance;
import eu.netmobiel.banker.model.Ledger;
import eu.netmobiel.banker.model.TransactionType;
import eu.netmobiel.banker.model.User;
import eu.netmobiel.banker.repository.converter.InstantConverter;
import eu.netmobiel.banker.test.Fixture;
import eu.netmobiel.banker.util.BankerUrnHelper;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.repository.AbstractDao;

@RunWith(Arquillian.class)
public class AccountingEntryDaoIT {
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
                .addPackages(true, Fixture.class.getPackage())
            .addClass(AccountingEntryDao.class)
            .addClass(Resources.class)
            .addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
//		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private AccountingEntryDao accountingEntryDao;

    @PersistenceContext(unitName = "pu-banker")
    private EntityManager em;
    
    @Inject
    private UserTransaction utx;
    
    @Inject
    private Logger log;
    
    private Ledger ledger;
    private Account account1;
    private Account account2;
    private Account account3;
    private Balance balance1;
    private Balance balance2;
    private Balance balance3;
    
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
        em.createQuery("delete from AccountingEntry").executeUpdate();
        em.createQuery("delete from AccountingTransaction").executeUpdate();
        em.createQuery("delete from Balance").executeUpdate();
        em.createQuery("delete from Account").executeUpdate();
        em.createQuery("delete from Ledger").executeUpdate();
        em.createQuery("delete from User").executeUpdate();
        utx.commit();
    }

    private void insertData() throws Exception {
        utx.begin();
        em.joinTransaction();
        log.debug("Inserting records...");
        ledger = createLedger("ledger-1", "2020-01-01T01:00:00Z", null);
        User user1 = new User("U1", "A", "Family U1", null);
        User user2 = new User("U2", "B", "Family U2", null);
        User user3 = new User("U3", "C", "Family U3", null); 
        em.persist(ledger);
    	em.persist(user1);
    	em.persist(user2);
    	em.persist(user3);
    	account1 = Fixture.createAccount("account-1", "account-1", AccountType.LIABILITY);
    	account2 = Fixture.createAccount("account-2", "account-2", AccountType.LIABILITY); 
    	account3 = Fixture.createAccount("account-3", "account-3", AccountType.LIABILITY); 
        em.persist(account1);
        em.persist(account2);
        em.persist(account3);
        user1.setPersonalAccount(account1);
        user2.setPersonalAccount(account2);
        user3.setPersonalAccount(account3);
        balance1 = new Balance(ledger, account1, 100); 
        balance2 = new Balance(ledger, account2, 200); 
        balance3 = new Balance(ledger, account3, 0); 
        em.persist(balance1);
        em.persist(balance2);
        em.persist(balance3);
    	
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
    
    private Ledger createLedger(String name, String startTimeIso, String endTimeIso) {
    	Instant startPeriod = Instant.parse(startTimeIso);
    	Instant endPeriod = endTimeIso != null ? Instant.parse(endTimeIso) : null;
    	Ledger ledger = new Ledger();
    	ledger.setName(name);
    	ledger.setStartPeriod(startPeriod);
    	ledger.setEndPeriod(endPeriod);
    	return ledger;
    }
    
    private void dump(String subject, Collection<AccountingEntry> entries) {
    	entries.forEach(obj -> log.info(subject + ": " + obj.toString()));
    }
    
    private void checkBalance(Account acc, int amount) {
		TypedQuery<Balance> tq = em.createQuery("from Balance bal where bal.ledger = :ledger and bal.account = :account", Balance.class);
		tq.setParameter("ledger", ledger);
		tq.setParameter("account", acc);
		Balance b = tq.getSingleResult();
		assertEquals(amount, b.getEndAmount());

    }
    @Test
    public void listAccountingEntries() {
    	// Take care to load the balances into the persistence context
    	balance1 = em.find(Balance.class, balance1.getId());
    	balance2 = em.find(Balance.class, balance2.getId());
    	balance3 = em.find(Balance.class, balance3.getId());
    	int oldAmount1 = balance1.getEndAmount();
    	int oldAmount2 = balance2.getEndAmount();
    	AccountingTransaction trans = ledger.createTransaction(TransactionType.PAYMENT, "description-1", "ref-1", Instant.parse("2020-04-07T17:00:00Z"), Instant.parse("2020-04-07T18:00:00Z"))
    			.credit(balance1, 10, balance2.getAccount().getName())
    			.debit(balance2, 10, balance1.getAccount().getName())
    			.build();
    	em.persist(trans);
    	checkBalance(balance1.getAccount(), oldAmount1 + 10);
    	checkBalance(balance2.getAccount(), oldAmount2 - 10);
    	oldAmount1 += 10;
    	oldAmount2 -= 10;

    	trans = ledger.createTransaction(TransactionType.PAYMENT, "description-2", "ref-2", Instant.parse("2020-04-08T17:00:00Z"), Instant.parse("2020-04-08T18:00:00Z"))
    			.credit(balance2, 20, balance1.getAccount().getName())
    			.debit(balance1, 20, balance2.getAccount().getName())
    			.build();
    	em.persist(trans);
    	checkBalance(balance1.getAccount(), oldAmount1 - 20);
    	checkBalance(balance2.getAccount(), oldAmount2 + 20);
    	oldAmount1 -= 20;
    	oldAmount2 += 20;

    	int oldAmount3 = balance3.getEndAmount();
    	trans = ledger.createTransaction(TransactionType.PAYMENT, "description-3", "ref-3", Instant.parse("2020-04-09T17:00:00Z"), Instant.parse("2020-04-09T18:00:00Z"))
    			.credit(balance3, 20, balance1.getAccount().getName())
    			.debit(balance1, 20, balance3.getAccount().getName())
    			.build();
    	em.persist(trans);
    	checkBalance(balance3.getAccount(), oldAmount3 + 20);
    	checkBalance(balance1.getAccount(), oldAmount1 - 20);
    	oldAmount3 += 20;
    	oldAmount2 -= 20;

    	String accref = null;
    	Instant since = null;
    	Instant until = null;
    	PagedResult<Long> actual = accountingEntryDao.listAccountingEntries(accref, since, until, 0, 0);
    	assertNotNull(actual);
    	assertEquals(0, actual.getCount());
    	assertEquals(0, actual.getData().size());
    	assertEquals(6, actual.getTotalCount().intValue());
    
    	actual = accountingEntryDao.listAccountingEntries(accref, since, until, 2, 0);
    	assertNotNull(actual);
    	assertEquals(2, actual.getCount());
    	assertEquals(2, actual.getData().size());
    	assertNull(actual.getTotalCount());
    	List<AccountingEntry> entries = accountingEntryDao.fetch(actual.getData(), null, AccountingEntry::getId);
    	// sorting by transaction time desc, entry type asc
    	assertEquals(AccountingEntryType.CREDIT, entries.get(0).getEntryType());
    	assertEquals(AccountingEntryType.DEBIT, entries.get(1).getEntryType());
    	dump("listEntries 2", entries);

    	// TEST Account reference
    	accref = account1.getNcan();
    	actual = accountingEntryDao.listAccountingEntries(accref, since, until, 10, 0);
    	assertNotNull(actual);
    	assertEquals(3, actual.getData().size());
    	entries = accountingEntryDao.fetch(actual.getData(), null, AccountingEntry::getId);
    	dump("listEntries - " + accref, entries);
    	for (AccountingEntry entry : entries) {
    		assertEquals(accref, entry.getAccount().getNcan());
		}

    	// TEST since
    	accref = null;
    	since = Instant.parse("2020-04-09T17:00:00Z");
    	actual = accountingEntryDao.listAccountingEntries(accref, since, until, 10, 0);
    	assertNotNull(actual);
    	assertEquals(2, actual.getData().size());
    	entries = accountingEntryDao.fetch(actual.getData(), null, AccountingEntry::getId);
    	dump("listEntries - since " + DateTimeFormatter.ISO_INSTANT.format(since), entries);
    	for (AccountingEntry entry : entries) {
    		assertTrue(!entry.getTransaction().getAccountingTime().isBefore(since));
		}
    	// TEST until
    	since = null;
    	until = Instant.parse("2020-04-08T17:00:00Z");
    	actual = accountingEntryDao.listAccountingEntries(accref, since, until, 10, 0);
    	assertNotNull(actual);
    	assertEquals(2, actual.getData().size());
    	entries = accountingEntryDao.fetch(actual.getData(), null, AccountingEntry::getId);
    	dump("listEntries - until " + DateTimeFormatter.ISO_INSTANT.format(until), entries);
    	for (AccountingEntry entry : entries) {
    		assertTrue(entry.getTransaction().getAccountingTime().isBefore(until));
		}
    }

}
