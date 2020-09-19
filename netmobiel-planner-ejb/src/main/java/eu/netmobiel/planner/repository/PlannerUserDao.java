package eu.netmobiel.planner.repository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import eu.netmobiel.commons.repository.UserDao;
import eu.netmobiel.planner.annotation.PlannerDatabase;
import eu.netmobiel.planner.model.PlannerUser;


@ApplicationScoped
@Typed(PlannerUserDao.class)
public class PlannerUserDao extends UserDao<PlannerUser> {

	@Inject @PlannerDatabase
    private EntityManager em;

    public PlannerUserDao() {
		super(PlannerUser.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

}
