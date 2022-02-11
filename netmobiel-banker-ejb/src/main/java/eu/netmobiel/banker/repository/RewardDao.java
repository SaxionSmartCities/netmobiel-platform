package eu.netmobiel.banker.repository;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import eu.netmobiel.banker.annotation.BankerDatabase;
import eu.netmobiel.banker.model.BankerUser;
import eu.netmobiel.banker.model.Incentive;
import eu.netmobiel.banker.model.Reward;
import eu.netmobiel.commons.repository.AbstractDao;

@ApplicationScoped
@Typed(RewardDao.class)
public class RewardDao extends AbstractDao<Reward, Long> {

    @Inject @BankerDatabase
    private EntityManager em;

    public RewardDao() {
		super(Reward.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

	public Optional<Reward> findByFact(Incentive incentive, BankerUser recipient, String factContext) {
		String q = "from Reward rwd where rwd.incentive = :incentive and rwd.recfipient = recipient and rwd.factContext = factContext";
		TypedQuery<Reward> tq = em.createQuery(q, Reward.class);
		tq.setParameter("incentive", incentive);
		tq.setParameter("recipient", recipient);
		tq.setParameter("factContext", factContext);
		List<Reward> results = tq.getResultList();
		if (results.size() > 1) {
			throw new IllegalStateException(String.format("Multiple rewards with same incentive, recipient and fact context: %s %s %s", 
					incentive.getId(), recipient.getId(), factContext));
		}
		return Optional.ofNullable(results.isEmpty() ? null : results.get(0));
	}

}
