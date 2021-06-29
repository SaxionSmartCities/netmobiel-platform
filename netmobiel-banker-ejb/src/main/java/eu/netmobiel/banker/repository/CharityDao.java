package eu.netmobiel.banker.repository;

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
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;

import com.vividsolutions.jts.geom.Polygon;

import eu.netmobiel.banker.annotation.BankerDatabase;
import eu.netmobiel.banker.model.Charity;
import eu.netmobiel.banker.model.CharitySortBy;
import eu.netmobiel.banker.model.Charity_;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.GeoLocation_;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.commons.util.EllipseHelper;

@ApplicationScoped
@Typed(CharityDao.class)
public class CharityDao extends AbstractDao<Charity, Long> {
    @SuppressWarnings("unused")
	@Inject
    private Logger logger;
    
    @Inject @BankerDatabase
    private EntityManager em;

    public CharityDao() {
		super(Charity.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

	private Expression<?> createOrderExpression(CriteriaBuilder cb, Root<Charity> root, CharitySortBy sortBy, GeoLocation location) throws BadRequestException {
        Expression<?> orderExpr = null;
        if (sortBy == CharitySortBy.SCORE) {
        	// Convert to promille first, quot acts as an integer division
        	orderExpr = cb.quot(cb.prod(1000, root.get(Charity_.donatedAmount)), root.get(Charity_.goalAmount));
        } else if (sortBy == CharitySortBy.DATE) {
        	orderExpr = root.get(Charity_.campaignStartTime);
        } else if (sortBy == CharitySortBy.DISTANCE) {
        	if (location == null) {
        		throw new BadRequestException("Cannot sort by distance without a location");
        	}
        	orderExpr = cb.function("st_distance", Double.class, 
        			root.get(Charity_.location).get(GeoLocation_.point), cb.literal(location.getPoint()));
        } else if (sortBy == CharitySortBy.NAME) {
        	orderExpr = root.get(Charity_.name);
        } else {
        	throw new IllegalStateException("Sort by not supported: " + sortBy);
        }
		return orderExpr;
	}
	
    /**
     * Lists charities within a specified radius around a location. 
     * @param now the reference time. Especially needed for testing.
     * @param location the reference location as the center point to search for charities.
     * @param radius the circle radius in meter.
     * @param since only lists charities that start campaigning after this date.
     * @param until limit the search to charities having started campaigning before this date.
     * @param inactiveToo also finds charities that are no longer campaigning.
     * @param sortBy which sort key to use. Default is by name.
     * @param sortDir which sort direction. Default is ascending.
     * @param maxResults For paging: maximum results.
     * @param offset For paging: the offset in the results to return.
     * @return A list of charities matching the criteria.
     */
    public PagedResult<Long> findCharities(Instant now, GeoLocation location, Integer radius, Instant since, Instant until, Boolean inactiveToo, 
    		CharitySortBy sortBy, SortDirection sortDir, Integer maxResults, Integer offset) throws BadRequestException {
    	if (sortBy == null) {
    		sortBy = CharitySortBy.NAME;
    	}
    	if (sortDir == null) {
    		sortDir = SortDirection.ASC;
    	}
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Charity> root = cq.from(Charity.class);
        List<Predicate> predicates = new ArrayList<>();
        if (location != null && radius != null) {
            Polygon circle = EllipseHelper.calculateCircle(location.getPoint(), radius);
//            predicates.add(new WithinPredicate(cb, root.get(Charity_.location).get(GeoLocation_.point), circle));
            predicates.add(cb.isTrue(cb.function("st_within", Boolean.class, root.get(Charity_.location).get(GeoLocation_.point), cb.literal(circle))));
        }
        if (since != null) {
	        predicates.add(cb.greaterThanOrEqualTo(root.get(Charity_.campaignStartTime), since));
        }        
        if (until != null) {
	        predicates.add(cb.lessThan(root.get(Charity_.campaignStartTime), until));
        }        
        if (inactiveToo == null || !inactiveToo.booleanValue()) {
	        predicates.add(cb.and(
	        		cb.lessThanOrEqualTo(root.get(Charity_.campaignStartTime), now),
	        		cb.or(cb.isNull(root.get(Charity_.campaignEndTime)), cb.greaterThan(root.get(Charity_.campaignEndTime), now))
	        		));
        }
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (maxResults == 0) {
        	// Only count the objects, order is not relevant
            cq.select(cb.count(root.get(Charity_.id)));
            TypedQuery<Long> tq = em.createQuery(cq);
            totalCount = tq.getSingleResult();
        } else {
        	// Select the objects, order is relevant
            cq.select(root.get(Charity_.id));
            Expression<?> orderExpr = createOrderExpression(cb, root, sortBy, location);
            cq.orderBy((sortDir == SortDirection.ASC) ? cb.asc(orderExpr) : cb.desc(orderExpr)); 
            TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(offset);
			tq.setMaxResults(maxResults);
			results = tq.getResultList();
    	}        	
        return new PagedResult<>(results, maxResults, offset, totalCount);
    }

}