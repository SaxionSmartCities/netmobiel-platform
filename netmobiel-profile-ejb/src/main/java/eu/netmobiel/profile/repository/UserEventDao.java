package eu.netmobiel.profile.repository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.profile.annotation.ProfileDatabase;
import eu.netmobiel.profile.model.UserEvent;


@ApplicationScoped
@Typed(UserEventDao.class)
public class UserEventDao extends AbstractDao<UserEvent, String> {

	@Inject @ProfileDatabase
    private EntityManager em;

	public UserEventDao() {
		super(String.class, UserEvent.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

}
