package eu.netmobiel.banker.repository;

import java.time.Instant;
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
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import eu.netmobiel.banker.annotation.BankerDatabase;
import eu.netmobiel.banker.filter.RewardFilter;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.Incentive;
import eu.netmobiel.banker.model.Incentive_;
import eu.netmobiel.banker.model.Reward;
import eu.netmobiel.banker.model.RewardType;
import eu.netmobiel.banker.model.Reward_;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.repository.AbstractDao;

@ApplicationScoped
@Typed(RewardDao.class)
public class RewardDao extends AbstractDao<Reward, Long> {

    @Inject @BankerDatabase
    private EntityManager em;

    public RewardDao() {
		super(Reward.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

	public Optional<Reward> findByFact(Incentive incentive, BankerUser recipient, String factContext) {
		String q = "from Reward rwd where rwd.incentive = :incentive and rwd.recipient = :recipient and rwd.factContext = :factContext";
		TypedQuery<Reward> tq = em.createQuery(q, Reward.class);
		tq.setParameter("incentive", incentive);
		tq.setParameter("recipient", recipient);
		tq.setParameter("factContext", factContext);
		List<Reward> results = tq.getResultList();
		if (results.size() > 1) {
			throw new IllegalStateException(String.format("Multiple rewards with same incentive, recipient and fact context: %s %s %s", 
					incentive.getId(), recipient.getId(), factContext));
		}
		return Optional.ofNullable(results.isEmpty() ? null : results.get(0));
	}

    /**
     * Lists rewards according specific criteria. 
     * @param cursor The position and size of the result set. 
     * @return A list of reward IDs pending payment, in ascending order.
     */
    public PagedResult<Long> listRewards(RewardFilter filter, Cursor cursor) throws BadRequestException {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Reward> root = cq.from(Reward.class);
        List<Predicate> predicates = new ArrayList<>();
        if (filter.getUser() != null) {
            predicates.add(cb.equal(root.get(Reward_.recipient), filter.getUser()));
        }
        if (filter.getSince() != null) {
	        predicates.add(cb.greaterThanOrEqualTo(root.get(Reward_.rewardTime), filter.getSince()));
        }        
        if (filter.getUntil() != null) {
	        predicates.add(cb.lessThan(root.get(Reward_.rewardTime), filter.getUntil()));
        }        
        if (filter.getCancelled() != null) {
        	// Non-cancelled --> no cancel time set
        	Path<Instant> cancelPath = root.get(Reward_.cancelTime);
        	if (Boolean.TRUE.equals(filter.getCancelled())) {
                predicates.add(cb.isNotNull(cancelPath));
        	} else {
                predicates.add(cb.isNull(cancelPath));
        	}
        }
        if (filter.getRewardType() != null) {
        	Path<Boolean> redemptionPath = root.get(Reward_.incentive).get(Incentive_.redemption); 
        	if (filter.getRewardType() == RewardType.PREMIUM) {
                predicates.add(cb.isFalse(redemptionPath));
        	} else {
                predicates.add(cb.isTrue(redemptionPath));
        	}
        }
        if (filter.getPaid() != null) {
        	Path<Boolean> paidOutPath = root.get(Reward_.paidOut);
        	if (Boolean.TRUE.equals(filter.getPaid())) {
                predicates.add(cb.isTrue(paidOutPath));
        	} else {
                predicates.add(cb.isFalse(paidOutPath));
        	}
        }
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (cursor.isCountingQuery()) {
    		cq.select(cb.count(root.get(Reward_.id)));
            totalCount = em.createQuery(cq).getSingleResult();
        } else {
            cq.select(root.get(Reward_.id));
            Expression<Instant> orderExpr = root.get(Reward_.rewardTime);
            cq.orderBy((filter.getSortDir() == SortDirection.ASC) ? cb.asc(orderExpr) : cb.desc(orderExpr)); 
            TypedQuery<Long> tq = em.createQuery(cq);
    		tq.setFirstResult(cursor.getOffset());
    		tq.setMaxResults(cursor.getMaxResults());
			results = tq.getResultList();
        }
        return new PagedResult<>(results, cursor, totalCount);
    }

}
