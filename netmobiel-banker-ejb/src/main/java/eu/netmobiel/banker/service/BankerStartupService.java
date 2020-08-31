package eu.netmobiel.banker.service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.banker.model.AccountType;
import eu.netmobiel.banker.model.Ledger;
import eu.netmobiel.commons.model.PagedResult;

@Singleton
@Startup
public class BankerStartupService {
    @Inject
    private Logger logger;

	public enum States { BEFORESTARTED, STARTED, SHUTTINGDOWN };
    
    private States state;
    
    @EJB
    private LedgerService ledgerService;

    /**
     * Creates the initial data structure for the credit system: A system user is created, a first ledger starting at January 1st.
     */
    @PostConstruct
    public void startup() {
        state = States.BEFORESTARTED;
   		bootstrapTheBank();
        state = States.STARTED;
        logger.info("Started");
    }

    @PreDestroy
    public void terminate() {
        state = States.SHUTTINGDOWN;
        logger.info("Shutting down");
    }

    public States getState() {
        return state;
    }
    
    public void bootstrapTheBank() {
    	PagedResult<Ledger> prl = ledgerService.listLedgers(1, 0);
    	if (prl.getTotalCount() == 0) {
    		// No active ledger, create the initial ledger and the rest
    		OffsetDateTime odt = OffsetDateTime.of(Instant.now().atOffset(ZoneOffset.UTC).getYear(), 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    		ledgerService.createLedger(odt.toInstant());
    	}
   		ledgerService.prepareAccount(LedgerService.ACC_REF_BANKING_RESERVE, LedgerService.ACC_NAME_BANKING_RESERVE, AccountType.ASSET);
   		ledgerService.prepareAccount(LedgerService.ACC_REF_RESERVATIONS, LedgerService.ACC_NAME_RESERVATIONS, AccountType.LIABILITY);
    }

}
