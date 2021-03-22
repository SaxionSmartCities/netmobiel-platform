package eu.netmobiel.profile.repository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.profile.annotation.ProfileDatabase;
import eu.netmobiel.profile.model.Place;


@ApplicationScoped
@Typed(PlaceDao.class)
public class PlaceDao extends AbstractDao<Place, Long> {

	@Inject @ProfileDatabase
    private EntityManager em;

    public PlaceDao() {
		super(Place.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

}
