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
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;

import eu.netmobiel.banker.annotation.BankerDatabase;
import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.AccountPurposeType;
import eu.netmobiel.banker.model.AccountType;
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

	/**
	 * Finds the balance given an account. This method is to inform the caller about the number of credits in the balance.
	 * A dirty read is no problem.
	 * @param account the account in question
	 * @return the balance belonging to the account and the ledger period.
	 * @throws NoResultException
	 * @throws NonUniqueResultException
	 */
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

	/**
	 * Lists the balances by filtering their accounts. 
	 * @param accountName the account name, use '%' for any substring and '_' for any character match. Use '\' to 
	 * 					escape the special characters.  
	 * @param purpose the account purpose type
	 * @param type The account type or null for any type.
	 * @param ledger The ledger to look for. Cannot be null.
	 * @param maxResults The maximum results to query. If set to 0 the total number of results is fetched.
	 * @param offset The zero-based paging offset 
	 * @return a paged result of account identifiers, sorted by account name ascending
	 */
    public PagedResult<Long> listBalances(String accountName, AccountPurposeType purpose, AccountType type, @NotNull Ledger ledger, Integer maxResults, Integer offset) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Balance> root = cq.from(Balance.class);
        List<Predicate> predicates = new ArrayList<>();
        Path<Account> account = root.get(Balance_.account);
        predicates.add(cb.equal(root.get(Balance_.ledger), ledger));
        if (accountName != null) {
            predicates.add(cb.like(cb.lower(account.get(Account_.name)), accountName.toLowerCase(), '\\'));
        }
        if (purpose != null) {
            predicates.add(cb.equal(account.get(Account_.purpose), purpose));
        }
        if (type != null) {
            Predicate predType = cb.equal(account.get(Account_.accountType), type);
            predicates.add(predType);
        }
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (maxResults == 0) {
          cq.select(cb.count(root.get(Balance_.id)));
          totalCount = em.createQuery(cq).getSingleResult();
        } else {
	        cq.select(root.get(Balance_.id));
	        cq.orderBy(cb.asc(account.get(Account_.name)));
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(offset);
			tq.setMaxResults(maxResults);
			results = tq.getResultList();
        }
        return new PagedResult<>(results, maxResults, offset, totalCount);
    }
	
}
