package eu.netmobiel.banker.repository;

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

import com.vividsolutions.jts.geom.Polygon;

import eu.netmobiel.banker.annotation.BankerDatabase;
import eu.netmobiel.banker.filter.DonationFilter;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.BankerUser_;
import eu.netmobiel.banker.model.Charity_;
import eu.netmobiel.banker.model.Donation;
import eu.netmobiel.banker.model.DonationSortBy;
import eu.netmobiel.banker.model.Donation_;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.filter.Cursor;
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
     * @param filter The donation selection and sorting criteria.
     * @param cursor The position and size of the result set. 
     * @return A list of donation IDs matching the criteria.
     */
    public PagedResult<Long> listDonations(DonationFilter filter, Cursor cursor) throws BadRequestException {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Donation> root = cq.from(Donation.class);
    	cq.where(createPredicate(cb, root, filter));
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        cq.select(root.get(Donation_.id));
        Expression<?> orderExpr = null;
        if (filter.getSortBy() == DonationSortBy.AMOUNT) {
        	orderExpr = root.get(Donation_.amount);
        } else if (filter.getSortBy() == DonationSortBy.DATE) {
        	orderExpr = root.get(Donation_.donationTime);
        } else {
        	throw new IllegalStateException("Sort by not supported: " + filter.getSortBy());
        }
        cq.orderBy((filter.getSortDir() == SortDirection.ASC) ? cb.asc(orderExpr) : cb.desc(orderExpr)); 
        if (!cursor.isCountingQuery()) {
            TypedQuery<Long> tq = em.createQuery(cq);
    		tq.setFirstResult(cursor.getOffset());
    		tq.setMaxResults(cursor.getMaxResults());
			results = tq.getResultList();
        }
        totalCount = count(cb, cq, root);
        return new PagedResult<Long>(results, cursor, totalCount);
    }

    /**
     * Transforms the filter into a predicate for the 'where'clause. When an charity is defined as filter, 
     * then the charity selection attributeds location and omitInactiveCharities will be ignored.
     * @param cb The criteria builder.
     * @param root The root object.
     * @param filter the donation selection criteria.
     * @return A predicate comprising all other predicates. Default true.
     */
    private Predicate createPredicate(CriteriaBuilder cb, Root<Donation> root, DonationFilter filter) {
        List<Predicate> predicates = new ArrayList<>();
        if (filter.getCharity() != null) {
	        predicates.add(cb.equal(root.get(Donation_.charity), filter.getCharity()));
        } else {
            if (filter.getLocation() != null && filter.getRadius() != null) {
                Polygon circle = EllipseHelper.calculateCircle(filter.getLocation().getPoint(), filter.getRadius());
                predicates.add(cb.isTrue(cb.function("st_within", Boolean.class, 
                		root.get(Donation_.charity).get(Charity_.location).get(GeoLocation_.point), cb.literal(circle))));
            }
            if (filter.isOmitInactiveCharities()) {
    	        predicates.add(cb.and(
    	        		cb.lessThanOrEqualTo(root.get(Donation_.charity).get(Charity_.campaignStartTime), filter.getNow()),
    	        		cb.or(cb.isNull(root.get(Donation_.charity).get(Charity_.campaignEndTime)), 
    	        			  cb.greaterThan(root.get(Donation_.charity).get(Charity_.campaignEndTime), filter.getNow()))
    	        		));
            }
        }
        if (filter.getSince() != null) {
	        predicates.add(cb.greaterThanOrEqualTo(root.get(Donation_.donationTime), filter.getSince()));
        }        
        if (filter.getUntil() != null) {
	        predicates.add(cb.lessThan(root.get(Donation_.donationTime), filter.getUntil()));
        }
        if (filter.getUser() != null) {
	        predicates.add(cb.equal(root.get(Donation_.user), filter.getUser()));
        }        
        if (! filter.isAnonymousToo()) {
	        predicates.add(cb.isFalse(root.get(Donation_.anonymous)));
        }        
    	return cb.and(predicates.toArray(new Predicate[predicates.size()])); 
    };

    public static class CharityPopularity {
    	public long charityId;
    	public long donorCount;
    	
    	public CharityPopularity(long aCharityId, long aDonorCount) {
    		this.charityId = aCharityId;
    		this.donorCount = aDonorCount;
    	}
    }
    
    /**
     * Retrieves the popular charities according some filter: Count distinct users per charity ordered by 
     * descending donor count and by charity id descending (younger charities are prioritized).
     * @param filter The donation selection and sorting criteria. Anonymous donations are never included.
     * @param cursor The position and size of the result set. 
     * @return A list of CharityPopularity objects matching the criteria.
     */
    public PagedResult<CharityPopularity> reportCharityPopularityTopN(DonationFilter filter, Cursor cursor) {
    	/**
    	 * select d.charity, count(distinct d.bn_user) from donation d where d.anonymous = false
    	 * group by d.charity order by  count(distinct d.bn_user) desc
    	 * 
    	 * Counting query:
    	 * select count(distinct d.charity) from donation d where d.anonymous = false
     	 */
    	filter.setAnonymousToo(false);
    	CriteriaBuilder cb = em.getCriteriaBuilder();
    	CriteriaQuery<CharityPopularity> cq = cb.createQuery(CharityPopularity.class);
    	Root<Donation> root = cq.from(Donation.class);
    	cq.where(cb.and(createPredicate(cb, root, filter)));
    	cq.groupBy(root.get(Donation_.charity));
    	Expression<?> userCountExpr = cb.countDistinct(root.get(Donation_.user));
    	cq.select(cb.construct(CharityPopularity.class, root.get(Donation_.charity).get(Charity_.id), userCountExpr));
        cq.orderBy(filter.getSortDir() == SortDirection.ASC ? cb.asc(userCountExpr) : cb.desc(userCountExpr),
        		cb.desc(root.get(Donation_.charity).get(Charity_.id))); 

        List<CharityPopularity> results = Collections.emptyList();
    	if (!cursor.isCountingQuery()) {
            TypedQuery<CharityPopularity> tq = em.createQuery(cq);	
    		tq.setFirstResult(cursor.getOffset());
    		tq.setMaxResults(cursor.getMaxResults());
    		results = tq.getResultList();
    	}
    	Long totalCount = countDistinct(cb,  cq, root, root.get(Donation_.charity));
        return new PagedResult<CharityPopularity>(results, cursor, totalCount);
    }
    
    /**
     * Donated before: List of latest donations by a donor to each charity ordered by donation date descending
     * @param user The user to select the latest (distinct) donations for. 
     * @param cursor The position and size of the result set.
     * @return A list of donation ids.
     */
    public PagedResult<Long> reportMostRecentDistinctDonations(BankerUser user, Cursor cursor) {
    	/**
    	 * select d.* from donation d 
    	 * where d.bn_user = 53 and (d.charity, d.donation_time) in 
    	 *  (select dd.charity, max(dd.donation_time) from donation dd
     	 *   where dd.bn_user = 53 group by dd.charity
     	 *  ) order by d.donation_time desc
		 *
    	 */
		String basicQuery = 
				"from Donation d " +
		    	"where d.user = :user and (d.charity, d.donationTime) in " +
	    		"(select dd.charity, max(dd.donationTime) from Donation dd " +
	     		" where dd.user = :user group by dd.charity) "; 
		Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (! cursor.isCountingQuery()) {
    		TypedQuery<Long> sq = em.createQuery("select d.id " + basicQuery + " order by d.donationTime desc", Long.class);
    		sq.setParameter("user", user);
    		sq.setFirstResult(cursor.getOffset());
    		sq.setMaxResults(cursor.getMaxResults());
    		results = sq.getResultList();
        }
		TypedQuery<Long> tq = em.createQuery("select count(d) " + basicQuery, Long.class);
		tq.setParameter("user", user);
        totalCount = tq.getSingleResult();
        return new PagedResult<Long>(results, cursor, totalCount);
    }

    public static class DonorGenerosity {
    	public long donorId;
    	public long amount;
    	
    	public DonorGenerosity(long aDonorId, long anAmount) {
    		this.donorId = aDonorId;
    		this.amount = anAmount;
    	}
    }

    /**
     * Retrieves the total amount donated in to any charity ordered by total amount donated descending 
     * and by user id descending (late adopters are prioritized). If the totals per charity are needed, then specify
     * a charity as filter criterium.
     * @param filter The donation selection and sorting criteria. Anonymous donations are never included.
     * @param cursor The position and size of the result set. 
     * @return A list of DonorGenerosity objects matching the criteria.
     */
    public PagedResult<DonorGenerosity> reportDonorGenerosityTopN(DonationFilter filter, Cursor cursor) {
    	/**
    	 * select d.bn_user, sum(d.amount) from donation d where d.anonymous = false 
    	 * group by bn_user order by sum(amount) desc
    	 *
    	 * Counting query:
    	 * select count(distinct d.bn_user) from donation d where d.anonymous = false 
    	 */
    	filter.setAnonymousToo(false);
    	CriteriaBuilder cb = em.getCriteriaBuilder();
    	CriteriaQuery<DonorGenerosity> cq = cb.createQuery(DonorGenerosity.class);
    	Root<Donation> root = cq.from(Donation.class);
    	cq.where(cb.and(createPredicate(cb, root, filter)));
    	cq.groupBy(root.get(Donation_.user));
    	Expression<?> amountSummedExpr = cb.sum(root.get(Donation_.amount));
    	cq.select(cb.construct(DonorGenerosity.class, root.get(Donation_.user).get(BankerUser_.id), amountSummedExpr));
        cq.orderBy(filter.getSortDir() == SortDirection.ASC ? cb.asc(amountSummedExpr) : cb.desc(amountSummedExpr), 
        		cb.desc(root.get(Donation_.user).get(BankerUser_.id))); 

        List<DonorGenerosity> results = Collections.emptyList();
    	if (!cursor.isCountingQuery()) {
            TypedQuery<DonorGenerosity> tq = em.createQuery(cq);	
    		tq.setFirstResult(cursor.getOffset());
    		tq.setMaxResults(cursor.getMaxResults());
    		results = tq.getResultList();
    	}
    	Long totalCount = countDistinct(cb, cq, root, root.get(Donation_.user));
        return new PagedResult<DonorGenerosity>(results, cursor, totalCount);
    }

}
