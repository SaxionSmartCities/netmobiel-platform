package eu.netmobiel.profile.repository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.slf4j.Logger;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.profile.annotation.ProfileDatabase;
import eu.netmobiel.profile.model.RidesharePreferences;


@ApplicationScoped
@Typed(RidesharePreferencesDao.class)
public class RidesharePreferencesDao extends AbstractDao<RidesharePreferences, Long> {

	@Inject @ProfileDatabase
    private EntityManager em;

	@SuppressWarnings("unused")
	@Inject
	private Logger logger;
	
    public RidesharePreferencesDao() {
		super(RidesharePreferences.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

}
