package eu.netmobiel.banker.service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.PermitAll;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.AccountingTransaction;
import eu.netmobiel.banker.model.DepositRequest;
import eu.netmobiel.banker.model.PaymentStatus;
import eu.netmobiel.banker.repository.DepositRequestDao;
import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.payment.client.PaymentClient;
import eu.netmobiel.payment.client.model.PaymentLink;
import eu.netmobiel.payment.client.model.PaymentLinkStatus;

/**
 * Stateless bean for the management of the ledger.
 * 
 * TODO: Security
 * 
 * @author Jaap Reitsma
 *
 */
		
@Stateless
@Logging
@DeclareRoles({ "admin" })
@PermitAll
public class DepositService {
	public static final int PAYMENT_LINK_EXPIRATION_SECS = 15 * 60;

    @Inject
    private Logger log;

    @Inject
    private DepositRequestDao depositRequestDao;

    @Inject
    private PaymentClient paymentClient;

    @Inject
    private LedgerService ledgerService;
    
    /**
     * Verify the active deposits every hour. 
     */
	@Schedule(info = "Deposit check", hour = "*/1", minute = "0", second = "0", persistent = false /* non-critical job */)
	public void verifyActiveDepositRequests() {
		// Get the list of active deposit requests
		// For each request:
		//    Check the status at the provider
		List<DepositRequest> activeRequests = depositRequestDao.listByStatus(PaymentStatus.ACTIVE);
		if (! activeRequests.isEmpty()) {
			log.info(String.format("Checking %d active deposit request(s)", activeRequests.size()));
			for (DepositRequest dr : activeRequests) {
		    	try {
			    	PaymentLink plink = paymentClient.getPaymentLink(dr.getPaymentLinkId());
					synchronizeDepositionRequest(dr, plink);
		    	} catch (Exception ex) {
		    		log.error("Error verivying deposit request - " + ex.getMessage());
		    	}
			}
			activeRequests = depositRequestDao.listByStatus(PaymentStatus.ACTIVE);
			log.info(String.format("Now %d active deposit requests", activeRequests.size()));
		}
	}
	
    /**
     * Creates a payment link request at the payment provider of netmobiel
     * @param account account to deposit credits to.
     * @param amountCredits the number of credits to deposit. 
     * @param description the description to use on the payment page
     * @param returnUrl the url to use to return to. The payment provider will add query parameters.
     * 			The parameter object_id must be passed on to the method verifyDeposition.   
     * @return the url to the payment page for the client to redirect the browser to.
     */
    public String createDepositRequest(Account acc, int amountCredits, String description, String returnUrl) {
    	DepositRequest dr = new DepositRequest();
    	
    	PaymentLink plink = new PaymentLink();
    	plink.amount = amountCredits * LedgerService.CREDIT_EXCHANGE_RATE;
    	plink.description = description;
    	plink.expirationPeriod = Duration.ofSeconds(PAYMENT_LINK_EXPIRATION_SECS);
    	plink.merchantOrderId = String.format("%s-%d", acc.getNcan(), Instant.now().getEpochSecond());
    	plink.returnUrl = returnUrl;
    	plink = paymentClient.createPaymentLink(plink);
    	// Only id and payment url are added now by the client. Do not use other fields, they are not yet set!
    	dr.setAccount(acc);
    	dr.setAmountCredits(amountCredits);
    	dr.setAmountEurocents(plink.amount);
    	dr.setMerchantOrderId(plink.merchantOrderId);
    	dr.setDescription(plink.description);
    	dr.setCreationTime(Instant.now());
    	// The development environment of EMS Pay ignores our expiration period 
    	dr.setExpirationTime(dr.getCreationTime().plusSeconds(plink.expirationPeriod.getSeconds()));
    	dr.setPaymentLinkId(plink.id);
    	dr.setStatus(PaymentStatus.ACTIVE);
    	depositRequestDao.save(dr);
    	return plink.paymentUrl;
    }

    /**
     * Verifies the deposition of credits by the order id supplied by the payment provider.
     * The order id is one of the parameters added to the return url after completing the payment at the payment page of
     * the payment provider. In addition, the order id is also part of the payment webhook. 
     * @param paymentProjectId The project id as supplied by the payment provider.
     * @param paymentOrderId The order id as supplied by the payment provider.
     * @return true if the deposition of the payment link was added now or earlier. If false then deposition has not taken place (yet), or something
     * 			went wrong.
     */
    public Optional<DepositRequest> verifyDeposition(String paymentProjectId, String paymentOrderId) {
    	DepositRequest dr = null;
    	try {
	    	PaymentLink plink = paymentClient.getPaymentLinkByOrderId(paymentOrderId);
	    	// Possible optimization: 
	    	// First get payment link id, fetch deposit request, then if still ACTIVE get the payment link record.
	    	dr = depositRequestDao.findByPaymentLink(plink.id);
	    	synchronizeDepositionRequest(dr, plink);
    	} catch (Exception ex) {
    		log.error("Error verivying deposition - " + ex.getMessage());
    	}
    	return Optional.ofNullable(dr == null ? null : dr);
    }

    /**
     * Synchronizes the the deposit request with the payment link at the payment provider.
     * @param dr The deposit request to verify.
     * @param paymentLink The payment link object retrieved from the payment provider.
     */
    public void synchronizeDepositionRequest(DepositRequest dr, PaymentLink plink) {
    	if (!plink.id.equals(dr.getPaymentLinkId())) {
    		throw new IllegalArgumentException("Invalid combination of deposit request and payment link");
    	}
    	// Assure the request is in the persistence context
    	DepositRequest dr_db = depositRequestDao.find(dr.getId())
    			.orElseThrow(() -> new IllegalArgumentException("DepositRequest has gone: dr.getId()"));
    	if (dr_db.getStatus() == PaymentStatus.ACTIVE) {
        	if (plink.status == PaymentLinkStatus.COMPLETED) {
        		// Transition to COMPLETED, add transaction to deposit credits in the Netmobiel system
        		dr_db.setCompletedTime(plink.completed.toInstant());
        		AccountingTransaction tr = ledgerService.deposit(dr_db.getAccount(), dr_db.getAmountCredits(), 
        				dr_db.getCompletedTime().atOffset(ZoneOffset.UTC), dr_db.getDescription(), dr_db.getDepositRequestRef());
        		dr_db.setTransaction(tr);
        		dr_db.setStatus(PaymentStatus.COMPLETED);
        		log.info(String.format("DepositRequest %d has completed", dr_db.getId()));
        	} else if (plink.status == PaymentLinkStatus.EXPIRED) {
        		dr_db.setStatus(PaymentStatus.EXPIRED);
        		log.info(String.format("DepositRequest %d has expired", dr_db.getId()));
        	}
    	}
    }
    
}
