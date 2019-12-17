package eu.netmobiel.rideshare.repository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.rideshare.annotation.RideshareDatabase;
import eu.netmobiel.rideshare.model.Stop;

@ApplicationScoped
@Typed(StopDao.class)
public class StopDao extends AbstractDao<Stop, Long> {

    @SuppressWarnings("unused")
	@Inject @RideshareDatabase
    private EntityManager em;

    public StopDao() {
		super(Stop.class);
	}
   
	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

}
