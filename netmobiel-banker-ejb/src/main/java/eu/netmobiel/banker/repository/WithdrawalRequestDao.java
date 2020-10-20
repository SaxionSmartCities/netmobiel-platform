package eu.netmobiel.banker.repository;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import eu.netmobiel.banker.annotation.BankerDatabase;
import eu.netmobiel.banker.model.PaymentStatus;
import eu.netmobiel.banker.model.WithdrawalRequest;
import eu.netmobiel.commons.repository.AbstractDao;

@ApplicationScoped
@Typed(WithdrawalRequestDao.class)
public class WithdrawalRequestDao extends AbstractDao<WithdrawalRequest, Long> {

    @Inject @BankerDatabase
    private EntityManager em;

    public WithdrawalRequestDao() {
		super(WithdrawalRequest.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

	public List<WithdrawalRequest> findByStatus(PaymentStatus status) {
		String q = "from WithdrawalRequest wr where wr.status = :status";
		TypedQuery<WithdrawalRequest> tq = em.createQuery(q, WithdrawalRequest.class);
		tq.setParameter("status", status);
		return tq.getResultList();
	}
}
