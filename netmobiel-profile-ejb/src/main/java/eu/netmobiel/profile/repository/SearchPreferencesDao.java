package eu.netmobiel.profile.repository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.slf4j.Logger;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.profile.annotation.ProfileDatabase;
import eu.netmobiel.profile.model.SearchPreferences;


@ApplicationScoped
@Typed(SearchPreferencesDao.class)
public class SearchPreferencesDao extends AbstractDao<SearchPreferences, Long> {

	@Inject @ProfileDatabase
    private EntityManager em;

	@SuppressWarnings("unused")
	@Inject
	private Logger logger;
	
    public SearchPreferencesDao() {
		super(SearchPreferences.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

}
