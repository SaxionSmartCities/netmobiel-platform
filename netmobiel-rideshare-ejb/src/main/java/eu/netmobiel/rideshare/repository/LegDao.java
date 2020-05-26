package eu.netmobiel.rideshare.repository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.rideshare.annotation.RideshareDatabase;
import eu.netmobiel.rideshare.model.Leg;

@ApplicationScoped
@Typed(LegDao.class)
public class LegDao extends AbstractDao<Leg, Long> {

    @Inject @RideshareDatabase
    private EntityManager em;

    public LegDao() {
		super(Leg.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

}