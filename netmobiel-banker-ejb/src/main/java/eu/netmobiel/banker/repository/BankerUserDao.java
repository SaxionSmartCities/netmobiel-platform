package eu.netmobiel.banker.repository;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import eu.netmobiel.banker.annotation.BankerDatabase;
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
	 * Finds all banker users that have no persoanl account set.
	 * @return A list of users without an personal account.
	 */
	public List<BankerUser> findUsersWithoutPersonalAccount() {
		String q = "from BankerUser u where u.personalAccount is null";
		TypedQuery<BankerUser> tq = em.createQuery(q, BankerUser.class);
		return tq.getResultList();
	}

}
