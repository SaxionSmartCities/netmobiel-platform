package eu.netmobiel.planner.repository;

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
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;

import com.vividsolutions.jts.geom.Polygon;

import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.GeoLocation_;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.commons.util.EllipseHelper;
import eu.netmobiel.planner.annotation.PlannerDatabase;
import eu.netmobiel.planner.filter.ShoutOutFilter;
import eu.netmobiel.planner.filter.TripPlanFilter;
import eu.netmobiel.planner.model.PlanType;
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

	/**
	 * Lists trip plans according the specified criteria.
	 * @param filter the trip plan filter to apply
	 * @param cursor the cursor
	 * @return A list of trip plans.
	 */
    public PagedResult<Long> findTripPlans(TripPlanFilter filter, Cursor cursor) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<TripPlan> plan = cq.from(TripPlan.class);
        List<Predicate> predicates = new ArrayList<>();
        if (filter.getTraveller() != null) {
        	predicates.add(cb.equal(plan.get(TripPlan_.traveller), filter.getTraveller()));
        }
        if (filter.getPlanType() != null) {
            predicates.add(cb.equal(plan.get(TripPlan_.planType), filter.getPlanType()));
        }
        if (filter.getSince() != null) {
	        predicates.add(cb.greaterThanOrEqualTo(plan.get(TripPlan_.travelTime), filter.getSince()));
        }        
        if (filter.getUntil() != null) {
	        predicates.add(cb.lessThan(plan.get(TripPlan_.travelTime), filter.getUntil()));
        }        
        if (filter.getInProgress() != null) { 
    		Expression<?> rd = plan.get(TripPlan_.requestDuration);
	        predicates.add(filter.getInProgress().booleanValue() ? cb.isNull(rd) : cb.isNotNull(rd));
        }
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (cursor.isCountingQuery()) {
            cq.select(cb.count(plan.get(TripPlan_.id)));
            totalCount = em.createQuery(cq).getSingleResult();
        } else {
            cq.select(plan.get(TripPlan_.id));
            if (filter.getSortDir() == SortDirection.DESC) {
            	cq.orderBy(cb.desc(plan.get(TripPlan_.travelTime)), cb.desc(plan.get(TripPlan_.id)));
            } else {
            	cq.orderBy(cb.asc(plan.get(TripPlan_.travelTime)), cb.asc(plan.get(TripPlan_.id)));
            }
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(cursor.getOffset());
			tq.setMaxResults(cursor.getMaxResults());
			results = tq.getResultList();
        }
        return new PagedResult<>(results, cursor, totalCount);
    }

    /**
     * Lists a page of trip plans in progress of the shout-out type that have a departure or arrival location within a circle with radius 
     * <code>arrdepRadius</code> meter around the <code>location</code> and where both departure and arrival location are within
     * a circle with radius <code>travelRadius</code> meter. Consider only plans with a travel time beyond now.
     * For a shout-out we have two option: Drive to the nearby departure, then to the drop-off, then back home. The other way around is
     * also feasible. This why the small circle must included either departure or arrival location!
     * @param filter The shout-out filter to apply
     * @param cursor the cursor to apply.
     * @return A list of trip plans matching the criteria.
     */
    public PagedResult<Long> findShoutOutPlans(ShoutOutFilter filter, Cursor cursor) {
    	Polygon deparrCircle = EllipseHelper.calculateCircle(filter.getLocation().getPoint(), filter.getDepArrRadius());
    	Polygon travelCircle = EllipseHelper.calculateCircle(filter.getLocation().getPoint(), filter.getTravelRadius());
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<TripPlan> plan = cq.from(TripPlan.class);
        List<Predicate> predicates = new ArrayList<>();
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
        // Only plans of shout_out type
        predicates.add(cb.equal(plan.get(TripPlan_.planType), PlanType.SHOUT_OUT));
        if (filter.getCaller() != null) {
	        // Only plans not issued by me
	        predicates.add(cb.notEqual(plan.get(TripPlan_.traveller), filter.getCaller()));
        }
        if (filter.getSince() != null) {
            // Only consider plans that travel after since
            predicates.add(cb.greaterThanOrEqualTo(plan.get(TripPlan_.travelTime), filter.getSince()));
        }
        if (filter.getUntil() != null) {
            // Only consider plans that travel before until
            predicates.add(cb.lessThan(plan.get(TripPlan_.travelTime), filter.getUntil()));
        }
        if (filter.isInProgressOnly()) {
	        // Select only the plans that are in progress
	        predicates.add(cb.isNull(plan.get(TripPlan_.requestDuration)));
        }
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (cursor.isCountingQuery()) {
            cq.select(cb.count(plan.get(TripPlan_.id)));
            totalCount = em.createQuery(cq).getSingleResult();
        } else {
            cq.select(plan.get(TripPlan_.id));
            // Order by travel time according filter
            Expression<?> orderExpr = plan.get(TripPlan_.travelTime);
	        cq.orderBy(filter.getSortDir() == SortDirection.ASC ? cb.asc(orderExpr) : cb.desc(orderExpr));
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(cursor.getOffset());
			tq.setMaxResults(cursor.getMaxResults());
			results = tq.getResultList();
        }
        return new PagedResult<>(results, cursor, totalCount);
    }

}