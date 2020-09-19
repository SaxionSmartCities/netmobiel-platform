package eu.netmobiel.banker.repository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;

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

}
