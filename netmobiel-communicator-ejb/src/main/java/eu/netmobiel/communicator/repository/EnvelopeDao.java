package eu.netmobiel.communicator.repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

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
	
	@Override
	public List<Envelope> fetch(List<Long> ids, String graphName) {
		Map<Long, Envelope> resultMap;
		if (ids.size() > 0) {
			// Create an identity map using the generic fetch. Rows are returned, but not necessarily in the same order
			resultMap = super.fetch(ids, graphName).stream().collect(Collectors.toMap(Envelope::getId, Function.identity()));
		} else {
			resultMap = Collections.emptyMap();
		}
		// Now return the rows in the same order as the ids.
		return ids.stream().map(id -> resultMap.get(id)).collect(Collectors.toList());
	}
}
