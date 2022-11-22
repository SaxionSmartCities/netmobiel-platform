package eu.netmobiel.banker.repository;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.TypedQuery;

import eu.netmobiel.banker.annotation.BankerDatabase;
import eu.netmobiel.banker.model.DepositRequest;
import eu.netmobiel.banker.model.PaymentStatus;
import eu.netmobiel.commons.repository.AbstractDao;

@ApplicationScoped
@Typed(DepositRequestDao.class)
public class DepositRequestDao extends AbstractDao<DepositRequest, Long> {

    @Inject @BankerDatabase
    private EntityManager em;

    public DepositRequestDao() {
		super(DepositRequest.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

	public DepositRequest findByPaymentLink(String paymentLinkId) throws NoResultException, NonUniqueResultException {
		String q = "from DepositRequest dr where dr.paymentLinkId = :paymentLinkId";
		TypedQuery<DepositRequest> tq = em.createQuery(q, DepositRequest.class);
		tq.setParameter("paymentLinkId", paymentLinkId);
		return tq.getSingleResult();
	}
	
	public List<DepositRequest> listByStatus(PaymentStatus status) {
		String q = "from DepositRequest dr where dr.status = :status";
		TypedQuery<DepositRequest> tq = em.createQuery(q, DepositRequest.class);
		tq.setParameter("status", status);
		return tq.getResultList();
	}
}
