package eu.netmobiel.planner.repository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.slf4j.Logger;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.planner.annotation.PlannerDatabase;
import eu.netmobiel.planner.model.TripPlan;

@ApplicationScoped
@Typed(TripPlanDao.class)
public class TripPlanDao extends AbstractDao<TripPlan, Long> {
    @SuppressWarnings("unused")
	@Inject
    private Logger logger;
    
    @Inject @PlannerDatabase
    private EntityManager em;

    public TripPlanDao() {
		super(TripPlan.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}


}