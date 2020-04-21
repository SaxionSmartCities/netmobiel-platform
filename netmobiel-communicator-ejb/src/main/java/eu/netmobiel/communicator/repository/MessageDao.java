package eu.netmobiel.communicator.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.communicator.annotation.CommunicatorDatabase;
import eu.netmobiel.communicator.model.DeliveryMode;
import eu.netmobiel.communicator.model.Envelope;
import eu.netmobiel.communicator.model.Envelope_;
import eu.netmobiel.communicator.model.Message;
import eu.netmobiel.communicator.model.Message_;
import eu.netmobiel.communicator.model.User_;

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
	 * Lists the message ids considering the filter parameters. If a message has context, it is considered 
	 * as part of a conversation. In that case the messages with that context must be grouped and only the 
	 * most recent message is added in the list.   
	 * @param participant The address (managed identity) of the recipient or sender of the message. 
	 * @param context the context of the message (used as a conversation id). 
	 * @param since the date from which to list messages, using the creation date.
	 * @param until the date until to list the messages, using the creation date.
     * @param mode only show messages with the specified (effective) delivery mode. Omitting the mode or 
     * 				specifying DeliveryMode.ALL has the same effect: no filter on delivery mode.   
	 * @param maxResults The maximum number of messages (page size).
	 * @param offset the zero-based index to start the page.
	 * @return A list of envelope IDs matching the criteria. 
	 */
	public PagedResult<Long> listMessages(String participant, String context, Instant since, Instant until, DeliveryMode mode, Integer maxResults, Integer offset) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Message> message = cq.from(Message.class);
        List<Predicate> predicates = new ArrayList<>();
        if (participant != null) {
            Join<Message, Envelope> envelope = message.join(Message_.envelopes);
            Predicate predSender = cb.equal(message.get(Message_.sender).get(User_.managedIdentity), participant);
            Predicate predRecipient = cb.equal(envelope.get(Envelope_.recipient).get(User_.managedIdentity), participant);
            predicates.add(cb.or(predSender, predRecipient));
//	        cq.distinct(true);
        }
        if (context != null) {
        	Predicate contextNotNull = cb.isNotNull(message.get(Message_.context));
        	Predicate contextMatches = cb.equal(message.get(Message_.context), context);
	        predicates.add(cb.and(contextNotNull,contextMatches));
        }        
        if (since != null) {
	        Predicate predSince = cb.greaterThanOrEqualTo(message.get(Message_.creationTime), since);
	        predicates.add(predSince);
        }        
        if (until != null) {
	        Predicate predUntil = cb.lessThan(message.get(Message_.creationTime), until);
	        predicates.add(predUntil);
        }
        // 'modes' represents the query: null, empty or ALL represent any message. 
        // The message attribute 'deliveryMode' has a slightly different meaning. 
        if (mode != null && mode != DeliveryMode.ALL) {
	        Predicate predMode = cb.equal(message.get(Message_.deliveryMode), mode);
	        Predicate predModeAll = cb.equal(message.get(Message_.deliveryMode), DeliveryMode.ALL);
	        predicates.add(cb.or(predMode, predModeAll));
        }        
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (maxResults == 0) {
          cq.select(cb.count(message.get(Message_.id)));
          totalCount = em.createQuery(cq).getSingleResult();
        } else {
	        cq.select(message.get(Message_.id));
	        cq.orderBy(cb.desc(message.get(Message_.creationTime)));
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(offset);
			tq.setMaxResults(maxResults);
			results = tq.getResultList();
        }
        return new PagedResult<Long>(results, maxResults, offset, totalCount);
	}

	
	public PagedResult<Long> listConversations(String participant, Integer maxResults, Integer offset) {
		String basicQuery = 
				" from Message m where (m.context, m.creationTime) in " +
				" (select mm.context, max(mm.creationTime) from Message mm join mm.envelopes env" + 
				" where (env.recipient.managedIdentity = :participant or mm.sender.managedIdentity = :participant)" + 
				" group by mm.context) and m.deliveryMode in (:DeliveryModeAll, :DeliveryModeMessage)";

		Long totalCount = null;
        List<Long> results = null;
        if (maxResults == 0) {
    		TypedQuery<Long> countQuery = em.createQuery("select count(m) " + basicQuery, Long.class);
    		countQuery.setParameter("participant", participant);
    		countQuery.setParameter("DeliveryModeAll", DeliveryMode.ALL);
    		countQuery.setParameter("DeliveryModeMessage", DeliveryMode.MESSAGE);
            totalCount = countQuery.getSingleResult();
            results = Collections.emptyList();
        } else {
    		TypedQuery<Long> selectQuery = em.createQuery("select m.id " + basicQuery + " order by m.creationTime desc", Long.class);
    		selectQuery.setParameter("participant", participant);
    		selectQuery.setParameter("DeliveryModeAll", DeliveryMode.ALL);
    		selectQuery.setParameter("DeliveryModeMessage", DeliveryMode.MESSAGE);
    		selectQuery.setFirstResult(offset);
    		selectQuery.setMaxResults(maxResults);
    		results = selectQuery.getResultList();
        }
        return new PagedResult<Long>(results, maxResults, offset, totalCount);
	}
	
	@Override
	public List<Message> fetch(List<Long> ids, String graphName) {
		// Create an identity map using the generic fetch. Rows are returned, but not necessarily in the same order
		Map<Long, Message> resultMap = super.fetch(ids, graphName).stream().collect(Collectors.toMap(Message::getId, Function.identity()));
		// Now return the rows in the same order as the ids.
		return ids.stream().map(id -> resultMap.get(id)).collect(Collectors.toList());
	}

/*
Get the latest message for each context for recipient A2:
select distinct m.id, m.body, m.context, m.subject, m.sender, m.created_time from envelope e join message m on m.id = e.message
where e.recipient = 'recipient A2' and (m.context, m.created_time) in 
(select mm.context, max(mm.created_time) from envelope e join message mm on mm.id = e.message  
 where e.recipient = 'recipient A2' group by mm.context) order by m.created_time desc

 Get the  number of unread messages for each context for recipient A2
select mm.context, count(*) from envelope e join message mm on mm.id = e.message  
 where e.recipient = 'recipient A2' and e.ack_time is null group by mm.context 
 select mm.context, count(*) from envelope e join message mm on mm.id = e.message  
 where e.recipient = 'recipient A2' and e.ack_time is null group by mm.context
 */

}
