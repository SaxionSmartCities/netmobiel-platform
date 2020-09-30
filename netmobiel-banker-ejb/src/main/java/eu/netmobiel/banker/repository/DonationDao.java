package eu.netmobiel.banker.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CompoundSelection;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.vividsolutions.jts.geom.Polygon;

import eu.netmobiel.banker.annotation.BankerDatabase;
import eu.netmobiel.banker.filter.DonationFilter;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.BankerUser_;
import eu.netmobiel.banker.model.Charity;
import eu.netmobiel.banker.model.Charity_;
import eu.netmobiel.banker.model.Donation;
import eu.netmobiel.banker.model.DonationGroupBy;
import eu.netmobiel.banker.model.DonationSortBy;
import eu.netmobiel.banker.model.Donation_;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.model.GeoLocation;
import eu.netmobiel.commons.model.GeoLocation_;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.commons.util.EllipseHelper;

@ApplicationScoped
@Typed(DonationDao.class)
public class DonationDao extends AbstractDao<Donation, Long> {

    @Inject @BankerDatabase
    private EntityManager em;

    public DonationDao() {
		super(Donation.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

    /**
     * Lists donations according specific criteria. 
     * @param now the reference time. Especially needed for testing.
     * @param charity list donations to a specific charity.
     * @param user list donations from a specific user.
     * @param since only lists charities that start campaigning after this date.
     * @param until limit the search to charities having started campaigning before this date.
     * @param inactiveToo also list donations to charities that are no longer campaigning 
     * @param sortBy which sort key to use. Default is by name.
     * @param sortDir which sort direction. Default is ascending.
     * @param maxResults For paging: maximum results.
     * @param offset For paging: the offset in the results to return.
     * @return A list of donation IDs matching the criteria.
     */
    public PagedResult<Long> listDonations(Instant now, Charity charity, BankerUser user, Instant since, Instant until, Boolean inactiveToo, 
    		DonationSortBy sortBy, SortDirection sortDir, Integer maxResults, Integer offset) throws BadRequestException {
    	if (sortBy == null) {
    		throw new BadRequestException("Specify a sort direction");
    	}
    	if (sortDir == null) {
    		sortDir = SortDirection.DESC;
    	}
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Donation> root = cq.from(Donation.class);
        List<Predicate> predicates = new ArrayList<>();
        if (charity != null) {
	        predicates.add(cb.equal(root.get(Donation_.charity), charity));
        }        
        if (user != null) {
	        predicates.add(cb.equal(root.get(Donation_.user), user));
        }        
        if (since != null) {
	        predicates.add(cb.greaterThanOrEqualTo(root.get(Donation_.donationTime), since));
        }        
        if (until != null) {
	        predicates.add(cb.lessThan(root.get(Donation_.donationTime), until));
        }        
        if (inactiveToo == null || !inactiveToo.booleanValue()) {
	        predicates.add(cb.and(
	        		cb.lessThanOrEqualTo(root.get(Donation_.charity).get(Charity_.campaignStartTime), now),
	        		cb.or(cb.isNull(root.get(Donation_.charity).get(Charity_.campaignEndTime)), 
	        			  cb.greaterThan(root.get(Donation_.charity).get(Charity_.campaignEndTime), now))
	        		));
        }
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (maxResults == 0) {
        	// Only count the objects, order is not relevant
            cq.select(cb.count(root.get(Donation_.id)));
            TypedQuery<Long> tq = em.createQuery(cq);
            totalCount = tq.getSingleResult();
        } else {
        	// Select the objects, order is relevant
            cq.select(root.get(Donation_.id));
            Expression<?> orderExpr = null;
            if (sortBy == DonationSortBy.AMOUNT) {
            	orderExpr = root.get(Donation_.amount);
            } else if (sortBy == DonationSortBy.DATE) {
            	orderExpr = root.get(Donation_.donationTime);
            } else {
            	throw new IllegalStateException("Sort by not supported: " + sortBy);
            }
            cq.orderBy((sortDir == SortDirection.ASC) ? cb.asc(orderExpr) : cb.desc(orderExpr)); 
            TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(offset);
			tq.setMaxResults(maxResults);
			results = tq.getResultList();
    	}        	
        return new PagedResult<Long>(results, maxResults, offset, totalCount);
    }

    private Predicate createPredicate(CriteriaBuilder cb, Root<Donation> root, DonationFilter filter) {
        List<Predicate> predicates = new ArrayList<>();
        if (filter.getCharity() != null) {
	        predicates.add(cb.equal(root.get(Donation_.charity), filter.getCharity()));
        }        
        if (filter.getUser() != null) {
	        predicates.add(cb.equal(root.get(Donation_.user), filter.getUser()));
        }        
        if (filter.getLocation() != null && filter.getRadius() != null) {
            Polygon circle = EllipseHelper.calculateCircle(filter.getLocation().getPoint(), filter.getRadius());
            predicates.add(cb.isTrue(cb.function("st_within", Boolean.class, 
            		root.get(Donation_.charity).get(Charity_.location).get(GeoLocation_.point), cb.literal(circle))));
        }
        if (filter.getSince() != null) {
	        predicates.add(cb.greaterThanOrEqualTo(root.get(Donation_.donationTime), filter.getSince()));
        }        
        if (filter.getUntil() != null) {
	        predicates.add(cb.lessThan(root.get(Donation_.donationTime), filter.getUntil()));
        }
        if (filter.getInactiveToo() == null || !filter.getInactiveToo().booleanValue()) {
	        predicates.add(cb.and(
	        		cb.lessThanOrEqualTo(root.get(Donation_.charity).get(Charity_.campaignStartTime), filter.getNow()),
	        		cb.or(cb.isNull(root.get(Donation_.charity).get(Charity_.campaignEndTime)), 
	        			  cb.greaterThan(root.get(Donation_.charity).get(Charity_.campaignEndTime), filter.getNow()))
	        		));
        }
    	return cb.and(predicates.toArray(new Predicate[predicates.size()])); 
    };

    /*
What can we report on? 
- Amount donated (sum) per user, per charity. Total # of records: # charities x # users (for each charity)
- Amount donated (sum) in total per charity. Total # of records: # charities
- Amount donated (sum) in total per user. Total # of records: # donors --> Popularity of the charity system
- For a specific charity: # of donors --> popularity
- For a specific user: # of charities donated to 

Charity Overview, see:
https://projects.invisionapp.com/d/main#/console/17160442/389360057/preview
 
-- Populair in de buurt: Count distinct users per charity ordered by descending donor count and by
-- charity id descending (younger charities are prioritized)
select d.charity, count(distinct d.bn_user) from donation d where d.anonymous = false 
 group by d.charity order by  count(distinct d.bn_user) desc
; 

-- Donated before: List of latest donations by a donor to each charity ordered by donation date descending
select distinct d.* from donation d 
	where d.bn_user = 53 and (d.charity, d.donation_time) in 
		(select dd.charity, max(dd.donation_time) from donation dd
 			where dd.bn_user = 53 group by dd.charity
 	) order by d.donation_time desc
 
 -- Donated in total to any charity ordered by total amount donated descending
select d.bn_user, sum(d.amount) from donation d where d.anonymous = false 
	group by bn_user order by sum(amount) desc
;

 Specific charity:
 https://projects.invisionapp.com/d/main#/console/17160442/389360056/preview
 -- List top N donors for a specific charity, ordered by donated summed amount descending 
-- and user id descending (users that joined more recently are prioritized)
select d.bn_user, sum(d.amount) from donation d 
	where d.charity = 53 and d.anonymous = false group by bn_user order by sum(d.amount) desc, d.bn_user desc
;

     */
    /**
     * Report on donations using a groupBy criterium. Suppported are group by charity, user or both:
     * Group by Charity: Get the total amount of donations to each charity after filtering.
     * Group by User: Get the total amount of donations of each user after filtering. 
     * @param now the reference time. Especially needed for testing.
     * @param charity filter by a specific charity.
     * @param user filter by a specific user.
     * @param location the reference location as the center point to search for charities.
     * @param radius the circle radius in meter.
     * @param since only lists charities that start campaigning after this date.
     * @param until limit the search to charities having started campaigning before this date.
     * @param inactiveToo also finds charities that are no longer campaigning.
     * @param sortBy which sort key to use. Default is by name.
     * @param sortDir which sort direction. Default is ascending.
     * @param maxResults For paging: maximum results.
     * @param offset For paging: the offset in the results to return.
     * @return A list of charities matching the filter criteria and the grouping selection.
     */
    public PagedResult<Donation> reportCharities(Instant now, Charity charity, BankerUser user, GeoLocation location, Integer radius, 
    		Instant since, Instant until, Boolean inactiveToo, DonationGroupBy groupBy, 
    		DonationSortBy sortBy, SortDirection sortDir, Integer maxResults, Integer offset) throws BadRequestException {
    	if (groupBy == null) {
    		throw new BadRequestException("Specify a grouping value");
    	}
    	if (sortBy == null) {
    		sortBy = DonationSortBy.AMOUNT;
    	}
    	if (sortBy != DonationSortBy.AMOUNT) {
    		throw new BadRequestException("Only sort by amount is supported");
    	}
    	if (sortDir == null) {
    		sortDir = SortDirection.DESC;
    	}
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        Function<Root<Donation>, Predicate> createFilter = root -> {
            List<Predicate> predicates = new ArrayList<>();
            if (charity != null) {
    	        predicates.add(cb.equal(root.get(Donation_.charity), charity));
            }        
            if (user != null) {
    	        predicates.add(cb.equal(root.get(Donation_.user), user));
            }        
            if (location != null && radius != null) {
                Polygon circle = EllipseHelper.calculateCircle(location.getPoint(), radius);
                predicates.add(cb.isTrue(cb.function("st_within", Boolean.class, root.get(Donation_.charity).get(Charity_.location).get(GeoLocation_.point), cb.literal(circle))));
            }
            if (since != null) {
    	        predicates.add(cb.greaterThanOrEqualTo(root.get(Donation_.donationTime), since));
            }        
            if (until != null) {
    	        predicates.add(cb.lessThan(root.get(Donation_.donationTime), until));
            }
        	return cb.and(predicates.toArray(new Predicate[predicates.size()])); 
        };
        Function<Root<Donation>, List<Expression<?>>> groupby = root -> {
        	List<Expression<?>> gb = new ArrayList<>(); 
        	if (groupBy == DonationGroupBy.CHARITY) {
                gb.add(root.get(Donation_.charity));
            } else if (groupBy == DonationGroupBy.USER) {
            	gb.add(root.get(Donation_.user));
            } else if (groupBy == DonationGroupBy.CHARITY_AND_USER) {
                gb.add(root.get(Donation_.charity));
            	gb.add(root.get(Donation_.user));
            } else {
            	throw new IllegalStateException("Group by not supported: " + groupBy);
        	}
        	return gb;
        };
        Function<Root<Donation>, CompoundSelection<Donation>> select = root -> {
        	CompoundSelection<Donation> cs; 
        	if (groupBy == DonationGroupBy.CHARITY) {
        		cs = cb.construct(Donation.class, root.get(Donation_.charity), cb.sum(root.get(Donation_.amount)));
            } else if (groupBy == DonationGroupBy.USER) {
        		cs = cb.construct(Donation.class, root.get(Donation_.user), cb.sum(root.get(Donation_.amount)));
            } else if (groupBy == DonationGroupBy.CHARITY_AND_USER) {
        		cs = cb.construct(Donation.class, root.get(Donation_.charity), root.get(Donation_.user), cb.sum(root.get(Donation_.amount)));
            } else {
            	throw new IllegalStateException("Group by not supported: " + groupBy);
        	}
        	return cs;
        };

        if (maxResults == 0) {
            Long totalCount = null;
            List<Long> results = Collections.emptyList();
        	// Only count the objects, order is not relevant
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<Donation> root = cq.from(Donation.class);
            cq.where(createFilter.apply(root));
            cq.groupBy(groupby.apply(root));
            cq.select(cb.count(root));
            TypedQuery<Long> tq = em.createQuery(cq);
            totalCount = tq.getSingleResult();
        } else {
        	// Select the objects, order is relevant
            CriteriaQuery<Donation> cq = cb.createQuery(Donation.class);
            Root<Donation> root = cq.from(Donation.class);
            cq.where(createFilter.apply(root));
            cq.groupBy(groupby.apply(root));
        	cq.select(select.apply(root));
            Expression<?> orderExpr = null;
            if (sortBy == DonationSortBy.AMOUNT) {
            	orderExpr = cb.sum(root.get(Donation_.amount));
            } else {
            	throw new IllegalStateException("Sort by not supported: " + sortBy);
            }
            cq.orderBy((sortDir == SortDirection.ASC) ? cb.asc(orderExpr) : cb.desc(orderExpr)); 
            TypedQuery<Donation> tq = em.createQuery(cq);
			tq.setFirstResult(offset);
			tq.setMaxResults(maxResults);
			List<Donation> results = tq.getResultList();
    	}        	
//        return new PagedResult<Long>(results, maxResults, offset, totalCount);
        return null;
    }
/*
-- Popular nearby: Count distinct users per charity ordered by descending donor count and by
-- charity id descending (younger charities are prioritized)
select d.charity, count(distinct d.bn_user) from donation d where d.anonymous = false 
 group by d.charity order by  count(distinct d.bn_user) desc
; 
*/
    public List<Donation> reportCharityPopularityTopN(DonationFilter filter, Integer maxResults) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
    	CriteriaQuery<Donation> cq = cb.createQuery(Donation.class);
    	Root<Donation> root = cq.from(Donation.class);
//    	root.fetch(Donation_.charity, JoinType.INNER);
    	cq.where(cb.and(createPredicate(cb, root, filter), cb.isFalse(root.get(Donation_.anonymous))));
    	cq.groupBy(root.get(Donation_.charity));
    	Expression<?> userCountExpr = cb.countDistinct(root.get(Donation_.user));
    	cq.select(cb.construct(Donation.class, root.get(Donation_.charity), userCountExpr));
        cq.orderBy(filter.getSortDir() == SortDirection.ASC ? cb.asc(userCountExpr) : cb.desc(userCountExpr),
        		cb.desc(root.get(Donation_.charity).get(Charity_.id))); 
        TypedQuery<Donation> tq = em.createQuery(cq);	
		tq.setMaxResults(maxResults);
//		tq.setHint(JPA_HINT_FETCH, getEntityGraph(Donation.REPORT_TOP_N_CHARITY));
		// We cannot fetch only the charity. An error message is returned:
		// java.lang.IllegalArgumentException: org.hibernate.QueryException: query specified join fetching, but the owner of the fetched association was not present in the select list 
		List<Donation> results = tq.getResultList();
		// The object returned contains proxies for charity and user. The service has to fill in the desired graphs!
		return results;
    }
/*    
    -- Donated before: List of latest donations by a donor to each charity ordered by donation date descending
    select distinct d.* from donation d 
    	where d.bn_user = 53 and (d.charity, d.donation_time) in 
    		(select dd.charity, max(dd.donation_time) from donation dd
     			where dd.bn_user = 53 group by dd.charity
     	) order by d.donation_time desc
     
     -- Donated in total to any charity ordered by total amount donated descending
    select d.bn_user, sum(d.amount) from donation d where d.anonymous = false 
    	group by bn_user order by sum(amount) desc
    ;
*/
    public List<Long> reportMostRecentDistinctDonations(BankerUser user, Integer maxResults) {
		TypedQuery<Long> tq = em.createQuery("select d.id from Donation d " +
		    	"where d.user = :user and (d.charity, d.donationTime) in " +
	    		"(select dd.charity, max(dd.donationTime) from Donation dd " +
	     		" where dd.user = :user group by dd.charity) " + 
	     		"order by d.donationTime desc", Long.class);
		tq.setParameter("user", user);
		tq.setMaxResults(maxResults);
		List<Long> results = tq.getResultList();
		return results;
    }
/*    
    -- Donated in total to any charity ordered by total amount donated descending
    select d.bn_user, sum(d.amount) from donation d where d.anonymous = false 
    	group by bn_user order by sum(amount) desc
    ;
*/
    public List<Donation> reportDonorGenerousityTopN(DonationFilter filter, Integer maxResults) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
    	CriteriaQuery<Donation> cq = cb.createQuery(Donation.class);
    	Root<Donation> root = cq.from(Donation.class);
    	cq.where(cb.and(createPredicate(cb, root, filter), cb.isFalse(root.get(Donation_.anonymous))));
    	cq.groupBy(root.get(Donation_.user));
    	Expression<?> amountSummedExpr = cb.sum(root.get(Donation_.amount));
    	cq.select(cb.construct(Donation.class, root.get(Donation_.user), amountSummedExpr));
        cq.orderBy(filter.getSortDir() == SortDirection.ASC ? cb.asc(amountSummedExpr) : cb.desc(amountSummedExpr), 
        		cb.desc(root.get(Donation_.user).get(BankerUser_.id))); 
        TypedQuery<Donation> tq = em.createQuery(cq);
		tq.setMaxResults(maxResults);
		List<Donation> results = tq.getResultList();
		// The object returned contains proxies for charity and user. The service has to fill in the desired graphs!
		return results;
    }

    /*
    Specific charity:
    	 https://projects.invisionapp.com/d/main#/console/17160442/389360056/preview
    	-- List top N donors for a specific charity, ordered by donated summed amount descending 
    	-- and user id descending (users that joined more recently are prioritized)
    	select d.bn_user, sum(d.amount) from donation d 
    		where d.charity = 53 and d.anonymous = false group by bn_user order by sum(d.amount) desc, d.bn_user desc
    	;
     */
    
    // Hmmmm, can be realized with method above

}
