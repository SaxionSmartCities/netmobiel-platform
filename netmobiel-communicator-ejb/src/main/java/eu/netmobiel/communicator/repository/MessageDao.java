package eu.netmobiel.communicator.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.communicator.annotation.CommunicatorDatabase;
import eu.netmobiel.communicator.filter.MessageFilter;
import eu.netmobiel.communicator.model.CommunicatorUser;
import eu.netmobiel.communicator.model.Conversation_;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Envelope;
import eu.netmobiel.communicator.model.Envelope_;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.model.Message_;

@ApplicationScoped
@Typed(MessageDao.class)
public class MessageDao extends AbstractDao<Message, Long> {

    @Inject @CommunicatorDatabase
    private EntityManager em;

    public MessageDao() {
		super(Message.class);
	}

	@Override
	protected EntityManager getEntityManager() {
		return em;
	}

	/**
	 * Lists the message ids considering the filter parameters.  
	 * @param filter The message filter to use. 
	 * @param cursor The cursor to use. 
	 * @return A list of message IDs matching the criteria. 
	 */
	public PagedResult<Long> listMessages(MessageFilter filter, Cursor cursor) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Message> message = cq.from(Message.class);
        List<Predicate> predicates = new ArrayList<>();
        if (filter.getConversationId() != null) {
            Join<Message, Envelope> envelope = message.join(Message_.envelopes);
            Predicate predConversation = cb.equal(envelope.get(Envelope_.conversation)
            		.get(Conversation_.id), filter.getConversationId());
            predicates.add(predConversation);
//	        cq.distinct(true);
        }
        if (filter.getParticipant() != null) {
            Join<Message, Envelope> envelope = message.join(Message_.envelopes);
            Predicate predRecipient = cb.equal(envelope.get(Envelope_.conversation)
            		.get(Conversation_.owner), filter.getParticipant());
            predicates.add(predRecipient);
//	        cq.distinct(true);
        }
        if (filter.getContext() != null) {
        	Predicate msgContext = cb.equal(message.get(Message_.context), filter.getContext());
	        predicates.add(msgContext);
        }        
        if (filter.getSince() != null) {
	        Predicate predSince = cb.greaterThanOrEqualTo(message.get(Message_.createdTime), filter.getSince());
	        predicates.add(predSince);
        }        
        if (filter.getUntil() != null) {
	        Predicate predUntil = cb.lessThan(message.get(Message_.createdTime), filter.getUntil());
	        predicates.add(predUntil);
        }
        // 'modes' represents the query: null, empty or ALL represent any message. 
        // The message attribute 'deliveryMode' has a slightly different meaning. 
        if (filter.getDeliveryMode() != null && filter.getDeliveryMode() != DeliveryMode.ALL) {
	        Predicate predMode = cb.equal(message.get(Message_.deliveryMode), filter.getDeliveryMode());
	        Predicate predModeAll = cb.equal(message.get(Message_.deliveryMode), DeliveryMode.ALL);
	        predicates.add(cb.or(predMode, predModeAll));
        }        
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (cursor.isCountingQuery()) {
          cq.select(cb.count(message.get(Message_.id)));
          totalCount = em.createQuery(cq).getSingleResult();
        } else {
	        cq.select(message.get(Message_.id));
	        if (filter.getSortDir() == SortDirection.DESC) {
	        	cq.orderBy(cb.desc(message.get(Message_.createdTime)), cb.desc(message.get(Message_.id)));
	        } else {
	        	cq.orderBy(cb.asc(message.get(Message_.createdTime)), cb.asc(message.get(Message_.id)));
	        }
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(cursor.getOffset());
			tq.setMaxResults(cursor.getMaxResults());
			results = tq.getResultList();
        }
        return new PagedResult<>(results, cursor, totalCount);
	}

	
	public PagedResult<Long> listTopMessagesByConversations(String context, CommunicatorUser owner, 
			boolean actualOnly, boolean archivedOnly, SortDirection sortDir, Integer maxResults, Integer offset) {
		String sort = sortDir != null ? sortDir.name().toLowerCase() : "desc"; 
		// To write the query below as a criteria query seems impossible, I can't get the selection of a subquery right.
		String queryString = String.format( 
				"%s from Envelope e where (e.conversation, e.message.id) in" +
				" (select env.conversation, max(env.message.id) from Envelope env" + 
				"  where env.message.deliveryMode in :deliverySet %s %s" +
				"  group by env.conversation" +
				" ) %s %s",
				maxResults == 0 ? "select count(e.message.id)" : "select e.message.id",  
				owner != null ? "and env.conversation.owner = :participant" : "",
				context != null ? "and :context member of env.conversation.contexts" : "",
				actualOnly ? "and e.conversation.archivedTime is null" : (archivedOnly ? "and e.conversation.archivedTime is not null" : ""),
				maxResults > 0 ? "order by e.message.id " + sort : ""
		);
		TypedQuery<Long> query = em.createQuery(queryString, Long.class);
		if (owner != null) {
			query.setParameter("participant", owner);
		}
		if (context != null) {
			query.setParameter("context", context);
		}
		query.setParameter("deliverySet", EnumSet.of(DeliveryMode.ALL, DeliveryMode.MESSAGE));
		Long totalCount = null;
        List<Long> results = null;
        if (maxResults == 0) {
            totalCount = query.getSingleResult();
            results = Collections.emptyList();
        } else {
    		query.setFirstResult(offset);
    		query.setMaxResults(maxResults);
    		results = query.getResultList();
        }
        return new PagedResult<>(results, maxResults, offset, totalCount);
	}

}
