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
import eu.netmobiel.banker.model.Incentive;
import eu.netmobiel.banker.model.Incentive_;
import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
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

}
