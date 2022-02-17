package eu.netmobiel.banker.repository;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import eu.netmobiel.banker.annotation.BankerDatabase;
import eu.netmobiel.banker.model.Incentive;
import eu.netmobiel.commons.repository.AbstractDao;

@ApplicationScoped
@Typed(IncentiveDao.class)
public class IncentiveDao extends AbstractDao<Incentive, Long> {

    @Inject @BankerDatabase
    private EntityManager em;

    public IncentiveDao() {
		super(Incentive.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

	public Optional<Incentive> findByCode(String code) {
		String q = "from Incentive inc where inc.code = :code";
		TypedQuery<Incentive> tq = em.createQuery(q, Incentive.class);
		tq.setParameter("code", code);
		List<Incentive> results = tq.getResultList();
		if (results.size() > 1) {
			throw new IllegalStateException("Multiple incentives with same code: " + code);
		}
		return Optional.ofNullable(results.isEmpty() ? null : results.get(0));
	}
	
	public Optional<Incentive> findByReference(String category, String reference) {
		String q = "from Incentive inc where inc.category = :category and inc.externalReference = :reference";
		TypedQuery<Incentive> tq = em.createQuery(q, Incentive.class);
		tq.setParameter("category", category);
		tq.setParameter("reference", reference);
		List<Incentive> results = tq.getResultList();
		if (results.size() > 1) {
			throw new IllegalStateException("Multiple incentives with same reference: " + category + ", " + reference);
		}
		return Optional.ofNullable(results.isEmpty() ? null : results.get(0));
	}
}
