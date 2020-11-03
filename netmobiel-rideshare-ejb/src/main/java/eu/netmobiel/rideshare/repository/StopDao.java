package eu.netmobiel.rideshare.repository;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.rideshare.annotation.RideshareDatabase;
import eu.netmobiel.rideshare.model.Ride;
import eu.netmobiel.rideshare.model.Stop;

@ApplicationScoped
@Typed(StopDao.class)
public class StopDao extends AbstractDao<Stop, Long> {

	@Inject @RideshareDatabase
    private EntityManager em;

    public StopDao() {
		super(Stop.class);
	}
   
	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

    public List<Stop> listStops(Ride ride) {
    	TypedQuery<Stop> tq = em.createQuery(
    			"from Stop where ride = :ride"
    			, Stop.class)
    			.setParameter("ride", ride);
    	return tq.getResultList();
    }
}
