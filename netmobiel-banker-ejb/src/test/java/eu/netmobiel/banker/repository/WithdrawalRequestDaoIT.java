package eu.netmobiel.banker.repository;


import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.PersistenceUnitUtil;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.banker.model.AccountingTransaction;
import eu.netmobiel.banker.model.PaymentBatch;
import eu.netmobiel.banker.model.PaymentBatch_;
import eu.netmobiel.banker.model.PaymentStatus;
import eu.netmobiel.banker.model.TransactionType;
import eu.netmobiel.banker.model.WithdrawalRequest;
import eu.netmobiel.banker.model.WithdrawalRequest_;
import eu.netmobiel.banker.test.BankerIntegrationTestBase;
import eu.netmobiel.banker.test.Fixture;
import eu.netmobiel.commons.model.PagedResult;

@RunWith(Arquillian.class)
public class WithdrawalRequestDaoIT extends BankerIntegrationTestBase {
    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive archive = createDeploymentBase()
                .addClass(WithdrawalRequestDao.class)
                ;
//   		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private Logger log;
    
    @Inject
    private WithdrawalRequestDao dao;

    private AccountingTransaction dummyTransaction1;
    private AccountingTransaction dummyTransaction2;
    private AccountingTransaction dummyTransaction3;
    
    private void dump(String subject, Collection<WithdrawalRequest> wrs) {
    	wrs.forEach(m -> log.info(subject + ": " + wrs.toString()));
    }
    
    @Override
    protected void insertData() throws Exception {
    	prepareBasicLedger();
    	createAndAssignUsers();
    	dummyTransaction1 = ledger.createTransaction(TransactionType.PAYMENT, "description-1", "ref-1", Instant.parse("2020-04-07T17:00:00Z"), Instant.parse("2020-04-07T18:00:00Z"))
    			.credit(balance1, 10, balance2.getAccount())
    			.debit(balance2, 10, balance1.getAccount())
    			.build();
    	em.persist(dummyTransaction1);
    	dummyTransaction2 = ledger.createTransaction(TransactionType.PAYMENT, "description-2", "ref-2", Instant.parse("2020-04-07T17:00:00Z"), Instant.parse("2020-04-07T18:00:00Z"))
    			.credit(balance1, 10, balance2.getAccount())
    			.debit(balance2, 10, balance1.getAccount())
    			.build();
    	em.persist(dummyTransaction2);
    	dummyTransaction3 = ledger.createTransaction(TransactionType.PAYMENT, "description-3", "ref-3", Instant.parse("2020-04-07T17:00:00Z"), Instant.parse("2020-04-07T18:00:00Z"))
    			.credit(balance1, 10, balance2.getAccount())
    			.debit(balance2, 10, balance1.getAccount())
    			.build();
    	em.persist(dummyTransaction3);

		WithdrawalRequest wr1 = Fixture.createWithdrawalRequest(account1, user1, "Test my request 1", 100, dummyTransaction1);
    	em.persist(wr1);
    	PaymentBatch pb = Fixture.createPaymentBatch(user1);
    	em.persist(pb);
    	pb.addWithdrawalRequest(wr1);

    	WithdrawalRequest wr2 = Fixture.createWithdrawalRequest(account2, user2, "Test my request 2", 100, dummyTransaction2);
    	wr2.setSettlementTime(Instant.now());
    	wr2.setSettledBy(user2);
    	pb.addWithdrawalRequest(wr2);
    	wr2.setStatus(PaymentStatus.COMPLETED);
    	em.persist(wr2);
    	
		WithdrawalRequest wr3 = Fixture.createWithdrawalRequest(account1, user3, "Test my request 3", 100, dummyTransaction3);
    	em.persist(wr3);

    }
    
    @Test
    public void findPendingRequests() {
    	List<WithdrawalRequest> actual = dao.findPendingRequests();
    	assertNotNull(actual);
    	assertEquals(1, actual.size());
    	assertNull(actual.get(0).getPaymentBatch());
    	dump("save", actual);
    }

    @Test
    public void list_Default() throws Exception {
    	PagedResult<Long> actual = dao.list(null, null, null, null, 0, 0);
    	assertNotNull(actual);
    	assertEquals(3, actual.getTotalCount().longValue());
    }
    
