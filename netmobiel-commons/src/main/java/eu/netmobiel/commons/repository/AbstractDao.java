package eu.netmobiel.commons.repository;


import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

public abstract class AbstractDao<T, ID> {
	public static final String JPA_HINT_FETCH = "javax.persistence.fetchgraph";
	public static final String JPA_HINT_LOAD = "javax.persistence.loadgraph";
	
    
    protected abstract EntityManager getEntityManager();

    private Class<T> persistentClass;

    public AbstractDao(Class<T> entityClass) {
        this.persistentClass = entityClass;
    }

    protected Class<T> getPersistentClass() {
        return this.persistentClass;
    }

    public void clear() {
        getEntityManager().clear();
    }

    public void flush() {
        getEntityManager().flush();
    }

    public boolean contains(T entity) {
        return getEntityManager().contains(entity);
    }

    public void detach(T entity) {
        getEntityManager().detach(entity);
    }

    /**
     * Find by primary key.
     * Search for an entity of the specified class and primary key.
     * If the entity instance is contained in the persistence context,
     * it is returned from there.
     * @param entityClass  entity class
     * @param primaryKey  primary key
     * @return the found entity instance or null if the entity does
     *         not exist, as an Optional.
     * @throws IllegalArgumentException if the first argument does
     *         not denote an entity type or the second argument is 
     *         is not a valid type for that entity's primary key or
     *         is null
     */
    public Optional<T> find(ID id) {
        return Optional.ofNullable(getEntityManager().find(getPersistentClass(), id));
    }

    /**
     * Find by primary key, using the specified properties. 
     * Search for an entity of the specified class and primary key. 
     * If the entity instance is contained in the persistence 
     * context, it is returned from there. 
     * If a vendor-specific property or hint is not recognized, 
     * it is silently ignored. 
     * @param entityClass  entity class
     * @param primaryKey   primary key
     * @param properties  standard and vendor-specific properties 
     *        and hints
     * @return the found entity instance or null if the entity does
     *         not exist, as an Optional.
     * @throws IllegalArgumentException if the first argument does 
     *         not denote an entity type or the second argument is
     *         is not a valid type for that entity's primary key or 
     *         is null 
     * @since Java Persistence 2.0
     */ 
    public Optional<T> find(ID id, Map<String, Object> properties) {
        return Optional.ofNullable(getEntityManager().find(getPersistentClass(), id, properties));
    }

    public void refresh(T entity) {
        getEntityManager().refresh(entity);
    }

    public T save(T entity) {
        getEntityManager().persist(entity);
        return entity;
    }

    public T merge(T entity) {
        return getEntityManager().merge(entity);
    }

    public void remove(T entity) {
        getEntityManager().remove(entity);
    }
    
    public List<T> findAll() {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(getPersistentClass());
        Root<T> rootEntry = cq.from(getPersistentClass());
        CriteriaQuery<T> all = cq.select(rootEntry);
        TypedQuery<T> allQuery = getEntityManager().createQuery(all);
        return allQuery.getResultList();
    }

    public boolean isLoaded(T entity) {
        PersistenceUnitUtil puu = getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil();
        return puu.isLoaded(entity);
    }

    public boolean isLoaded(T entity, String attributeName) {
        PersistenceUnitUtil puu = getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil();
        return puu.isLoaded(entity, attributeName);
    }
    
    public Map<String, Object> createLoadHint(String graphName) {
        return Collections.singletonMap(JPA_HINT_LOAD, getEntityManager().getEntityGraph(graphName));
    }

    public Map<String, Object> createFetchHint(String graphName) {
        return Collections.singletonMap(JPA_HINT_FETCH, getEntityManager().getEntityGraph(graphName));
    }

    public T loadGraph(ID id, String graphName) {
        return id == null ? null : getEntityManager().find(getPersistentClass(), id, createLoadHint(graphName));
    }

    public T fetchGraph(ID id, String graphName) {
        return id == null ? null : getEntityManager().find(getPersistentClass(), id, createFetchHint(graphName));
    }

}
