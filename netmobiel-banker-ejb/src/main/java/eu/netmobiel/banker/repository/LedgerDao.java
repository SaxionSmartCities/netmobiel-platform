package eu.netmobiel.banker.repository;

import java.time.Instant;
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
		String q = "from Ledger ldg where :point >= ldg.startPeriod and :point < ldg.endPeriod";
		TypedQuery<Ledger> tq = em.createQuery(q, Ledger.class);
		tq.setParameter("point", point);
		return tq.getSingleResult();
	}
	
	@Override
	public List<Ledger> fetch(List<Long> ids, String graphName) {
		return super.fetch(ids, graphName, Ledger::getId);
	}
}
