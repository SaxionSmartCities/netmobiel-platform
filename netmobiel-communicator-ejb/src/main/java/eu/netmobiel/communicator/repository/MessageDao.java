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
import eu.netmobiel.commons.model.User_;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.communicator.annotation.CommunicatorDatabase;
import eu.netmobiel.communicator.filter.MessageFilter;
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
        if (filter.getParticipantId() != null) {
            Join<Message, Envelope> envelope = message.join(Message_.envelopes);
            Predicate predRecipient = cb.equal(envelope.get(Envelope_.conversation)
            		.get(Conversation_.owner)
            		.get(User_.managedIdentity), filter.getParticipantId());
            predicates.add(predRecipient);
//	        cq.distinct(true);
        }
        if (filter.getContext() != null) {
            Join<Message, Envelope> envelope = message.join(Message_.envelopes);
            Predicate envContext = cb.equal(envelope.get(Envelope_.context), filter.getContext());
        	Predicate msgContext = cb.equal(message.get(Message_.context), filter.getContext());
	        predicates.add(cb.or(envContext, msgContext));
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

	
	public PagedResult<Long> listTopMessagesByConversations(String ownerManagedIdentity, boolean actualOnly, boolean archivedOnly, Integer maxResults, Integer offset) {
		// To write the query below as a criteria query seems impossible, I can't get the selection of a subquery right.
		String queryString = String.format( 
				"%s from Envelope e where (e.conversation, e.message.createdTime) in" +
				" (select env.conversation, max(env.message.createdTime) from Envelope env" + 
				"  where env.message.deliveryMode in :deliverySet %s" +
				"  group by env.conversation" +
				" ) %s %s",
				maxResults == 0 ? "select count(e.message.id)" : "select e.message.id",  
						ownerManagedIdentity != null ? "and env.conversation.owner.managedIdentity = :participant" : "",
				actualOnly ? "and e.conversation.archivedTime is null" : (archivedOnly ? "and e.conversation.archivedTime is not null" : ""),
				maxResults > 0 ? "order by e.message.createdTime desc" : ""
		);
		TypedQuery<Long> query = em.createQuery(queryString, Long.class);
		if (ownerManagedIdentity != null) {
			query.setParameter("participant", ownerManagedIdentity);
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

//	public static class RecentConversationMessage {
//		private Conversation conversation;
//		private Instant creationTime;
//		public Conversation getConversation() {
//			return conversation;
//		}
//		public void setConversation(Conversation conversation) {
//			this.conversation = conversation;
//		}
//		public Instant getCreationTime() {
//			return creationTime;
//		}
//		public void setCreationTime(Instant creationTime) {
//			this.creationTime = creationTime;
//		}
//	}
//	public PagedResult<Long> listTopMessagesByConversations(String ownerMangedIdentity, boolean actualOnly, boolean archiveOnly, Integer maxResults, Integer offset) {
//    	CriteriaBuilder cb = em.getCriteriaBuilder();
//        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
//        Root<Envelope> envelope = cq.from(Envelope.class);
//        List<Predicate> predicates = new ArrayList<>();
//        
//        if (ownerMangedIdentity != null) {
//            Predicate predRecipient = cb.equal(envelope.get(Envelope_.conversation)
//            		.get(Conversation_.owner)
//            		.get(User_.managedIdentity), ownerMangedIdentity);
//            predicates.add(predRecipient);
////	        cq.distinct(true);
//        }
//        Subquery<RecentConversationMessage> subquery = cq.subquery(RecentConversationMessage.class);
//        Root<Envelope> sqenv = subquery.from(Envelope.class);
//        cb.construct(RecentConversationMessage.class, sqenv.get(Envelope_.conversation), cb.greatest(sqenv.get(Envelope_.message).get(Message_.createdTime))).alias("topMessages");
//        subquery.select();
////        subquery.select();
////        , envelope.get(Envelope_.message).get(Message_.createdTime)
//        cq.where(cb.in(envelope.get(Envelope_.conversation)).value(subquery));
//        Long totalCount = null;
//        List<Long> results = Collections.emptyList();
//        if (maxResults == 0) {
//          cq.select(cb.count(envelope.get(Envelope_.message).get(Message_.id)));
//          totalCount = em.createQuery(cq).getSingleResult();
//        } else {
//	        cq.select(envelope.get(Envelope_.message).get(Message_.id));
//	        cq.orderBy(cb.desc(envelope.get(Envelope_.message).get(Message_.createdTime)));
//	        TypedQuery<Long> tq = em.createQuery(cq);
//			tq.setFirstResult(offset);
//			tq.setMaxResults(maxResults);
//			results = tq.getResultList();
//        }
//        return new PagedResult<>(results, maxResults, offset, totalCount);
//	}	
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
	
	/*
select u.family_name, date_part('year', m.created_time) as year, 
date_part('month', m.created_time) as month, count(*)
from message m 
join envelope e on m.id = e.message
join cm_user u on u.id = e.recipient
where m.delivery_mode = 'AL' or m.delivery_mode = 'MS'
group by u.family_name, date_part('year', m.created_time), date_part('month', m.created_time)
order by u.family_name, date_part('year', m.created_time), date_part('month', m.created_time)
	 
	 */

//    protected List<NumericReportValue> reportMessagesReceived(Instant since, Instant until, Cursor cursor) throws BadRequestException {
        // This criteria code can work only when registering the date_part function in the dialect.
        // @see https://thorben-janssen.com/database-functions/
//    	CriteriaBuilder cb = em.getCriteriaBuilder();
//        CriteriaQuery<NumericReportValue> cq = cb.createQuery(NumericReportValue.class);
//        Root<Message> message = cq.from(Message.class);
//        List<Predicate> predicates = new ArrayList<>();
//        Join<Message, Envelope> envelope = message.join(Message_.envelopes);
//        if (since != null) {
//	        predicates.add(cb.greaterThanOrEqualTo(message.get(Message_.creationTime), since));
//        }        
//        if (until != null) {
//	        predicates.add(cb.lessThan(message.get(Message_.creationTime), until));
//        }
//        Predicate predMode = cb.equal(message.get(Message_.deliveryMode), DeliveryMode.MESSAGE);
//        Predicate predModeAll = cb.equal(message.get(Message_.deliveryMode), DeliveryMode.ALL);
//        predicates.add(cb.or(predMode, predModeAll));
//        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
//        Expression<Integer> year = cb.function("date_part", Integer.class, cb.literal("year"), message.get(Message_.creationTime));
//        Expression<Integer> month = cb.function("date_part", Integer.class, cb.literal("month"), message.get(Message_.creationTime));
//        Path<String> user = envelope.get(Envelope_.recipient).get(CommunicatorUser_.managedIdentity);
//        cq.groupBy(user, year, month);
//    	cq.select(cb.construct(NumericReportValue.class, user, year, month, cb.count(message)));
//        cq.orderBy(cb.asc(user), cb.asc(year), cb.asc(month));
//        TypedQuery<NumericReportValue> tq = em.createQuery(cq);
//		tq.setFirstResult(cursor.getOffset());
//		tq.setMaxResults(cursor.getMaxResults());
//		return tq.getResultList();
//    }

}
