package eu.netmobiel.profile.repository;

import java.util.ArrayList;
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

import org.slf4j.Logger;

import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.model.User_;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.profile.annotation.ProfileDatabase;
import eu.netmobiel.profile.filter.ComplimentsFilter;
import eu.netmobiel.profile.model.Compliments;
import eu.netmobiel.profile.model.Compliments_;


@ApplicationScoped
@Typed(ComplimentsDao.class)
public class ComplimentsDao extends AbstractDao<Compliments, Long> {

	@Inject @ProfileDatabase
    private EntityManager em;

	@SuppressWarnings("unused")
	@Inject
	private Logger logger;
	
    public ComplimentsDao() {
		super(Compliments.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

	/**
	 * Retrieves the compliments according the search criteria.
	 * @param sender the managed identity of the receiver of the compliment. 
	 * @param receiver the managed identity of the receiver of the compliment. 
	 * @param maxResults The maximum results in one result set.
	 * @param offset The offset (starting at 0) to start the result set.
	 * @return A pages result. Total count is determined only when maxResults is set to 0.
	 */
	public PagedResult<Long> listComplimentSets(ComplimentsFilter filter, Cursor cursor) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Compliments> root = cq.from(Compliments.class);
        List<Predicate> predicates = new ArrayList<>();
        if (filter.getSender() != null) {
        	predicates.add(cb.equal(root.get(Compliments_.sender).get(User_.managedIdentity), filter.getSender()));
        }
        if (filter.getReceiver() != null) {
        	predicates.add(cb.equal(root.get(Compliments_.receiver).get(User_.managedIdentity), filter.getReceiver()));
        }
        if (filter.getContext() != null) {
        	predicates.add(cb.equal(root.get(Compliments_.context), filter.getContext()));
        }
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = null;
        if (cursor.isCountingQuery()) {
          cq.select(cb.count(root.get(Compliments_.id)));
          totalCount = em.createQuery(cq).getSingleResult();
        } else {
	        cq.select(root.get(Compliments_.id));
	        Expression<?> sortBy = root.get(Compliments_.id);
	        cq.orderBy(filter.getSortDir() == SortDirection.ASC ? cb.asc(sortBy) : cb.desc(sortBy));
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(cursor.getOffset());
			tq.setMaxResults(cursor.getMaxResults());
			results = tq.getResultList();
        }
        return new PagedResult<>(results, cursor, totalCount);
	}

	public Optional<Compliments> findComplimentSetByReceiverAndContext(String rcvmid, String context) {
    	List<Compliments> results = 
    			em.createQuery("from Compliments c where c.receiver.managedIdentity = :receiver and c.context = :context",
    			Compliments.class)
    			.setParameter("receiver", rcvmid)
    			.setParameter("context", context)
    			.getResultList();
    	return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0)); 
	}
}
