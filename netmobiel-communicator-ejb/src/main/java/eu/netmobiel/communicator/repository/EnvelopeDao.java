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
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.communicator.annotation.CommunicatorDatabase;
import eu.netmobiel.communicator.model.Envelope;
import eu.netmobiel.communicator.model.Envelope_;
import eu.netmobiel.communicator.model.Message_;
import eu.netmobiel.communicator.model.User_;

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

	/**
	 * Lists the message ids considering the filter parameters. If a message has context, it is considered 
	 * as part of a conversation. In that case the messages with that context must be grouped and only the 
	 * most recent message is added in the list.   
	 * @param recipient The recipient address (managed identity). 
	 * @param context the context of the message (used as a conversation id). 
	 * @param since the date from which to list messages, using the creation date.
	 * @param until the date until to list the messages, using the creation date.
	 * @param maxResults The maximum number of messages (page size).
	 * @param offset the zero-based index to start the page.
	 * @return A list of envelope IDs matching the criteria. 
	 */
	public PagedResult<Long> listEnvelopes(String recipient, String context, Instant since, Instant until, Integer maxResults, Integer offset) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Envelope> env = cq.from(Envelope.class);
        List<Predicate> predicates = new ArrayList<>();
        if (recipient != null) {
            Predicate predRecipient = cb.equal(env.get(Envelope_.recipient).get(User_.managedIdentity), recipient);
            predicates.add(predRecipient);
        }
        if (context != null) {
	        Predicate predContext = cb.and(cb.isNotNull(env.get(Envelope_.message).get(Message_.context)), 
	        		cb.equal(env.get(Envelope_.message).get(Message_.context), context));
	        predicates.add(predContext);
        }        
        if (since != null) {
	        Predicate predSince = cb.greaterThanOrEqualTo(env.get(Envelope_.message).get(Message_.creationTime), since);
	        predicates.add(predSince);
        }        
        if (until != null) {
	        Predicate predUntil = cb.lessThan(env.get(Envelope_.message).get(Message_.creationTime), until);
	        predicates.add(predUntil);
        }        
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        if (maxResults == 0) {
          cq.select(cb.count(env.get(Envelope_.id)));
          totalCount = em.createQuery(cq).getSingleResult();
        } else {
	        cq.select(env.get(Envelope_.id));
	        cq.orderBy(cb.desc(env.get(Envelope_.message).get(Message_.creationTime)));
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(offset);
			tq.setMaxResults(maxResults);
			results = tq.getResultList();
        }
        return new PagedResult<Long>(results, maxResults, offset, totalCount);
	}

	/**
	 * Lists the message ids considering the filter parameters and the context. Each message has a context 
	 * that is used as a conversation. Messages with that context are grouped and only the 
	 * most recent message is added in the list.
	 * @param recipient The recipient address. 
	 * @param since the date from which to list messages, using the creation date.
	 * @param until the date until to list the messages, using the creation date.
	 * @param maxResults The maximum number of messages (page size).
	 * @param offset the zero-based index to start the page.
	 * @return A list of envelope IDs matching the criteria. 
	 */
//	public List<Long> listConverations(String recipient, String context, Instant since, Instant until, Integer maxResults, Integer offset) {
//    	CriteriaBuilder cb = em.getCriteriaBuilder();
//        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
//        Root<Envelope> env = cq.from(Envelope.class);
//        Join<Envelope, Message> msg = env.join(Envelope_.message); 
//        cq.select(msg.get(Message_.id));
//        List<Predicate> predicates = new ArrayList<>();
//        if (recipient != null) {
//            Predicate predRecipient = cb.equal(env.get(Envelope_.recipient), recipient);
//            predicates.add(predRecipient);
//        }
//        
//        Subquery<Tuple> sq = cq.subquery(Tuple.class);
////        CriteriaQuery<Tuple> sq = cb.createTupleQuery();
//        Root<Envelope> sqenv = sq.from(Envelope.class);
//        Join<Envelope, Message> sqmsg = sqenv.join(Envelope_.message); 
//        sq.select(cb.tuple(sqenv.get(Envelope_.message).get(Message_.context), cb.greatest(sqenv.get(Envelope_.message).get(Message_.creationTime))));
//        sq.where(cb.and(cb.equal(sqenv.get(Envelope_.recipient), recipient), 
//        		        cb.equal(sqenv.get(Envelope_.message).get(Message_.id), msg.get(Message_.id))));
//        sq.groupBy(sqenv.get(Envelope_.message).get(Message_.context));
//
//        
//        if (since != null) {
//	        Predicate predSince = cb.greaterThanOrEqualTo(env.get(Envelope_.message).get(Message_.creationTime), since);
//	        predicates.add(predSince);
//        }        
//        if (until != null) {
//	        Predicate predUntil = cb.lessThan(env.get(Envelope_.message).get(Message_.creationTime), until);
//	        predicates.add(predUntil);
//        }        
//        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
//        cq.orderBy(cb.desc(env.get(Envelope_.message).get(Message_.creationTime)));
//        TypedQuery<Long> tq = em.createQuery(cq);
//		tq.setFirstResult(offset == null ? 0 : offset);
//		tq.setMaxResults(maxResults == null ? MAX_RESULTS : maxResults);
//        return tq.getResultList();
//	}
	
	public PagedResult<Long> listConversations(String recipient, Integer maxResults, Integer offset) {
		String basicQuery = 
				" from Envelope e where e.recipient.managedIdentity = :recipient and (e.message.context, e.message.creationTime) in " +
				" (select ee.message.context, max(ee.message.creationTime) from Envelope ee where ee.recipient.managedIdentity = :recipient group by ee.message.context) ";
		TypedQuery<Long> countQuery = em.createQuery("select count(e) " + basicQuery, Long.class);
		countQuery.setParameter("recipient", recipient);
        Long totalCount = countQuery.getSingleResult();

        List<Long> results = Collections.emptyList();
        if (maxResults > 0) {
    		TypedQuery<Long> tq = em.createQuery("select e.id " + basicQuery + " order by e.message.creationTime desc", Long.class);
    		tq.setParameter("recipient", recipient);
    		tq.setFirstResult(offset);
    		tq.setMaxResults(maxResults);
    		results = tq.getResultList();
        }
        return new PagedResult<Long>(results, maxResults, offset, totalCount);
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
