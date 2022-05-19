package eu.netmobiel.communicator.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;

import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.SortDirection;
import eu.netmobiel.commons.repository.AbstractDao;
import eu.netmobiel.communicator.annotation.CommunicatorDatabase;
import eu.netmobiel.communicator.filter.ConversationFilter;
import eu.netmobiel.communicator.model.CommunicatorUser;
import eu.netmobiel.communicator.model.Conversation;
import eu.netmobiel.communicator.model.Conversation_;

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
	
	/**
	 * Lists the conversation ids considering the filter parameters.  
	 * @param filter The conversation filter to use. 
	 * @param cursor The cursor to use. 
	 * @return A list of conversation IDs matching the criteria. 
	 */
	public PagedResult<Long> listConversations(ConversationFilter filter, Cursor cursor) {
    	CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Conversation> root = cq.from(Conversation.class);
        List<Predicate> predicates = new ArrayList<>();
        if (filter.getOwner() != null) {
            predicates.add(cb.equal(root.get(Conversation_.owner), filter.getOwner()));
        }
        if (filter.getContext() != null) {
	        predicates.add(cb.isMember(filter.getContext(), root.get(Conversation_.contexts)));
        }        
        if (filter.getSince() != null) {
	        predicates.add(cb.greaterThanOrEqualTo(root.get(Conversation_.createdTime), filter.getSince()));
        }        
        if (filter.getUntil() != null) {
	        predicates.add(cb.lessThan(root.get(Conversation_.createdTime), filter.getUntil()));
        }
        if (filter.isActualOnly()) {
	        predicates.add(cb.isNull(root.get(Conversation_.archivedTime)));
        } else if (filter.isArchivedOnly()) {
	        predicates.add(cb.isNotNull(root.get(Conversation_.archivedTime)));
        }
        cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        Long totalCount = null;
        List<Long> results = Collections.emptyList();
        Expression<Long> primaryKeyExpr = root.get(Conversation_.id);
        if (cursor.isCountingQuery()) {
          cq.select(cb.count(primaryKeyExpr));
          totalCount = em.createQuery(cq).getSingleResult();
        } else {
	        cq.select(primaryKeyExpr);
            Expression<Instant> orderExpr = root.get(Conversation_.createdTime);
	        if (filter.getSortDir() == SortDirection.DESC) {
	        	cq.orderBy(cb.desc(orderExpr), cb.desc(primaryKeyExpr));
	        } else {
	        	cq.orderBy(cb.asc(orderExpr), cb.asc(primaryKeyExpr));
	        }
	        TypedQuery<Long> tq = em.createQuery(cq);
			tq.setFirstResult(cursor.getOffset());
			tq.setMaxResults(cursor.getMaxResults());
			results = tq.getResultList();
        }
        return new PagedResult<>(results, cursor, totalCount);
	}

	/**
	 * Archive all conversations with a most recent message before the specified history date.
	 * @param history the threshold date to consider a conversation archivable.
	 * @return the number of conversations archived.
	 */
	public int archiveConversations(Instant history) {
		Instant now = Instant.now();
		String queryString =  
			"update Conversation c set c.archivedTime = :archivedTime where c.archivedTime is null and c in " +
				"(select env.conversation from Envelope env group by env.conversation having max(env.message.createdTime) < :history)";
		Query query = em.createQuery(queryString);
		query.setParameter("archivedTime", now);
		query.setParameter("history", history);
   		return query.executeUpdate();
	}

}
