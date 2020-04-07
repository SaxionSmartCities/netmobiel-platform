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
import eu.netmobiel.banker.model.User_;
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

	public Balance findByLedgerAndAccount(@NotNull Ledger ledger, @NotNull Account account) throws NoResultException, NonUniqueResultException {
		String q = "from Balance bal where bal.ledger = :ledger and bal.account = :account";
		TypedQuery<Balance> tq = em.createQuery(q, Balance.class);
		tq.setParameter("ledger", ledger);
		tq.setParameter("account", account);
		return tq.getSingleResult();
	}
	
	public Balance findByLedgerAndAccountReference(@NotNull Ledger ledger, @NotNull String accountReference) throws NoResultException, NonUniqueResultException {
		String q = "from Balance bal where bal.ledger = :ledger and bal.account.reference = :accountReference";
		TypedQuery<Balance> tq = em.createQuery(q, Balance.class);
		tq.setParameter("ledger", ledger);
		tq.setParameter("accountReference", accountReference);
		return tq.getSingleResult();
	}
	
    public PagedResult<Long> listBalances(String holder, String accountReference, @NotNull Ledger ledger, Integer maxResults, Integer offset) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Balance> entry = cq.from(Balance.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(entry.get(Balance_.ledger), ledger));
        if (holder != null) {
            Predicate predHolder = cb.equal(entry.get(Balance_.account).get(Account_.holder).get(User_.managedIdentity), holder);
            predicates.add(predHolder);
        }
        if (accountReference != null) {
            Predicate predAccRef = cb.equal(entry.get(Balance_.account).get(Account_.reference), accountReference);
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
	        cq.orderBy(cb.desc(entry.get(Balance_.account).get(Account_.reference)));
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(offset);
			tq.setMaxResults(maxResults);
			results = tq.getResultList();
        }
        return new PagedResult<Long>(results, maxResults, offset, totalCount);
    }

}
