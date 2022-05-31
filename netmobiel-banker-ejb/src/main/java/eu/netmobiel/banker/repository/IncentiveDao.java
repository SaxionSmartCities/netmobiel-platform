package eu.netmobiel.banker.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
import javax.persistence.criteria.Subquery;

import eu.netmobiel.banker.annotation.BankerDatabase;
import eu.netmobiel.banker.filter.IncentiveFilter;
import eu.netmobiel.banker.model.Incentive;
import eu.netmobiel.banker.model.Incentive_;
import eu.netmobiel.banker.model.Reward;
import eu.netmobiel.banker.model.Reward_;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.repository.AbstractDao;

@ApplicationScoped
@Typed(IncentiveDao.class)
public class IncentiveDao extends AbstractDao<Incentive, Long> {

    @Inject @BankerDatabase
    private EntityManager em;

    public IncentiveDao() {
		super(Incentive.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

	public Optional<Incentive> findByCode(String code) {
		String q = "from Incentive inc where inc.code = :code";
		TypedQuery<Incentive> tq = em.createQuery(q, Incentive.class);
		tq.setParameter("code", code);
		List<Incentive> results = tq.getResultList();
		if (results.size() > 1) {
			throw new IllegalStateException("Multiple incentives with same code: " + code);
		}
		return Optional.ofNullable(results.isEmpty() ? null : results.get(0));
	}
	
    /**
     * Lists incentives according specific criteria. 
     * @param cursor The position and size of the result set. 
     * @return A list of incentive IDs in descending order.
     */
    public PagedResult<Long> listIncentives(boolean disabledToo, Cursor cursor) throws BadRequestException {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Incentive> root = cq.from(Incentive.class);
        List<Predicate> predicates = new ArrayList<>();
        if (!disabledToo) {
        	// Non-disabled --> no disable time set
	        predicates.add(cb.isNull(root.get(Incentive_.disableTime)));
        }
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (cursor.isCountingQuery()) {
    		cq.select(cb.count(root.get(Incentive_.id)));
            totalCount = em.createQuery(cq).getSingleResult();
        } else {
            cq.select(root.get(Incentive_.id));
            cq.orderBy(cb.desc(root.get(Incentive_.id))); 
            TypedQuery<Long> tq = em.createQuery(cq);
    		tq.setFirstResult(cursor.getOffset());
    		tq.setMaxResults(cursor.getMaxResults());
			results = tq.getResultList();
        }
        return new PagedResult<>(results, cursor, totalCount);
    }
    

    /**
     * List the incentives that need to be brought into attention to the specified user.
     * Ignore incentives that are disabled, unless specified.
     * @param filter the filter to apply
     * @param cursor the cursor to apply
     * @return A list of incentive ids complying to the criteria.
     * @throws BadRequestException
     */
    public PagedResult<Long> listCallToActions(IncentiveFilter filter, Cursor cursor) throws BadRequestException {
    	/*
	    	select inc.* from incentive inc where inc.cta_enabled and inc.disable_time is null and and not exists 
		    	(select 1 from reward r
		    	 join bn_user u on u.id = r.recipient
		    	 where r.incentive = inc.id and u.email = 'passagier-acc@netmobiel.eu' and r.cancel_time is null
		    	 group by r.incentive
		    	 having count(*) > inc.cta_hide_beyond_reward_count
		    	)
	    	order by inc.id asc;
		*/
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Incentive> root = cq.from(Incentive.class);
        List<Predicate> predicates = new ArrayList<>();
        // Only the CTA incentives
    	predicates.add(cb.isTrue(root.get(Incentive_.ctaEnabled)));
        if (!filter.isDisabledToo()) {
        	predicates.add(cb.isNull(root.get(Incentive_.disableTime)));
        }
        // Retrieve the CTA incentives that match the criteria (and this do not need attention anymore)
        // For a specific user and do not count the cancelled rewards
        Subquery<Incentive> sq = cq.subquery(Incentive.class);
        Root<Reward> sr = sq.from(Reward.class);
        sq.where(
        	cb.equal(sr.get(Reward_.incentive), root), 
        	cb.equal(sr.get(Reward_.recipient), filter.getUser()),
        	cb.isNull(sr.get(Reward_.cancelTime))
        );
        sq.groupBy(sr.get(Reward_.incentive));
        sq.having(cb.greaterThan(cb.count(sr).as(Integer.class), root.get(Incentive_.ctaHideBeyondRewardCount)));
        sq.select(sr.get(Reward_.incentive)).distinct(true);
    	predicates.add(cb.not(cb.exists(sq)));
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));

        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (cursor.isCountingQuery()) {
    		cq.select(cb.count(root.get(Incentive_.id)));
            totalCount = em.createQuery(cq).getSingleResult();
        } else {
            cq.select(root.get(Incentive_.id));
            Expression<Long> orderExpr = root.get(Incentive_.id);
            cq.orderBy((filter.getSortDir() == SortDirection.ASC) ? cb.asc(orderExpr) : cb.desc(orderExpr)); 
            TypedQuery<Long> tq = em.createQuery(cq);
    		tq.setFirstResult(cursor.getOffset());
    		tq.setMaxResults(cursor.getMaxResults());
			results = tq.getResultList();
        }
        return new PagedResult<>(results, cursor, totalCount);
    }
}
