package eu.netmobiel.planner.service;

import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.util.Logging;
import eu.netmobiel.planner.repository.LegDao;

/**
 * Migration service to help migration of things that can not easy be fixed by an sql script.
 *  
 * @author Jaap Reitsma
 *
 */
@Stateless
@Logging
public class PlannerMigrationService {

	@Inject
    private Logger log;

    @Inject
    private LegDao legDao;

    public void updateDriverIdInLeg(Map<String, String> rsuser2keycloakMap) {
    	for (Map.Entry<String, String> entry : rsuser2keycloakMap.entrySet()) {
			String rsuserId = entry.getKey();
			String keycloakId = entry.getValue();
			int count = legDao.updateDriverId(rsuserId, keycloakId);
			if (count > 0) {
				log.info(String.format("Migrated %d legs from %s to %s", count, rsuserId, keycloakId));
			}
		}
    }

}