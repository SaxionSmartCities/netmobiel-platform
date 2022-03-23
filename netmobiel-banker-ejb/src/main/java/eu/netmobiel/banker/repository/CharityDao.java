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
import eu.netmobiel.banker.filter.CharityFilter;
import eu.netmobiel.banker.model.Charity;
import eu.netmobiel.banker.model.CharitySortBy;
import eu.netmobiel.banker.model.Charity_;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.filter.Cursor;
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

	private static Expression<?> createOrderExpression(CriteriaBuilder cb, Root<Charity> root, CharitySortBy sortBy, GeoLocation location) throws BadRequestException {
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
     * @param now the reference time. Must be set.
	 * @param filter the filter parameters
	 * @param cursor the cursor  
     * @return A list of charities matching the criteria.
     */
    public PagedResult<Long> findCharities(Instant now, CharityFilter filter, Cursor cursor) throws BadRequestException {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Charity> root = cq.from(Charity.class);
        List<Predicate> predicates = new ArrayList<>();
        if (filter.getLocation() != null && filter.getRadius() != null) {
            Polygon circle = EllipseHelper.calculateCircle(filter.getLocation().getPoint(), filter.getRadius());
//            predicates.add(new WithinPredicate(cb, root.get(Charity_.location).get(GeoLocation_.point), circle));
            predicates.add(cb.isTrue(cb.function("st_within", Boolean.class, root.get(Charity_.location).get(GeoLocation_.point), cb.literal(circle))));
        }
        if (filter.getSince() != null) {
	        predicates.add(cb.greaterThanOrEqualTo(root.get(Charity_.campaignStartTime), filter.getSince()));
        }        
        if (filter.getUntil() != null) {
	        predicates.add(cb.lessThan(root.get(Charity_.campaignStartTime), filter.getUntil()));
        }        
        if (!filter.isInactiveToo()) {
	        predicates.add(cb.and(
	        		cb.lessThanOrEqualTo(root.get(Charity_.campaignStartTime), now),
	        		cb.or(cb.isNull(root.get(Charity_.campaignEndTime)), cb.greaterThan(root.get(Charity_.campaignEndTime), now))
	        		));
        }
        if (!filter.isDeletedToo()) {
	        predicates.add(cb.isFalse(root.get(Charity_.deleted)));
        }
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (cursor.isCountingQuery()) {
        	// Only count the objects, order is not relevant
            cq.select(cb.count(root.get(Charity_.id)));
            TypedQuery<Long> tq = em.createQuery(cq);
            totalCount = tq.getSingleResult();
        } else {
        	// Select the objects, order is relevant
            cq.select(root.get(Charity_.id));
            Expression<?> orderExpr = createOrderExpression(cb, root, filter.getSortBy(), filter.getLocation());
            cq.orderBy((filter.getSortDir() == SortDirection.ASC) ? cb.asc(orderExpr) : cb.desc(orderExpr)); 
            TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(cursor.getOffset());
			tq.setMaxResults(cursor.getMaxResults());
			results = tq.getResultList();
    	}        	
        return new PagedResult<>(results, cursor, totalCount);
    }

}