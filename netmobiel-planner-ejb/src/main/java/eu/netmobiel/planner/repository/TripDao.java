package eu.netmobiel.planner.repository;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.planner.annotation.PlannerDatabase;
import eu.netmobiel.planner.model.Trip;
import eu.netmobiel.planner.model.Trip_;
import eu.netmobiel.planner.model.User;

@ApplicationScoped
@Typed(TripDao.class)
public class TripDao extends AbstractDao<Trip, Long> {
    @SuppressWarnings("unused")
	@Inject
    private Logger logger;
    
    @Inject @PlannerDatabase
    private EntityManager em;

    public TripDao() {
		super(Trip.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

    public List<Trip> findByTraveller(User traveller, LocalDate since, LocalDate until, boolean deletedToo, String graphName) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Trip> cq = cb.createQuery(Trip.class);
        Root<Trip> trips = cq.from(Trip.class);
        cq.select(trips);
        List<Predicate> predicates = new ArrayList<>();
        Predicate predTraveller = cb.equal(trips.get(Trip_.traveller), traveller);
        predicates.add(predTraveller);
        if (since != null) {
	        Predicate predSince = cb.greaterThanOrEqualTo(trips.get(Trip_.departureTime), since.atStartOfDay().toInstant(ZoneOffset.UTC));
	        predicates.add(predSince);
        }        
        if (until != null) {
	        Predicate predUntil = cb.lessThanOrEqualTo(trips.get(Trip_.departureTime), until.atStartOfDay().toInstant(ZoneOffset.UTC));
	        predicates.add(predUntil);
        }        
        if (! deletedToo) {
            Predicate predNotDeleted = cb.or(cb.isNull(trips.get(Trip_.deleted)), cb.isFalse(trips.get(Trip_.deleted)));
	        predicates.add(predNotDeleted);
        }
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        cq.orderBy(cb.asc(trips.get(Trip_.departureTime)));
        
        TypedQuery<Trip> tq = em.createQuery(cq);
        if (graphName != null) {
        	tq.setHint(JPA_HINT_LOAD, em.getEntityGraph(graphName));
        }
        return tq.getResultList();
    }


}