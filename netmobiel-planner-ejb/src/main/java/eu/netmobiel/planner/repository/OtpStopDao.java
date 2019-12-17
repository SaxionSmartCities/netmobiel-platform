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
    	Query q = em.createQuery("update OtpStop s set s.nrRoutes = " + 
    			"(select count(distinct r) from OtpRoute r join r.stops ss where ss = s)");
    	int affectedRows = q.executeUpdate();
    	log.debug("updateNrRoutes: Rows updated #" + affectedRows);
    }

}
