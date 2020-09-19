package eu.netmobiel.rideshare.repository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import eu.netmobiel.commons.repository.UserDao;
import eu.netmobiel.rideshare.annotation.RideshareDatabase;
import eu.netmobiel.rideshare.model.RideshareUser;

@ApplicationScoped
@Typed(RideshareUserDao.class)
public class RideshareUserDao extends UserDao<RideshareUser> {

	@Inject @RideshareDatabase
    private EntityManager em;

    public RideshareUserDao() {
		super(RideshareUser.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

}
