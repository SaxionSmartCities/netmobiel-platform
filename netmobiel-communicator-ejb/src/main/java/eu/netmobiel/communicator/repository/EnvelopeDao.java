package eu.netmobiel.communicator.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.communicator.annotation.CommunicatorDatabase;
import eu.netmobiel.communicator.model.CommunicatorUser;
import eu.netmobiel.communicator.model.Conversation;
import eu.netmobiel.communicator.model.DeliveryMode;
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

	public Envelope findByMessageAndRecipient(Long messageId, CommunicatorUser user) throws NoResultException {
		String q = "select e from Envelope e where e.message.id = :messageId and e.conversation.owner = :identity";
		TypedQuery<Envelope> tq = em.createQuery(q, Envelope.class);
		tq.setParameter("messageId", messageId);
		tq.setParameter("identity", user);
		return tq.getSingleResult();
	}
	
    public static class UnreadMessagesCount {
    	public Long conversationId;
    	public Long unreadCount;
    	
    	public UnreadMessagesCount(Long aConversationId, Long anUnreadCount) {
    		this.conversationId = aConversationId;
    		this.unreadCount = anUnreadCount;
    	}
    }

    /**
	 * Count the unread messages in each conversation.
	 * @param conversations the conversations to count the messages of.
	 * @return a list of UnreadMessagesCount objects.
	 */
	public List<UnreadMessagesCount> countUnreadMessages(Collection<Long> conversationIds) {
		if (conversationIds.isEmpty()) {
			return Collections.emptyList();
		}
		String q = "select new eu.netmobiel.communicator.repository.EnvelopeDao$UnreadMessagesCount(e.conversation.id, count(e)) " +
				"from Envelope e where e.conversation.id in :conversationIds and e.sender = false and e.ackTime is null and " +
				"e.message.deliveryMode in :deliveryModes group by e.conversation";
		TypedQuery<UnreadMessagesCount> tq = em.createQuery(q, UnreadMessagesCount.class);
		tq.setParameter("conversationIds", conversationIds);
		tq.setParameter("deliveryModes", EnumSet.of(DeliveryMode.ALL, DeliveryMode.MESSAGE));
		return tq.getResultList();
	}

	/**
	 * Mark all envelopes of the conversation as acknowledged.
	 * @param conversation the conversation to acknowledge. 
	 * @param ackTime the the timestamp to use.
	 */
	public void acknowledge(Conversation conversation, Instant ackTime) {
		String queryString = 
			"update Envelope e set e.ackTime = :ackTime where e.conversation = :conversation and e.ackTime is null";
		Query query = em.createQuery(queryString);
		query.setParameter("ackTime", ackTime);
		query.setParameter("conversation", conversation);
   		query.executeUpdate();
	}

    /**
	 * Count the unread messages in each conversation.
	 * @param user the user to count the unread messages for.
	 * @return a count value.
	 */
	public int countUnreadMessages(CommunicatorUser user) {
		String q = "select count(e) from Envelope e where e.conversation.owner = :user and " + 
						"e.sender = false and e.ackTime is null and e.message.deliveryMode in :deliveryModes";
		TypedQuery<Long> tq = em.createQuery(q, Long.class);
		tq.setParameter("user", user);
		tq.setParameter("deliveryModes", EnumSet.of(DeliveryMode.ALL, DeliveryMode.MESSAGE));
		return tq.getSingleResult().intValue();
	}
}
