package eu.netmobiel.banker.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;

import eu.netmobiel.banker.annotation.BankerDatabase;
import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.Account_;
import eu.netmobiel.banker.model.Balance;
import eu.netmobiel.banker.model.Balance_;
import eu.netmobiel.banker.model.Ledger;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.repository.AbstractDao;

@ApplicationScoped
@Typed(BalanceDao.class)
public class BalanceDao extends AbstractDao<Balance, Long> {

    @Inject @BankerDatabase
    private EntityManager em;

    public BalanceDao() {
		super(Balance.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

	public Balance findActualBalance(@NotNull Account account) throws NoResultException, NonUniqueResultException {
		String q = "from Balance bal where bal.ledger.endPeriod is null and bal.account = :account";
		return em.createQuery(q, Balance.class)
				.setParameter("account", account)
				.getSingleResult();
	}
	
	public Balance findByLedgerAndAccount(@NotNull Ledger ledger, @NotNull Account account) throws NoResultException, NonUniqueResultException {
		String q = "from Balance bal where bal.ledger = :ledger and bal.account = :account";
		return em.createQuery(q, Balance.class)
				.setParameter("ledger", ledger)
				.setParameter("account", account)
				.getSingleResult();
	}
	
	public Balance findByLedgerAndAccountNumber(@NotNull Ledger ledger, @NotNull String ncan) throws NoResultException, NonUniqueResultException {
		String q = "from Balance bal where bal.ledger = :ledger and bal.account.ncan = :ncan";
		return em.createQuery(q, Balance.class)
				.setParameter("ledger", ledger)
				.setParameter("ncan", ncan)
				.getSingleResult();
	}
	
    public PagedResult<Long> listBalances(Account acc, @NotNull Ledger ledger, Integer maxResults, Integer offset) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Balance> entry = cq.from(Balance.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(entry.get(Balance_.ledger), ledger));
        if (acc != null) {
            Predicate predAccRef = cb.equal(entry.get(Balance_.account), acc);
            predicates.add(predAccRef);
        }
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (maxResults == 0) {
          cq.select(cb.count(entry.get(Balance_.id)));
          totalCount = em.createQuery(cq).getSingleResult();
        } else {
	        cq.select(entry.get(Balance_.id));
	        cq.orderBy(cb.desc(entry.get(Balance_.account).get(Account_.id)));
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(offset);
			tq.setMaxResults(maxResults);
			results = tq.getResultList();
        }
        return new PagedResult<>(results, maxResults, offset, totalCount);
    }

}
