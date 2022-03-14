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
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import eu.netmobiel.banker.annotation.BankerDatabase;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.Incentive;
import eu.netmobiel.banker.model.Reward;
import eu.netmobiel.banker.model.Reward_;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
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
     * Lists payment pending rewards according specific criteria. 
     * Rewards are removed if withdrawn. Only when the payment is cancelled (only as a test), the cancelTime is set.
     * The automatic payment will pickup the payment-pending rewards with this call.  
     * @param cursor The position and size of the result set. 
     * @return A list of reward IDs pending payment, in ascending order.
     */
    public PagedResult<Long> listPendingRewards(Cursor cursor) throws BadRequestException {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Reward> root = cq.from(Reward.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.or(
        		cb.isNull(root.get(Reward_.transaction)), 
        		cb.isNotNull(root.get(Reward_.cancelTime))
        		));
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (cursor.isCountingQuery()) {
    		cq.select(cb.count(root.get(Reward_.id)));
            totalCount = em.createQuery(cq).getSingleResult();
        } else {
            cq.select(root.get(Reward_.id));
            cq.orderBy(cb.asc(root.get(Reward_.id))); 
            TypedQuery<Long> tq = em.createQuery(cq);
    		tq.setFirstResult(cursor.getOffset());
    		tq.setMaxResults(cursor.getMaxResults());
			results = tq.getResultList();
        }
        return new PagedResult<>(results, cursor, totalCount);
    }

}
