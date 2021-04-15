package eu.netmobiel.commons.repository;

import java.util.List;
import java.util.Optional;

import javax.persistence.TypedQuery;

import org.hibernate.jpa.QueryHints;

import eu.netmobiel.commons.exception.BadRequestException;
import eu.netmobiel.commons.filter.Cursor;
import eu.netmobiel.commons.model.PagedResult;
import eu.netmobiel.commons.model.User;

public abstract class UserDao<T extends User> extends AbstractDao<T, Long> {

    public UserDao(Class<T> entityClass) {
		super(entityClass);
	}

    public Optional<T> findByManagedIdentity(String managedId) {
//    	CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
//        CriteriaQuery<T> cq = cb.createQuery(this.getPersistentClass());
//        Root<T> entry = cq.from(this.getPersistentClass());
//        cq.where(cb.equal(entry.get(AbstractUser_.managedIdentity), managedId));
//        TypedQuery<T> tq = getEntityManager().createQuery(cq);
//		List<T> results = tq.getResultList();
//    	return Optional.ofNullable(results.size() > 0 ? results.get(0) : null); 

    	List<T> users = getEntityManager().createQuery(String.format("from %s where managedIdentity = :identity", 
    			getPersistentClass().getSimpleName()), getPersistentClass())
    			.setParameter("identity", managedId)
    			.getResultList();
    	return Optional.ofNullable(users.size() > 0 ? users.get(0) : null); 
    }

    public Optional<T> findByManagedIdentity(String managedId, String graphName) {
    	TypedQuery<T> tq = getEntityManager().createQuery(String.format("from %s where managedIdentity = :identity", 
    			getPersistentClass().getSimpleName()), getPersistentClass())
    			.setParameter("identity", managedId);
    	if (graphName != null) {
    		tq.setHint(QueryHints.HINT_LOADGRAPH, getEntityManager().getEntityGraph(graphName));
    	}
    	List<T> users = tq.getResultList();
    	return Optional.ofNullable(users.size() > 0 ? users.get(0) : null); 
    }

    public Optional<T> getReferenceByManagedIdentity(String managedId) {
    	String primKeyName = getEntityManager().getMetamodel().entity(getPersistentClass()).getDeclaredId(Long.class).getName();
    	List<Long> objs = getEntityManager().createQuery(String.format("select %s from %s where managedIdentity = :identity", 
    			primKeyName, getPersistentClass().getSimpleName()), Long.class)
    			.setParameter("identity", managedId)
    			.getResultList();
    	return Optional.ofNullable(objs.size() > 0 ? getReference(objs.get(0)) : null); 
    }
    
    public PagedResult<T> listUsers(Cursor cursor) throws BadRequestException {
    	cursor.validate(100, 0);
    	Long totalCount = null;
    	List<T> ids = null;
    	if (cursor.isCountingQuery()) {
    		totalCount = getEntityManager().createQuery(
    				String.format("select count(u) from %s u order by u.id asc", getPersistentClass().getSimpleName()), 
    				Long.class)
    				.getSingleResult();
    	} else {
        	ids = getEntityManager().createQuery(String.format("from %s order by id asc", 
        			getPersistentClass().getSimpleName()), getPersistentClass())
        			.setFirstResult(cursor.getOffset())
        			.setMaxResults(cursor.getMaxResults())
        			.getResultList();
    	}
    	return new PagedResult<T>(ids, cursor, totalCount); 
    }

    public List<String> listManagedIdentities() {
    	return getEntityManager().createQuery(String.format("select managedIdentity from %s order by managedIdentity asc", 
    					getPersistentClass().getSimpleName()), String.class)
    				.getResultList();
    }
    
    public boolean userExists(String managedIdentity) {
   		Long count = getEntityManager().createQuery(
    				String.format("select count(u) from %s u where managedIdentity = :identity", getPersistentClass().getSimpleName()), 
    				Long.class)
   				.setParameter("identity", managedIdentity)	
    			.getSingleResult();
    	return count > 0; 
    }

}
