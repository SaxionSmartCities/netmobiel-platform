package eu.netmobiel.commons.keycloak;

import java.util.Map;

import org.keycloak.adapters.authorization.ClaimInformationPointProviderFactory;
import org.keycloak.adapters.authorization.PolicyEnforcer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyClaimInformationPointProviderFactory implements ClaimInformationPointProviderFactory<MyClaimInformationPointProvider> {
	private static final Logger log = LoggerFactory.getLogger(MyClaimInformationPointProviderFactory.class);
	
	@Override
    public String getName() {
        return "my-nb-claims";
    }

    @Override
    public void init(PolicyEnforcer policyEnforcer) {
    	log.debug("init()");
    }

    @Override
    public MyClaimInformationPointProvider create(Map<String, Object> config) {
    	log.debug("create()");
        return new MyClaimInformationPointProvider(config);
    }
}
