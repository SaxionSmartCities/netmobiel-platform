package eu.netmobiel.profile.repository;

import java.util.ArrayList;
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

import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.profile.annotation.ProfileDatabase;
import eu.netmobiel.profile.filter.DelegationFilter;
import eu.netmobiel.profile.model.Delegation;
import eu.netmobiel.profile.model.Delegation_;
import eu.netmobiel.profile.model.Profile;


@ApplicationScoped
@Typed(DelegationDao.class)
public class DelegationDao extends AbstractDao<Delegation, Long> {

	@Inject @ProfileDatabase
    private EntityManager em;

	@SuppressWarnings("unused")
	@Inject
	private Logger logger;
	
    public DelegationDao() {
		super(Delegation.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

	/**
	 * Retrieves the Delegations according the search criteria.
	 * @param filter the filter criteria. 
	 * @param cursor The cursor to use.
	 * @return A pages result. Total count is determined only when maxResults is set to 0.
	 */
	public PagedResult<Long> listDelegations(DelegationFilter filter, Cursor cursor) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Delegation> root = cq.from(Delegation.class);
        List<Predicate> predicates = new ArrayList<>();
        if (filter.getSince() != null) {
	        predicates.add(cb.greaterThanOrEqualTo(root.get(Delegation_.submissionTime), filter.getSince()));
        }        
        if (filter.getUntil() != null) {
	        predicates.add(cb.lessThanOrEqualTo(root.get(Delegation_.submissionTime), filter.getUntil()));
        }        
        if (filter.getDelegate() != null) {
        	predicates.add(cb.equal(root.get(Delegation_.delegate), filter.getDelegate()));
        }
        if (filter.getDelegator() != null) {
        	predicates.add(cb.equal(root.get(Delegation_.delegator), filter.getDelegator()));
        }
        if (!filter.isInactiveToo()) {
        	// Active delegation has no revocation time, or an revocation time beyond now
        	predicates.add(cb.or(cb.isNull(root.get(Delegation_.revocationTime)), cb.greaterThan(root.get(Delegation_.revocationTime), filter.getNow())));
        }
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = null;
        if (cursor.isCountingQuery()) {
          cq.select(cb.count(root.get(Delegation_.id)));
          totalCount = em.createQuery(cq).getSingleResult();
        } else {
	        cq.select(root.get(Delegation_.id));
	        Expression<?> sortBy = root.get(Delegation_.id);
	        cq.orderBy(filter.getSortDir() == SortDirection.ASC ? cb.asc(sortBy) : cb.desc(sortBy));
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(cursor.getOffset());
			tq.setMaxResults(cursor.getMaxResults());
			results = tq.getResultList();
        }
        return new PagedResult<Long>(results, cursor, totalCount);
	}

	/**
	 * Checks whether a delegation is active between two parties.
	 * @param delegate
	 * @param delegator
	 * @return
	 */
	public boolean isDelegationActive(Profile delegate, Profile delegator) {
    	Long count = em.createQuery("select count(d) from Delegation d where d.revocationTime is null and " 
				+ "d.delegate = :delegate and d.delegator = :delegator", Long.class)
			.setParameter("delegate", delegate)
			.setParameter("delegator", delegator)
			.getSingleResult();
    	return count > 0; 
	}
}
