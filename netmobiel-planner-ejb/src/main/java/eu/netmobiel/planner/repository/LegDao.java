package eu.netmobiel.planner.repository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.slf4j.Logger;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.planner.annotation.PlannerDatabase;
import eu.netmobiel.planner.model.Leg;

@ApplicationScoped
@Typed(LegDao.class)
public class LegDao extends AbstractDao<Leg, Long> {
    @SuppressWarnings("unused")
	@Inject
    private Logger logger;
    
    @Inject @PlannerDatabase
    private EntityManager em;

    public LegDao() {
		super(Leg.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

	public int updateDriverId(String oldId, String newId) {
    	return em.createQuery("update Leg lg set lg.driverId = :newDriverId where lg.driverId = :oldDriverId")
    			.setParameter("oldDriverId", oldId)
    			.setParameter("newDriverId", newId)
    			.executeUpdate();
	}
	
}