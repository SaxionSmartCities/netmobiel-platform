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
import eu.netmobiel.profile.filter.ComplimentFilter;
import eu.netmobiel.profile.model.Compliment;
import eu.netmobiel.profile.model.Compliment_;


@ApplicationScoped
@Typed(ComplimentDao.class)
public class ComplimentDao extends AbstractDao<Compliment, Long> {

	@Inject @ProfileDatabase
    private EntityManager em;

	@SuppressWarnings("unused")
	@Inject
	private Logger logger;
	
    public ComplimentDao() {
		super(Compliment.class);
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
	public PagedResult<Long> listCompliments(ComplimentFilter filter, Cursor cursor) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Compliment> compliment = cq.from(Compliment.class);
        List<Predicate> predicates = new ArrayList<>();
        if (filter.getSender() != null) {
        	predicates.add(cb.equal(compliment.get(Compliment_.sender).get(User_.managedIdentity), filter.getSender()));
        }
        if (filter.getReceiver() != null) {
        	predicates.add(cb.equal(compliment.get(Compliment_.receiver).get(User_.managedIdentity), filter.getReceiver()));
        }
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = null;
        if (cursor.isCountingQuery()) {
          cq.select(cb.count(compliment.get(Compliment_.id)));
          totalCount = em.createQuery(cq).getSingleResult();
        } else {
	        cq.select(compliment.get(Compliment_.id));
	        Expression<?> sortBy = compliment.get(Compliment_.id);
	        cq.orderBy(filter.getSortDir() == SortDirection.ASC ? cb.asc(sortBy) : cb.desc(sortBy));
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(cursor.getOffset());
			tq.setMaxResults(cursor.getMaxResults());
			results = tq.getResultList();
        }
        return new PagedResult<>(results, cursor, totalCount);
	}

	public Optional<Compliment> findComplimentByAttributes(Compliment r) {
    	List<Compliment> results = em.createQuery("from Compliment r where r.receiver.managedIdentity = :receiver " 
    				+ "and r.sender.managedIdentity = :sender and r.compliment = :compliment and r.published = :published",
    			Compliment.class)
    			.setParameter("receiver", r.getReceiver().getManagedIdentity())
    			.setParameter("sender", r.getSender().getManagedIdentity())
    			.setParameter("compliment", r.getCompliment())
    			.setParameter("published", r.getPublished())
    			.getResultList();
    	return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0)); 
	}
}
