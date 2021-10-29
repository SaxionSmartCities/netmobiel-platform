package eu.netmobiel.communicator.repository;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.communicator.annotation.CommunicatorDatabase;
import eu.netmobiel.communicator.model.CommunicatorUser;
import eu.netmobiel.communicator.model.Conversation;

@ApplicationScoped
@Typed(ConversationDao.class)
public class ConversationDao extends AbstractDao<Conversation, Long> {

    @Inject @CommunicatorDatabase
    private EntityManager em;

    @Inject
    private Logger logger;

    public ConversationDao() {
		super(Conversation.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

//	public Optional<Conversation> findByContextAndOwner(String context, CommunicatorUser owner) {
//		String q = "select distinct c from Conversation c where c in " +
//				"(select m.senderConversation from Message m where m.senderConversation is not null and " + 
//				"m.senderConversation.owner = :owner and m.context = :context) " +
//				"or c in " +
//				"(select e.conversation from Envelope e where e.conversation.owner = :owner and " + 
//				"((e.context is not null and e.context = :context) or (e.context is null and e.message.context = :context)))";
//		TypedQuery<Conversation> tq = em.createQuery(q, Conversation.class);
//		tq.setMaxResults(2);
//		tq.setParameter("owner", owner);
//		tq.setParameter("context", context);
//		List<Conversation> cs = tq.getResultList();
//		if (cs.size() > 1) {
//			logger.warn(String.format("Multiple matching conversation for context %s and user %s", context, owner));
//		}
//		return cs.size() > 0 ? Optional.of(cs.get(0)) : Optional.empty();
//	}

	public Optional<Conversation> findByContextAndOwner(String context, CommunicatorUser owner) {
		String q = "select c from Conversation c where c.owner = :owner and :context member of c.contexts";
		TypedQuery<Conversation> tq = em.createQuery(q, Conversation.class);
		tq.setMaxResults(2);
		tq.setParameter("owner", owner);
		tq.setParameter("context", context);
		List<Conversation> cs = tq.getResultList();
		if (cs.size() > 1) {
			logger.warn(String.format("Multiple matching conversation for context %s and user %s", context, owner));
		}
		return cs.size() > 0 ? Optional.of(cs.get(0)) : Optional.empty();
	}
	

	public Optional<Conversation> findByContextAndOwner(String context, String managedIdentity) {
		String q = "select c from Conversation c where c.owner.managedIdentity = :owner and :context member of c.contexts";
		TypedQuery<Conversation> tq = em.createQuery(q, Conversation.class);
		tq.setMaxResults(2);
		tq.setParameter("owner", managedIdentity);
		tq.setParameter("context", context);
		List<Conversation> cs = tq.getResultList();
		if (cs.size() > 1) {
			logger.warn(String.format("Multiple matching conversation for context %s and user %s", context, managedIdentity));
		}
		return cs.size() > 0 ? Optional.of(cs.get(0)) : Optional.empty();
	}
}
