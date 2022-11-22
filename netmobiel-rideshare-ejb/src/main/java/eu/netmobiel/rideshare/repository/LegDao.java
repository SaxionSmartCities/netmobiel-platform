package eu.netmobiel.rideshare.repository;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.rideshare.annotation.RideshareDatabase;
import eu.netmobiel.rideshare.model.Leg;
import eu.netmobiel.rideshare.model.Ride;

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

    public List<Leg> listLegs(Ride ride) {
    	TypedQuery<Leg> tq = em.createQuery(
    			"from Leg where ride = :ride order by legIx asc"
    			, Leg.class)
    			.setParameter("ride", ride);
    	return tq.getResultList();
    }
}