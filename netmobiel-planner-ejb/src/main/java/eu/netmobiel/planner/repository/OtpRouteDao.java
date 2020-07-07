package eu.netmobiel.planner.repository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.planner.annotation.PlannerDatabase;
import eu.netmobiel.planner.model.OtpRoute;

@ApplicationScoped
@Typed(OtpRouteDao.class)
public class OtpRouteDao extends AbstractDao<OtpRoute, String> {

	@Inject @PlannerDatabase
    private EntityManager em;

    public OtpRouteDao() {
		super(OtpRoute.class);
	}

    @Override
	protected EntityManager getEntityManager() {
		return em;
	}

}
