package eu.netmobiel.commons.keycloak;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.adapters.authorization.ClaimInformationPointProvider;
import org.keycloak.adapters.spi.HttpFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyClaimInformationPointProvider implements ClaimInformationPointProvider {
	private static final Logger log = LoggerFactory.getLogger(MyClaimInformationPointProvider.class);

    @SuppressWarnings("unused")
	private final Map<String, Object> config;

    public MyClaimInformationPointProvider(Map<String, Object> config) {
    	log.debug("Constructor");
        this.config = config;
    }

    @Override
    public Map<String, List<String>> resolve(HttpFacade httpFacade) {
        Map<String, List<String>> claims = new HashMap<>();

        log.debug("resolve()");
        // put whatever claim you want into the map
        claims.put("pipo", Collections.singletonList("de clown"));
        return claims;
    }
}
