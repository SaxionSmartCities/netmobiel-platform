package eu.netmobiel.banker.repository;


import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.AccountType;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.PaymentStatus;
import eu.netmobiel.banker.model.WithdrawalRequest;
import eu.netmobiel.banker.test.BankerIntegrationTestBase;
import eu.netmobiel.banker.test.Fixture;

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
    private WithdrawalRequestDao dao;

    private BankerUser driver1;
    private Account account1;

    public boolean isSecurityRequired() {
    	return false;
    }
    
    private void dump(String subject, Collection<WithdrawalRequest> wrs) {
    	wrs.forEach(m -> log.info(subject + ": " + wrs.toString()));
    }
    
    @Override
    protected void insertData() throws Exception {
	    driver1 = Fixture.createDriver1();
		em.persist(driver1);
		account1 = Account.newInstant("PAL-1", "Account 1", AccountType.LIABILITY, Instant.parse("2020-07-01T00:00:00Z"));
		em.persist(account1);
    }
    
    @Test
    public void findActive() {
		WithdrawalRequest wr = new WithdrawalRequest();
		wr.setAccount(account1);
		wr.setAmountCredits(100);
		wr.setAmountEurocents(1900);
		wr.setCreationTime(Instant.now());
		wr.setDescription("Test my request");
		wr.setRequestedBy(driver1);
		wr.setStatus(PaymentStatus.ACTIVE);
    	dao.save(wr);
    	List<WithdrawalRequest> actual = dao.findByStatus(PaymentStatus.ACTIVE);
    	assertNotNull(actual);
    	assertEquals(1, actual.size());
    	assertEquals(wr, actual.get(0));
    	dump("save", actual);
    }

}
