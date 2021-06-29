package eu.netmobiel.profile.repository;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.profile.annotation.ProfileDatabase;
import eu.netmobiel.profile.model.Place;
import eu.netmobiel.profile.model.Place_;
import eu.netmobiel.profile.model.Profile;


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

	/**
	 * Retrieves the Places according the search criteria.
	 * @param filter the filter criteria. 
	 * @param cursor The cursor to use.
	 * @return A pages result. Total count is determined only when maxResults is set to 0.
	 */
	public PagedResult<Long> listPlaces(Profile profile, Cursor cursor) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Place> root = cq.from(Place.class);
        Long totalCount = null;
        List<Long> results = null;
        if (cursor.isCountingQuery()) {
          cq.select(cb.count(root.get(Place_.id)));
          totalCount = em.createQuery(cq).getSingleResult();
        } else {
	        cq.select(root.get(Place_.id));
	        Expression<?> sortBy = root.get(Place_.id);
	        cq.orderBy(cb.asc(sortBy));
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(cursor.getOffset());
			tq.setMaxResults(cursor.getMaxResults());
			results = tq.getResultList();
        }
        return new PagedResult<>(results, cursor, totalCount);
	}
}
