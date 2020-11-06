package eu.netmobiel.banker.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import eu.netmobiel.banker.annotation.BankerDatabase;
import eu.netmobiel.banker.model.PaymentBatch;
import eu.netmobiel.banker.model.PaymentBatch_;
import eu.netmobiel.banker.model.PaymentStatus;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.repository.AbstractDao;

@ApplicationScoped
@Typed(PaymentBatchDao.class)
public class PaymentBatchDao extends AbstractDao<PaymentBatch, Long> {

    @Inject @BankerDatabase
    private EntityManager em;

    public PaymentBatchDao() {
		super(PaymentBatch.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

	public List<PaymentBatch> findByStatus(PaymentStatus status) {
		String q = "from PaymentBatch pb where pb.status = :status";
		TypedQuery<PaymentBatch> tq = em.createQuery(q, PaymentBatch.class);
		tq.setParameter("status", status);
		return tq.getResultList();
	}
	
	/**
	 * Lists the payment batches as a paged result set according the filter parameters. Supply null as values when
	 * a parameter is don't care.
	 * @param since the first date to take into account for creation time.
	 * @param until the last date (exclusive) to take into account for creation time.
	 * @param status The status to filter by. 
	 * @param maxResults The maximum number of results per page. Only if set to 0 the total number of results is returned. 
	 * @param offset the zero-based offset in the result set.
	 * @return A paged result with 0 or more results. Total count is only determined when maxResults is set to 0. 
	 * 		   The results are ordered by creation time descending and	then by id descending.
	 */
    public PagedResult<Long> list(Instant since, Instant until, PaymentStatus status, Integer maxResults, Integer offset) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<PaymentBatch> root = cq.from(PaymentBatch.class);
        List<Predicate> predicates = new ArrayList<>();
        if (since != null) {
	        predicates.add(cb.greaterThanOrEqualTo(root.get(PaymentBatch_.creationTime), since));
        }        
        if (until != null) {
	        predicates.add(cb.lessThan(root.get(PaymentBatch_.creationTime), until));
        }        
        if (status != null) {
	        predicates.add(cb.equal(root.get(PaymentBatch_.status), status));
        }

        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (maxResults == 0) {
          cq.select(cb.count(root.get(PaymentBatch_.id)));
          totalCount = em.createQuery(cq).getSingleResult();
        } else {
	        cq.select(root.get(PaymentBatch_.id));
	        cq.orderBy(cb.desc(root.get(PaymentBatch_.creationTime)), cb.desc(root.get(PaymentBatch_.id)));
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(offset);
			tq.setMaxResults(maxResults);
			results = tq.getResultList();
        }
        return new PagedResult<Long>(results, maxResults, offset, totalCount);
    }

    /**
     * Fetch for each given payment batch id the number of withdrawal requests contained in it. 
     * @param ids The payment batch IDs to query.
     * @return A map of payment batch id to withdrawal request count.
     */
    public Map<Long, Integer> fetchCount(List<Long> ids) {
    	if (ids == null || ids.isEmpty()) {
    		return Collections.emptyMap();
    	}
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createQuery(Tuple.class);
        Root<PaymentBatch> root = cq.from(PaymentBatch.class);
        Path<Long> idPath = root.get(PaymentBatch_.id);
        Expression<Integer> countExpr = cb.size(root.get(PaymentBatch_.withdrawalRequests));
        cq.groupBy(idPath);
        cq.multiselect(idPath, countExpr);
        cq.where(idPath.in(ids));
        TypedQuery<Tuple> tq = getEntityManager().createQuery(cq);
        return tq.getResultList().stream()
        		.collect(Collectors.toMap(tup -> tup.get(idPath), tup -> tup.get(countExpr)));
    }

}
