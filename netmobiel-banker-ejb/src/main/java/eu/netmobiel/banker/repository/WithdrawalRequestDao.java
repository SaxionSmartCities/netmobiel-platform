package eu.netmobiel.banker.repository;

import java.time.Instant;
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
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import eu.netmobiel.banker.annotation.BankerDatabase;
import eu.netmobiel.banker.model.Account_;
import eu.netmobiel.banker.model.PaymentStatus;
import eu.netmobiel.banker.model.WithdrawalRequest;
import eu.netmobiel.banker.model.WithdrawalRequest_;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.repository.AbstractDao;

@ApplicationScoped
@Typed(WithdrawalRequestDao.class)
public class WithdrawalRequestDao extends AbstractDao<WithdrawalRequest, Long> {

    @Inject @BankerDatabase
    private EntityManager em;

    public WithdrawalRequestDao() {
		super(WithdrawalRequest.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

	/**
	 * Finds all withdrawal requests that have not yet been attached to a payment batch.
	 * @return A list, possibly empty, of withdrawal requests.
	 */
	public List<WithdrawalRequest> findPendingRequests() {
		String q = "from WithdrawalRequest wr where wr.status = :status and wr.paymentBatch is null";
		TypedQuery<WithdrawalRequest> tq = em.createQuery(q, WithdrawalRequest.class);
		tq.setParameter("status", PaymentStatus.REQUESTED);
		return tq.getResultList();
	}
	
	/**
	 * Lists the withdrawal requests as a paged result set according the filter parameters. Supply null as values when
	 * a parameter is don't care.
	 * @param accountName the account name, use '%' for any substring and '_' for any character match. Use '\' to 
	 * 					escape the special characters.  
	 * @param since the first date to take into account for creation time.
	 * @param until the last date (exclusive) to take into account for creation time.
	 * @param status the status to filter on. 
	 * @param maxResults The maximum number of results per page. Only if set to 0 the total number of results is returned. 
	 * @param offset the zero-based offset in the result set.
	 * @return A paged result with 0 or more results. Total count is only determined when maxResults is set to 0. The results are ordered by creation time descending and
	 * 		then by id descending.
	 */
    public PagedResult<Long> list(String accountName, Instant since, Instant until, PaymentStatus status, Integer maxResults, Integer offset) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<WithdrawalRequest> root = cq.from(WithdrawalRequest.class);
        List<Predicate> predicates = new ArrayList<>();
        if (accountName != null) {
            predicates.add(cb.like(cb.lower(root.get(WithdrawalRequest_.account).get(Account_.name)), accountName.toLowerCase(), '\\'));
        }
        if (since != null) {
	        predicates.add(cb.greaterThanOrEqualTo(root.get(WithdrawalRequest_.creationTime), since));
        }        
        if (until != null) {
	        predicates.add(cb.lessThan(root.get(WithdrawalRequest_.creationTime), until));
        }        
        if (status != null) {
	        predicates.add(cb.equal(root.get(WithdrawalRequest_.status), status));
        }

        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (maxResults == 0) {
          cq.select(cb.count(root.get(WithdrawalRequest_.id)));
          totalCount = em.createQuery(cq).getSingleResult();
        } else {
	        cq.select(root.get(WithdrawalRequest_.id));
	        cq.orderBy(cb.desc(root.get(WithdrawalRequest_.creationTime)), cb.desc(root.get(WithdrawalRequest_.id)));
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(offset);
			tq.setMaxResults(maxResults);
			results = tq.getResultList();
        }
        return new PagedResult<>(results, maxResults, offset, totalCount);
    }

}
