package eu.netmobiel.banker.service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;

@Singleton
@Startup
public class BankerStartupService {
    public enum States { BEFORESTARTED, STARTED, SHUTTINGDOWN };
    private States state;
    
    @PostConstruct
    public void initialize() {
        state = States.BEFORESTARTED;
        // Perform intialization
        state = States.STARTED;
        System.out.println("Service Started");
    }
    @PreDestroy
    public void terminate() {
        state = States.SHUTTINGDOWN;
        // Perform termination
        System.out.println("Shut down in progress");
    }

    public States getState() {
        return state;
    }
    
    public void setState(States state) {
        this.state = state;
    }
}
