package eu.netmobiel.planner.repository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.Logger;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.planner.annotation.PlannerDatabase;
import eu.netmobiel.planner.model.OtpRoute;

@ApplicationScoped
@Typed(OtpRouteDao.class)
public class OtpRouteDao extends AbstractDao<OtpRoute, String> {

    @Inject
    private Logger log;

	@Inject @PlannerDatabase
    private EntityManager em;

    public OtpRouteDao() {
		super(OtpRoute.class);
	}

    @Override
	protected EntityManager getEntityManager() {
		return em;
	}

	public void markAllStale() {
    	log.debug("markAllStale: Mark all routes as stale");
    	Query q = em.createQuery("update OtpRoute r set r.stale = true");
    	int affectedRows = q.executeUpdate();
    	log.debug("markAllStale: Rows updated #" + affectedRows);
    }

	public void removeAllStale() {
    	log.debug("removeAllStale: Remove all stale routes");
    	Query q = em.createQuery("delete from OtpRoute where stale = true");
    	int affectedRows = q.executeUpdate();
    	log.debug("removeAllStale: Rows updated #" + affectedRows);
    }

}
