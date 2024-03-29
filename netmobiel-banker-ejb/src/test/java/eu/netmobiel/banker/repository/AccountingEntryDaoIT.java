package eu.netmobiel.banker.repository;


import static org.junit.Assert.*;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.TypedQuery;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.banker.exception.BalanceInsufficientException;
import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.AccountingEntry;
import eu.netmobiel.banker.model.AccountingTransaction;
import eu.netmobiel.banker.model.Balance;
import eu.netmobiel.banker.model.TransactionType;
import eu.netmobiel.banker.test.BankerIntegrationTestBase;
import eu.netmobiel.commons.model.PagedResult;

@RunWith(Arquillian.class)
public class AccountingEntryDaoIT extends BankerIntegrationTestBase {
	@Deployment
    public static Archive<?> createTestArchive() {
        WebArchive archive = createDeploymentBase()
        		.addClass(AccountingEntryDao.class)
        ;
// 		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private Logger log;
    
    @Inject
    private AccountingEntryDao accountingEntryDao;

	@Override
	protected void insertData() throws Exception {
		prepareBasicLedger();
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
    public void listAccountingEntries() throws BalanceInsufficientException {
    	// Take care to load the balances into the persistence context
    	balance1 = em.find(Balance.class, balance1.getId());
    	balance2 = em.find(Balance.class, balance2.getId());
    	balance3 = em.find(Balance.class, balance3.getId());
    	int oldAmount1 = balance1.getEndAmount();
    	int oldAmount2 = balance2.getEndAmount();
    	AccountingTransaction trans = ledger.createStartTransaction("description-1", "ref-1", Instant.parse("2020-04-07T17:00:00Z"), Instant.parse("2020-04-07T18:00:00Z"))
    			.credit(balance1, 10, TransactionType.PAYMENT, balance2.getAccount())
    			.debit(balance2, 10, TransactionType.PAYMENT, balance1.getAccount())
    			.build();
    	em.persist(trans);
    	checkBalance(balance1.getAccount(), oldAmount1 + 10);
    	checkBalance(balance2.getAccount(), oldAmount2 - 10);
    	oldAmount1 += 10;
    	oldAmount2 -= 10;

    	trans = ledger.createStartTransaction("description-2", "ref-2", Instant.parse("2020-04-08T17:00:00Z"), Instant.parse("2020-04-08T18:00:00Z"))
    			.credit(balance2, 20, TransactionType.PAYMENT, balance1.getAccount())
    			.debit(balance1, 20, TransactionType.PAYMENT, balance2.getAccount())
    			.build();
    	em.persist(trans);
    	checkBalance(balance1.getAccount(), oldAmount1 - 20);
    	checkBalance(balance2.getAccount(), oldAmount2 + 20);
    	oldAmount1 -= 20;
    	oldAmount2 += 20;

    	int oldAmount3 = balance3.getEndAmount();
    	trans = ledger.createStartTransaction("description-3", "ref-3", Instant.parse("2020-04-09T17:00:00Z"), Instant.parse("2020-04-09T18:00:00Z"))
    			.credit(balance3, 20, TransactionType.PAYMENT, balance1.getAccount())
    			.debit(balance1, 20, TransactionType.PAYMENT, balance3.getAccount())
    			.build();
    	em.persist(trans);
    	checkBalance(balance3.getAccount(), oldAmount3 + 20);
    	checkBalance(balance1.getAccount(), oldAmount1 - 20);
    	oldAmount3 += 20;
    	oldAmount2 -= 20;

    	String accref = null;
    	Instant since = null;
    	Instant until = null;
    	TransactionType purpose = null;
    	PagedResult<Long> actual = accountingEntryDao.listAccountingEntries(accref, since, until, purpose, 0, 0);
    	assertNotNull(actual);
    	assertEquals(0, actual.getCount());
    	assertEquals(0, actual.getData().size());
    	assertEquals(6, actual.getTotalCount().intValue());
    
    	actual = accountingEntryDao.listAccountingEntries(accref, since, until, purpose, 2, 0);
    	assertNotNull(actual);
    	assertEquals(2, actual.getCount());
    	assertEquals(2, actual.getData().size());
    	assertNull(actual.getTotalCount());
    	
    	List<AccountingEntry> entries = accountingEntryDao.loadGraphs(actual.getData(), null, AccountingEntry::getId);
    	// sorting by transaction time desc, entry type asc
    	assertEquals(2, actual.getData().size());
    	dump("listEntries 2", entries);
    	// Transaction time is same or most recent first
    	assertFalse(entries.get(0).getTransaction().getTransactionTime().isBefore(entries.get(1).getTransaction().getTransactionTime()));
    	assertTrue(entries.get(0).getTransaction().getTransactionTime().equals(entries.get(1).getTransaction().getTransactionTime()) || 
    			entries.get(0).getId().longValue() > entries.get(1).getId().longValue());

    	// TEST Account reference
    	accref = account1.getNcan();
    	actual = accountingEntryDao.listAccountingEntries(accref, since, until, purpose, 10, 0);
    	assertNotNull(actual);
    	assertEquals(3, actual.getData().size());
    	entries = accountingEntryDao.loadGraphs(actual.getData(), null, AccountingEntry::getId);
    	dump("listEntries - " + accref, entries);
    	for (AccountingEntry entry : entries) {
    		assertEquals(accref, entry.getAccount().getNcan());
		}

    	// TEST since
    	accref = null;
    	since = Instant.parse("2020-04-09T17:00:00Z");
    	actual = accountingEntryDao.listAccountingEntries(accref, since, until, purpose, 10, 0);
    	assertNotNull(actual);
    	assertEquals(2, actual.getData().size());
    	entries = accountingEntryDao.loadGraphs(actual.getData(), null, AccountingEntry::getId);
    	dump("listEntries - since " + DateTimeFormatter.ISO_INSTANT.format(since), entries);
    	for (AccountingEntry entry : entries) {
    		assertTrue(!entry.getTransaction().getAccountingTime().isBefore(since));
		}
    	// TEST until
    	since = null;
    	until = Instant.parse("2020-04-08T17:00:00Z");
    	actual = accountingEntryDao.listAccountingEntries(accref, since, until, purpose, 10, 0);
    	assertNotNull(actual);
    	assertEquals(2, actual.getData().size());
    	entries = accountingEntryDao.loadGraphs(actual.getData(), null, AccountingEntry::getId);
    	dump("listEntries - until " + DateTimeFormatter.ISO_INSTANT.format(until), entries);
    	for (AccountingEntry entry : entries) {
    		assertTrue(entry.getTransaction().getAccountingTime().isBefore(until));
		}

    	// TEST purpose
    	until = null;
    	purpose = TransactionType.PAYMENT;
    	actual = accountingEntryDao.listAccountingEntries(accref, since, until, purpose, 10, 0);
    	assertNotNull(actual);
    	assertEquals(6, actual.getData().size());
    	entries = accountingEntryDao.loadGraphs(actual.getData(), null, AccountingEntry::getId);
    	dump("listEntries - purpose " + purpose, entries);
    	for (AccountingEntry entry : entries) {
    		assertTrue(entry.getPurpose() == purpose);
		}
    	
    	purpose = TransactionType.DEPOSIT;
    	actual = accountingEntryDao.listAccountingEntries(accref, since, until, purpose, 10, 0);
    	assertNotNull(actual);
    	assertEquals(0, actual.getData().size());
    }

}