    @Test
    public void list_Load() throws Exception {
    	PagedResult<Long> actual = dao.list(null, null, null, PaymentStatus.ACTIVE, 10, 0);
    	assertNotNull(actual);
    	assertEquals(1, actual.getCount());
		List<WithdrawalRequest> results = dao.loadGraphs(actual.getData(), WithdrawalRequest.LIST_GRAPH, WithdrawalRequest::getId);
    	assertNotNull(results);
    	assertEquals(1, results.size());
    	flush();
		WithdrawalRequest wr = results.get(0);
    	PersistenceUnitUtil puu = em.getEntityManagerFactory().getPersistenceUnitUtil();
    	assertNotNull(wr);
    	assertFalse(em.contains(wr));
    	assertTrue(puu.isLoaded(wr, WithdrawalRequest_.ACCOUNT));
    	assertTrue(puu.isLoaded(wr, WithdrawalRequest_.CREATED_BY));
    	assertTrue(puu.isLoaded(wr, WithdrawalRequest_.SETTLED_BY));
    	assertTrue(puu.isLoaded(wr, WithdrawalRequest_.PAYMENT_BATCH));
    	assertTrue(puu.isLoaded(wr, WithdrawalRequest_.AMOUNT_CREDITS));
    	assertFalse(puu.isLoaded(wr, WithdrawalRequest_.TRANSACTION));
    	PaymentBatch pb = wr.getPaymentBatch();
    	assertNotNull(pb);
    	assertTrue(puu.isLoaded(pb, PaymentBatch_.CREATION_TIME));
    	assertFalse(puu.isLoaded(pb, PaymentBatch_.WITHDRAWAL_REQUESTS));
    	assertTrue(puu.isLoaded(pb, PaymentBatch_.CREATED_BY));
    }

    @Test
    public void list_Fetch() throws Exception {
    	PagedResult<Long> actual = dao.list(null, null, null, PaymentStatus.ACTIVE, 10, 0);
    	assertNotNull(actual);
    	assertEquals(1, actual.getCount());
    	// There does not seem much difference between fetch and load in Hibvernate 5.3.
		List<WithdrawalRequest> results = dao.fetchGraphs(actual.getData(), WithdrawalRequest.LIST_GRAPH, WithdrawalRequest::getId);
    	assertNotNull(results);
    	assertEquals(1, results.size());
    	flush();
		WithdrawalRequest wr = results.get(0);
    	PersistenceUnitUtil puu = em.getEntityManagerFactory().getPersistenceUnitUtil();
    	assertNotNull(wr);
    	assertFalse(em.contains(wr));
    	assertTrue(puu.isLoaded(wr, WithdrawalRequest_.ACCOUNT));
    	assertTrue(puu.isLoaded(wr, WithdrawalRequest_.CREATED_BY));
    	assertTrue(puu.isLoaded(wr, WithdrawalRequest_.SETTLED_BY));
    	assertTrue(puu.isLoaded(wr, WithdrawalRequest_.PAYMENT_BATCH));
    	assertTrue(puu.isLoaded(wr, WithdrawalRequest_.AMOUNT_CREDITS));
    	assertFalse(puu.isLoaded(wr, WithdrawalRequest_.TRANSACTION));
    	PaymentBatch pb = wr.getPaymentBatch();
    	assertNotNull(pb);
    	assertTrue(puu.isLoaded(pb, PaymentBatch_.CREATION_TIME));
    	assertFalse(puu.isLoaded(pb, PaymentBatch_.WITHDRAWAL_REQUESTS));
    	assertTrue(puu.isLoaded(pb, PaymentBatch_.CREATED_BY));
    }

    @Test
    public void list_ByStatus() {
    	PagedResult<Long> actual = dao.list(null, null, null, PaymentStatus.REQUESTED, 0, 0);
    	assertNotNull(actual);
    	assertEquals(1, actual.getTotalCount().longValue());

    	actual = dao.list(null, null, null, PaymentStatus.COMPLETED, 0, 0);
    	assertNotNull(actual);
    	assertEquals(1, actual.getTotalCount().longValue());
    }
}
