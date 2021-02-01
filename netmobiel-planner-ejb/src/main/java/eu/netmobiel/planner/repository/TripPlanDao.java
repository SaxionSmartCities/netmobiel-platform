package eu.netmobiel.planner.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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

import com.vividsolutions.jts.geom.Polygon;

import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.GeoLocation_;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.commons.util.EllipseHelper;
import eu.netmobiel.planner.annotation.PlannerDatabase;
import eu.netmobiel.planner.model.PlanType;
import eu.netmobiel.planner.model.PlannerUser;
import eu.netmobiel.planner.model.TripPlan;
import eu.netmobiel.planner.model.TripPlan_;

@ApplicationScoped
@Typed(TripPlanDao.class)
//@Logging
public class TripPlanDao extends AbstractDao<TripPlan, Long> {
    @SuppressWarnings("unused")
	@Inject
    private Logger logger;
    
    @Inject @PlannerDatabase
    private EntityManager em;

    public TripPlanDao() {
		super(TripPlan.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

    public PagedResult<Long> findTripPlans(PlannerUser traveller, PlanType planType, Instant since, Instant until, 
    		Boolean inProgressOnly, SortDirection sortDirection, Integer maxResults, Integer offset) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<TripPlan> plan = cq.from(TripPlan.class);
        List<Predicate> predicates = new ArrayList<>();
        if (traveller != null) {
        	predicates.add(cb.equal(plan.get(TripPlan_.traveller), traveller));
        }
        if (planType != null) {
            predicates.add(cb.equal(plan.get(TripPlan_.planType), planType));
        }
        if (since != null) {
	        predicates.add(cb.greaterThanOrEqualTo(plan.get(TripPlan_.travelTime), since));
        }        
        if (until != null) {
	        predicates.add(cb.lessThan(plan.get(TripPlan_.travelTime), until));
        }        
        if (inProgressOnly != null && inProgressOnly.booleanValue()) {
	        predicates.add(cb.isNull(plan.get(TripPlan_.requestDuration)));
        }
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (maxResults == 0) {
            cq.select(cb.count(plan.get(TripPlan_.id)));
            totalCount = em.createQuery(cq).getSingleResult();
        } else {
            cq.select(plan.get(TripPlan_.id));
            if (sortDirection == SortDirection.DESC) {
            	cq.orderBy(cb.desc(plan.get(TripPlan_.travelTime)), cb.desc(plan.get(TripPlan_.id)));
            } else {
            	cq.orderBy(cb.asc(plan.get(TripPlan_.travelTime)), cb.asc(plan.get(TripPlan_.id)));
            }
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(offset);
			tq.setMaxResults(maxResults);
			results = tq.getResultList();
        }
        return new PagedResult<Long>(results, maxResults, offset, totalCount);
    }

    /**
     * Lists a page of trip plans in progress of the shout-out type that have a departure or arrival location within a circle with radius 
     * <code>arrdepRadius</code> meter around the <code>location</code> and where both departure and arrival location are within
     * a circle with radius <code>travelRadius</code> meter. Consider only plans with a travel time beyond now.
     * For a shout-out we have two option: Drive to the nearby departure, then to the drop-off, then back home. The other way around is
     * also feasible. This why the small circle must included either departure or arrival location!
     * @param location the reference location of the driver asking for the trips.
     * @param startTime the time from where to start the search. 
     * @param depArrRadius the small circle containing at least departure or arrival location of the traveller.
     * @param travelRadius the larger circle containing both departure and arrival location of the traveller.
     * @param maxResults For paging: maximum results.
     * @param offset For paging: the offset in the results to return.
     * @return A list of trip plans matching the criteria.
     */
    public PagedResult<Long> findShoutOutPlans(GeoLocation location, Instant startTime, Integer depArrRadius, Integer travelRadius, Integer maxResults, Integer offset) {
    	Polygon deparrCircle = EllipseHelper.calculateCircle(location.getPoint(), depArrRadius);
    	Polygon travelCircle = EllipseHelper.calculateCircle(location.getPoint(), travelRadius);
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<TripPlan> plan = cq.from(TripPlan.class);
        List<Predicate> predicates = new ArrayList<>();
        // Only trips in planning state
        predicates.add(cb.equal(plan.get(TripPlan_.planType), PlanType.SHOUT_OUT));
        // Only consider trips that depart after startTime
        predicates.add(cb.greaterThanOrEqualTo(plan.get(TripPlan_.travelTime), startTime));
        // Select only the plans that are in progress
        predicates.add(cb.isNull(plan.get(TripPlan_.requestDuration)));
        // Either departure or arrival location must be within the small circle
        predicates.add(cb.or(
        		cb.isTrue(cb.function("st_within", Boolean.class, plan.get(TripPlan_.from).get(GeoLocation_.point), cb.literal(deparrCircle))),
        		cb.isTrue(cb.function("st_within", Boolean.class, plan.get(TripPlan_.to).get(GeoLocation_.point), cb.literal(deparrCircle)))
        ));
        // Both departure and arrival location must be within the large circle.
        predicates.add(cb.and(
        		cb.isTrue(cb.function("st_within", Boolean.class, plan.get(TripPlan_.from).get(GeoLocation_.point), cb.literal(travelCircle))),
        		cb.isTrue(cb.function("st_within", Boolean.class, plan.get(TripPlan_.to).get(GeoLocation_.point), cb.literal(travelCircle)))
        ));
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (maxResults == 0) {
            cq.select(cb.count(plan.get(TripPlan_.id)));
            totalCount = em.createQuery(cq).getSingleResult();
        } else {
            cq.select(plan.get(TripPlan_.id));
            // Order by increasing departure time
	        cq.orderBy(cb.asc(plan.get(TripPlan_.travelTime)));
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(offset);
			tq.setMaxResults(maxResults);
			results = tq.getResultList();
        }
        return new PagedResult<Long>(results, maxResults, offset, totalCount);
    }

}