package eu.netmobiel.banker.repository;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.TypedQuery;

import eu.netmobiel.banker.annotation.BankerDatabase;
import eu.netmobiel.banker.model.Ledger;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.repository.AbstractDao;

@ApplicationScoped
@Typed(LedgerDao.class)
public class LedgerDao extends AbstractDao<Ledger, Long> {

    @Inject @BankerDatabase
    private EntityManager em;

    public LedgerDao() {
		super(Ledger.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

	public Ledger findByDate(Instant point) throws NoResultException, NonUniqueResultException {
		String q = "from Ledger ldg where :point >= ldg.startPeriod and (:point < ldg.endPeriod or ldg.endPeriod is null)";
		TypedQuery<Ledger> tq = em.createQuery(q, Ledger.class);
		tq.setParameter("point", point);
		return tq.getSingleResult();
	}
	
	public PagedResult<Long> listLedgers(Integer maxResults, Integer offset) {
		Long totalCount = null;
        List<Long> results = null;
        if (maxResults == 0) {
    		TypedQuery<Long> countQuery = em.createQuery("select count(ldg) from Ledger ldg", Long.class);
            totalCount = countQuery.getSingleResult();
            results = Collections.emptyList();
        } else {
    		TypedQuery<Long> selectQuery = em.createQuery("from Ledger ldg order by ldg.startPeriod desc", Long.class);
    		selectQuery.setFirstResult(offset);
    		selectQuery.setMaxResults(maxResults);
    		results = selectQuery.getResultList();
        }
        return new PagedResult<Long>(results, maxResults, offset, totalCount);
	}

}
