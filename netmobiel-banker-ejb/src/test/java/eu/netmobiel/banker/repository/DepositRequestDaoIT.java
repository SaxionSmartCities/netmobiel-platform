package eu.netmobiel.banker.repository;


import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

import eu.netmobiel.banker.model.DepositRequest;
import eu.netmobiel.banker.model.PaymentStatus;
import eu.netmobiel.banker.test.BankerIntegrationTestBase;

@RunWith(Arquillian.class)
public class DepositRequestDaoIT extends BankerIntegrationTestBase {
    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive archive = createDeploymentBase()
                .addClass(DepositRequestDao.class)
                ;
//   		System.out.println(archive.toString(true));
		return archive;
    }

    @Inject
    private Logger log;
    
    @Inject
    private DepositRequestDao dao;

    private void dump(String subject, Collection<DepositRequest> drs) {
    	drs.forEach(m -> log.info(subject + ": " + drs.toString()));
    }
    
    @Override
    protected void insertData() throws Exception {
    	prepareBasicLedger();
    	createAndAssignUsers();
    }
    
    @Test
    public void findActive() {
		DepositRequest dr = new DepositRequest();
		dr.setAccount(account1);
		dr.setAmountCredits(100);
		dr.setAmountEurocents(1900);
		dr.setCreationTime(Instant.now());
		dr.setExpirationTime(dr.getCreationTime().plusSeconds(60 * 60 * 24 * 7));
		dr.setDescription("Test my request");
		dr.setPaymentLinkId("my-payment-link");
		dr.setStatus(PaymentStatus.ACTIVE);
		dr.setTransaction(null);
    	dao.save(dr);
    	DepositRequest actual = dao.findByPaymentLink(dr.getPaymentLinkId());
    	assertNotNull(actual);
    	assertEquals(dr, actual);
    	dump("save", Collections.singletonList(actual));
    }

}
