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
import eu.netmobiel.banker.model.AccountingEntry;
import eu.netmobiel.banker.model.AccountingEntry_;
import eu.netmobiel.banker.model.AccountingTransaction_;
import eu.netmobiel.banker.model.TransactionType;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.repository.AbstractDao;

@ApplicationScoped
@Typed(AccountingEntryDao.class)
public class AccountingEntryDao extends AbstractDao<AccountingEntry, Long> {

    @Inject @BankerDatabase
    private EntityManager em;

    public AccountingEntryDao() {
		super(AccountingEntry.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

	/**
	 * Lists the accounting entries as a paged result set according the filter parameters. Supply null as values when
	 * a parameter is don't care.
	 * @param accountReference the account reference
	 * @param since the first date to take into account for accountingTime.
	 * @param until the last date (exclusive) to take into account for accountingTime.
	 * @param purpose the transaction type to filter on
	 * @param maxResults The maximum number of results per page. Only if set to 0 the total number of results is returned. 
	 * @param offset the zero-based offset in the result set.
	 * @return A paged result with 0 or more results. Total count is only determined when maxResults is set to 0. The results are ordered by transaction time descending and
	 * 		then by id descending.
	 */
    public PagedResult<Long> listAccountingEntries(String accountReference, Instant since, Instant until, TransactionType purpose, Integer maxResults, Integer offset) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<AccountingEntry> entry = cq.from(AccountingEntry.class);
        List<Predicate> predicates = new ArrayList<>();
        if (accountReference != null) {
            Predicate predAccRef = cb.equal(entry.get(AccountingEntry_.account).get(Account_.ncan), accountReference);
            predicates.add(predAccRef);
        }
        if (since != null) {
	        Predicate predSince = cb.greaterThanOrEqualTo(entry.get(AccountingEntry_.transaction).get(AccountingTransaction_.accountingTime), since);
	        predicates.add(predSince);
        }        
        if (until != null) {
	        Predicate predUntil = cb.lessThan(entry.get(AccountingEntry_.transaction).get(AccountingTransaction_.accountingTime), until);
	        predicates.add(predUntil);
        }        
        if (purpose != null) {
	        Predicate predPurpose = cb.equal(entry.get(AccountingEntry_.purpose), purpose);
	        predicates.add(predPurpose);
        }        
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (maxResults == 0) {
          cq.select(cb.count(entry.get(AccountingEntry_.id)));
          totalCount = em.createQuery(cq).getSingleResult();
        } else {
	        cq.select(entry.get(AccountingEntry_.id));
	        cq.orderBy(cb.desc(entry.get(AccountingEntry_.transaction).get(AccountingTransaction_.transactionTime)),
	        		cb.desc(entry.get(AccountingEntry_.id)));
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(offset);
			tq.setMaxResults(maxResults);
			results = tq.getResultList();
        }
        return new PagedResult<>(results, maxResults, offset, totalCount);
    }

}
