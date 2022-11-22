package eu.netmobiel.planner.repository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.slf4j.Logger;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.planner.annotation.PlannerDatabase;
import eu.netmobiel.planner.model.Itinerary;

@ApplicationScoped
@Typed(ItineraryDao.class)
public class ItineraryDao extends AbstractDao<Itinerary, Long> {
    @SuppressWarnings("unused")
	@Inject
    private Logger logger;
    
    @Inject @PlannerDatabase
    private EntityManager em;

    public ItineraryDao() {
		super(Itinerary.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}


}