package eu.netmobiel.rideshare.repository;

import java.time.Instant;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.rideshare.annotation.RideshareDatabase;
import eu.netmobiel.rideshare.model.RideTemplate;

@ApplicationScoped
@Typed(RideTemplateDao.class)
public class RideTemplateDao extends AbstractDao<RideTemplate, Long> {

	@Inject @RideshareDatabase
    private EntityManager em;

    public RideTemplateDao() {
		super(RideTemplate.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

    public Long getNrRidesAttached(RideTemplate template) {
    	TypedQuery<Long> tq = em.createQuery(
    			"select count(r) from Ride r where r.rideTemplate = :template", Long.class)
    			.setParameter("template", template);
    	return tq.getSingleResult();
    }

    /**
     * Finds the templates that are eligible for generating new ride. An eligible template
     * has a departure time set before the system horizon and before the template horizon if available. 
     * @param systemHorizon the system horizon, i.e. the time in the future that limits the insertion of new rides.  
     * @param offset the offset paging results.
     * @param maxResults The maximum number of results to fetch in one query.
     * @return
     */
    public List<RideTemplate> findOpenTemplates(Instant systemHorizon, int offset, int maxResults) {
    	TypedQuery<RideTemplate> tq = em.createQuery(
    			"select rt from RideTemplate rt where rt.departureTime < :systemHorizon and " + 
    					"(rt.recurrence.horizon is null or rt.departureTime < rt.recurrence.horizon) "
    			, RideTemplate.class)
    			.setParameter("systemHorizon", systemHorizon)
    			.setFirstResult(offset)
    			.setMaxResults(maxResults);
    	return tq.getResultList();
    }
    
}
