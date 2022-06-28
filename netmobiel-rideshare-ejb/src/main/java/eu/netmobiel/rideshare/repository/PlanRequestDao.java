package eu.netmobiel.rideshare.repository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.rideshare.annotation.RideshareDatabase;
import eu.netmobiel.rideshare.model.PlanRequest;

@ApplicationScoped
@Typed(PlanRequestDao.class)
public class PlanRequestDao extends AbstractDao<PlanRequest, Long> {

	@Inject @RideshareDatabase
    private EntityManager em;

    public PlanRequestDao() {
		super(PlanRequest.class);
	}
   
	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

}
