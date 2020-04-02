package eu.netmobiel.banker.repository;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.TypedQuery;

import eu.netmobiel.banker.annotation.BankerDatabase;
import eu.netmobiel.banker.model.Account;
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

	public Account findByReference(String reference) throws NoResultException, NonUniqueResultException {
		String q = "from Account acc where acc.reference = :reference";
		TypedQuery<Account> tq = em.createQuery(q, Account.class);
		tq.setParameter("reference", reference);
		return tq.getSingleResult();
	}
	
	@Override
	public List<Account> fetch(List<Long> ids, String graphName) {
		return super.fetch(ids, graphName, Account::getId);
	}
}
