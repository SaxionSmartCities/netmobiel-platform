package eu.netmobiel.planner.service;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.util.Logging;

/**
 * Migration service to help migration of things that can not easy be fixed by an sql script.
 *  
 * @author Jaap Reitsma
 *
 */
@Stateless
@Logging
public class PlannerMigrationService {

	@SuppressWarnings("unused")
	@Inject
    private Logger log;
}