package eu.netmobiel.planner.repository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.Logger;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.planner.annotation.PlannerDatabase;
import eu.netmobiel.planner.model.OtpStop;

@ApplicationScoped
@Typed(OtpStopDao.class)
public class OtpStopDao extends AbstractDao<OtpStop, String> {

    @Inject
    private Logger log;

    @Inject @PlannerDatabase
    private EntityManager em;

    public OtpStopDao() {
		super(OtpStop.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

    public void updateNrRoutes() {
    	log.debug("updateNrRoutes: Start update stop count");
    	Query q = em.createQuery("update OtpStop s set s.nrRoutes = " + 
    			"(select count(distinct r) from OtpRoute r join r.stops ss where ss = s)");
    	int affectedRows = q.executeUpdate();
    	log.debug("updateNrRoutes: Rows updated #" + affectedRows);
    }

	public void markAllStale() {
    	log.debug("markAllStale: Mark all stops as stale");
    	Query q = em.createQuery("update OtpStop s set s.stale = true");
    	int affectedRows = q.executeUpdate();
    	log.debug("markAllStale: Rows updated #" + affectedRows);
    }

	public void removeAllStale() {
    	log.debug("removeAllStale: Remove all stale stops");
    	Query q = em.createQuery("delete from OtpStop where stale = true");
    	int affectedRows = q.executeUpdate();
    	log.debug("removeAllStale: Rows updated #" + affectedRows);
    }
}
