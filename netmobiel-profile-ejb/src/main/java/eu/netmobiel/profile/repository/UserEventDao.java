package eu.netmobiel.profile.repository;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.profile.annotation.ProfileDatabase;
import eu.netmobiel.profile.model.PageVisit;


@ApplicationScoped
@Typed(PageVisitDao.class)
public class PageVisitDao extends AbstractDao<PageVisit, String> {

	@Inject @ProfileDatabase
    private EntityManager em;

	public PageVisitDao() {
		super(String.class, PageVisit.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

}
