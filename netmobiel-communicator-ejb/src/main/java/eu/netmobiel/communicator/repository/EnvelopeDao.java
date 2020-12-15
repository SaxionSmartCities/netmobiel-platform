package eu.netmobiel.communicator.repository;

import java.time.Instant;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.report.NumericReportValue;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.communicator.annotation.CommunicatorDatabase;
import eu.netmobiel.communicator.model.Envelope;

@ApplicationScoped
@Typed(EnvelopeDao.class)
public class EnvelopeDao extends AbstractDao<Envelope, Long> {

    @Inject @CommunicatorDatabase
    private EntityManager em;

    public EnvelopeDao() {
		super(Envelope.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

	public void saveAll(List<Envelope> envelopes) {
		for (Envelope envelope : envelopes) {
			save(envelope);
		}
	}

	public Envelope findByMessageAndRecipient(Long messageId, String managedIdentity) throws NoResultException {
		String q = "select e from Envelope e where e.message.id = :messageId and e.recipient.managedIdentity = :identity";
		TypedQuery<Envelope> tq = em.createQuery(q, Envelope.class);
		tq.setParameter("messageId", messageId);
		tq.setParameter("identity", managedIdentity);
		return tq.getSingleResult();
	}
	
    public List<NumericReportValue> reportMessagesReceived(Instant since, Instant until) throws BadRequestException {
        return em.createNamedQuery("ListMessagesReceivedCount", NumericReportValue.class)
        		.setParameter(1, since)
        		.setParameter(2, until)
        		.getResultList();
    }

    public List<NumericReportValue> reportNotificationsReceived(Instant since, Instant until) throws BadRequestException {
        return em.createNamedQuery("ListNotificationsReceivedCount", NumericReportValue.class)
        		.setParameter(1, since)
        		.setParameter(2, until)
        		.getResultList();
    }

    public List<NumericReportValue> reportMessagesRead(Instant since, Instant until) throws BadRequestException {
        return em.createNamedQuery("ListMessagesReadCount", NumericReportValue.class)
        		.setParameter(1, since)
        		.setParameter(2, until)
        		.getResultList();
    }

    public List<NumericReportValue> reportNotificationsRead(Instant since, Instant until) throws BadRequestException {
        return em.createNamedQuery("ListNotificationsReadCount", NumericReportValue.class)
        		.setParameter(1, since)
        		.setParameter(2, until)
        		.getResultList();
    }
}
