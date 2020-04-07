package eu.netmobiel.banker.service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.slf4j.Logger;

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
    public void bootstrapTheBank() {
        state = States.BEFORESTARTED;
   		ledgerService.bootstrapTheBank();
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
    
    public void setState(States state) {
        this.state = state;
    }
}
