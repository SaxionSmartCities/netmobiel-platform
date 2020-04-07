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
import eu.netmobiel.banker.model.User_;
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

    public PagedResult<Long> listAccountingEntries(String holder, String accountReference, Instant since, Instant until, Integer maxResults, Integer offset) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<AccountingEntry> entry = cq.from(AccountingEntry.class);
        List<Predicate> predicates = new ArrayList<>();
        if (holder != null) {
            Predicate predHolder = cb.equal(entry.get(AccountingEntry_.account).get(Account_.holder).get(User_.managedIdentity), holder);
            predicates.add(predHolder);
        }
        if (accountReference != null) {
            Predicate predAccRef = cb.equal(entry.get(AccountingEntry_.account).get(Account_.reference), accountReference);
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
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (maxResults == 0) {
          cq.select(cb.count(entry.get(AccountingEntry_.id)));
          totalCount = em.createQuery(cq).getSingleResult();
        } else {
	        cq.select(entry.get(AccountingEntry_.id));
	        cq.orderBy(cb.desc(entry.get(AccountingEntry_.transaction).get(AccountingTransaction_.accountingTime)),
	        		cb.asc(entry.get(AccountingEntry_.entryType)));
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(offset);
			tq.setMaxResults(maxResults);
			results = tq.getResultList();
        }
        return new PagedResult<Long>(results, maxResults, offset, totalCount);
    }

}
