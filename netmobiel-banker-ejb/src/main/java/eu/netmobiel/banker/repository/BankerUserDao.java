package eu.netmobiel.banker.repository;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import eu.netmobiel.banker.annotation.BankerDatabase;
import eu.netmobiel.banker.model.Account;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.commons.repository.UserDao;


@ApplicationScoped
@Typed(BankerUserDao.class)
public class BankerUserDao extends UserDao<BankerUser> {

	@Inject @BankerDatabase
    private EntityManager em;

    public BankerUserDao() {
		super(BankerUser.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

	/**
	 * Finds all banker users that have no personal account set.
	 * @return A list of users without an personal account.
	 */
	public List<BankerUser> findUsersWithoutPersonalAccount() {
		String q = "from BankerUser u where u.personalAccount is null";
		TypedQuery<BankerUser> tq = em.createQuery(q, BankerUser.class);
		return tq.getResultList();
	}

	/**
	 * Finds all banker users that have no premium account set.
	 * @return A list of users without an personal account.
	 */
	public List<BankerUser> findUsersWithoutPremiumAccount() {
		String q = "from BankerUser u where u.premiumAccount is null";
		TypedQuery<BankerUser> tq = em.createQuery(q, BankerUser.class);
		return tq.getResultList();
	}
	
	public Optional<BankerUser> findByPersonalAccount(Account acc) {
		String q = "from BankerUser u where personalAccount = :acc";
		TypedQuery<BankerUser> tq = em.createQuery(q, BankerUser.class);
		tq.setParameter("acc", acc);
		List<BankerUser> users = tq.getResultList();
		if (users.size() > 1) {
			throw new IllegalStateException("Multiple users with same Account: " + acc.getId());
		}
		return Optional.ofNullable(users.isEmpty() ? null : users.get(0));
	}
	

}
