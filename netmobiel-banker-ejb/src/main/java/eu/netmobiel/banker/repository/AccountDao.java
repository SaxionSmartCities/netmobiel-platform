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
import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.AccountPurposeType;
import eu.netmobiel.banker.model.AccountType;
import eu.netmobiel.banker.model.Account_;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.repository.AbstractDao;

@ApplicationScoped
@Typed(AccountDao.class)
public class AccountDao extends AbstractDao<Account, Long> {

    @Inject @BankerDatabase
    private EntityManager em;

    public AccountDao() {
		super(Account.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

	public Optional<Account> findByAccountNumber(String ncan) {
		String q = "from Account acc where acc.ncan = :ncan";
		TypedQuery<Account> tq = em.createQuery(q, Account.class);
		tq.setParameter("ncan", ncan);
		List<Account> accounts = tq.getResultList();
		if (accounts.size() > 1) {
			throw new IllegalStateException("Multiple accounts with same NCAN: " + ncan);
		}
		return Optional.ofNullable(accounts.isEmpty() ? null : accounts.get(0));
	}
	
	/**
	 * Lists the accounts. Filter optionally by holder. 
	 * @param accountName the account name, use '%' for any substring and '_' for any character match. Use '\' to 
	 * 					escape the special characters.  
	 * @param purpose the account purpose type
	 * @param type The account type or null for any type.
	 * @param maxResults The maximum results to query. If set to 0 the total number of results is fetched.
	 * @param offset The zero-based paging offset 
	 * @return a paged result of account identifiers, sorted by account name ascending
	 */
    public PagedResult<Long> listAccounts(String accountName, AccountPurposeType purpose, AccountType type, Integer maxResults, Integer offset) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Account> account = cq.from(Account.class);
        List<Predicate> predicates = new ArrayList<>();
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
          cq.select(cb.count(account.get(Account_.id)));
          totalCount = em.createQuery(cq).getSingleResult();
        } else {
	        cq.select(account.get(Account_.id));
	        cq.orderBy(cb.asc(account.get(Account_.name)));
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(offset);
			tq.setMaxResults(maxResults);
			results = tq.getResultList();
        }
        return new PagedResult<>(results, maxResults, offset, totalCount);
    }
	
}
