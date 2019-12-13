package eu.netmobiel.rideshare.repository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import eu.netmobiel.rideshare.model.RideTemplate;

@ApplicationScoped
@Typed(RideTemplateDao.class)
public class RideTemplateDao extends AbstractDao<RideTemplate, Long> {

	@Inject
    private EntityManager em;

    public RideTemplateDao() {
		super(RideTemplate.class);
	}

    public Long getNrRidesAttached(RideTemplate template) {
    	TypedQuery<Long> tq = em.createQuery(
    			"select count(r) from Ride r where r.rideTemplate = :template", Long.class)
    			.setParameter("template", template);
    	return tq.getSingleResult();
    }
}
