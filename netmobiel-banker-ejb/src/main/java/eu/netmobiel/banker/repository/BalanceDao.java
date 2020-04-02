package eu.netmobiel.banker.repository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.TypedQuery;

import eu.netmobiel.banker.annotation.BankerDatabase;
import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.Balance;
import eu.netmobiel.banker.model.Ledger;
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

	public Balance findByLedgerAndAccount(Ledger ledger, Account account) throws NoResultException, NonUniqueResultException {
		String q = "from Balance bal where bal.ledger = :ledger and bal.account = :account";
		TypedQuery<Balance> tq = em.createQuery(q, Balance.class);
		tq.setParameter("ledger", ledger);
		tq.setParameter("account", account);
		return tq.getSingleResult();
	}
	
	public Balance findByLedgerAndAccountReference(Ledger ledger, String accountReference) throws NoResultException, NonUniqueResultException {
		String q = "from Balance bal where bal.ledger = :ledger and bal.account.reference = :accountReference";
		TypedQuery<Balance> tq = em.createQuery(q, Balance.class);
		tq.setParameter("ledger", ledger);
		tq.setParameter("accountReference", accountReference);
		return tq.getSingleResult();
	}
}
