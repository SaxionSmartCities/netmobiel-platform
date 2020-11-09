package eu.netmobiel.commons.repository;


import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

public abstract class AbstractDao<T, ID> {
	public static final String JPA_HINT_FETCH = "javax.persistence.fetchgraph";
	public static final String JPA_HINT_LOAD = "javax.persistence.loadgraph";
	
    
    protected abstract EntityManager getEntityManager();

    private Class<T> persistentClass;
    private Class<ID> primaryKeyClass;

    @SuppressWarnings("unchecked")
	public AbstractDao(Class<T> entityClass) {
        this((Class<ID>) Long.class, entityClass);
    }

    public AbstractDao(Class<ID> pkClass, Class<T> entityClass) {
        this.primaryKeyClass = pkClass;
        this.persistentClass = entityClass;
    }

    public Class<ID> getPrimaryKeyClass() {
		return primaryKeyClass;
	}

	public Class<T> getPersistentClass() {
        return this.persistentClass;
    }

	public T getReference(ID id) {
        return getEntityManager().getReference(getPersistentClass(), id);
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

    /**
     * Given a list of identifiers, return the objects according the graph and the query hint type. 
     * The output list has NOT necessarily the same order as the input list, specify a keyMapper to guarantee the same order.
     * @param ids A list of primary keys
     * @param graphName The name of the named entity graph to apply.
     * @param the JPA query hint name: javax.persistence.fetchgraph or javax.persistence.loadgraph
     * @param a keymapper function, mapping the domain object to the identity key.
     * @return
     */
    protected List<T> queryGraphs(List<ID> ids, String graphName, String queryHintType, Function<T, ID> keyMapper) {
    	if (ids == null || ids.isEmpty()) {
    		return Collections.emptyList();
    	}
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(getPersistentClass());
        Root<T> rootEntry = cq.from(getPersistentClass());
        cq.select(rootEntry);
        Expression<ID> exp = rootEntry.get(rootEntry.getModel().getDeclaredId(getPrimaryKeyClass()));
        cq.where(exp.in(ids));
        TypedQuery<T> tq = getEntityManager().createQuery(cq);
        if (graphName != null) {
        	tq.setHint(queryHintType, getEntityManager().getEntityGraph(graphName));
        }
        List<T> results = tq.getResultList();
        if (keyMapper != null) {
			Map<ID, T> resultMap = results.stream()
					.collect(Collectors.toMap(keyMapper, Function.identity())); 
			// Now return the rows in the same order as the ids.
			results = ids.stream()
					.map(id -> resultMap.get(id))
					.collect(Collectors.toList());
        }
        return results;
    }
        

	public List<T> loadGraphs(List<ID> ids, String graphName, Function<T, ID> keyMapper) {
		return queryGraphs(ids, graphName, JPA_HINT_LOAD, keyMapper);
	}

	public List<T> fetchGraphs(List<ID> ids, String graphName, Function<T, ID> keyMapper) {
		return queryGraphs(ids, graphName, JPA_HINT_FETCH, keyMapper);
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
        return graphName != null ? Collections.singletonMap(JPA_HINT_LOAD, getEntityManager().getEntityGraph(graphName)) : Collections.emptyMap();
    }

    public Map<String, Object> createFetchHint(String graphName) {
        return graphName != null ? Collections.singletonMap(JPA_HINT_FETCH, getEntityManager().getEntityGraph(graphName)) : Collections.emptyMap();
    }

    public Optional<T> loadGraph(ID id, String graphName) {
        return Optional.ofNullable(id == null ? null : getEntityManager().find(getPersistentClass(), id, createLoadHint(graphName)));
    }

    public Optional<T> fetchGraph(ID id, String graphName) {
        return Optional.ofNullable(id == null ? null : getEntityManager().find(getPersistentClass(), id, createFetchHint(graphName)));
    }

    public EntityGraph<?> getEntityGraph(String graphName) {
    	return getEntityManager().getEntityGraph(graphName);
    }
    
    // Fancy count stuff
    // https://stackoverflow.com/questions/2883887/in-jpa-2-using-a-criteriaquery-how-to-count-results

    /**
     * Determine the count of a slightly modified group query. The group is not used, instead a count(distinct countExpression) 
     * is used. Probably not valid for complex report queries. This query uses in a fact a proxy for counting
     * the results. 
     * @param cb the criteria builder
     * @param selectQuery the original query.
     * @param root the root object in the original query for obtaining the original joins.
     * @param countExpression the column expression to select for counting distinct.
     * @return The total number of results to expect.
     */
    public Long countDistinct(final CriteriaBuilder cb, final CriteriaQuery<?> selectQuery, Root<T> root, Expression<?> countExpression) {
        CriteriaQuery<Long> query = createCountQuery(cb, selectQuery, root, countExpression, true);
        return getEntityManager().createQuery(query).getSingleResult();
    }

    /**
     * Determine the total count given a query. The group list and group restriction is ignored.
     * This method can only be used for counting the number of results in simple queries without no grouping.
     * @param cb the criteria builder
     * @param selectQuery the original query.
     * @param root the root object in the original query for obtaining the original joins.
     * @return The total number of results to expect.
     */
    public Long count(final CriteriaBuilder cb, final CriteriaQuery<?> selectQuery, Root<T> root) {
        CriteriaQuery<Long> query = createCountQuery(cb, selectQuery, root, root, false);
        return getEntityManager().createQuery(query).getSingleResult();
    }

    private CriteriaQuery<Long> createCountQuery(final CriteriaBuilder cb, final CriteriaQuery<?> criteria, final Root<T> root, Expression<?> countExpression, boolean countDistinct) {

        final CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        final Root<?> countRoot = countQuery.from(root.getJavaType());

        doJoins(root.getJoins(), countRoot);
        doJoinsOnFetches(root.getFetches(), countRoot);

        countQuery.select(countDistinct ? cb.countDistinct(countExpression) : cb.count(countExpression));
        countQuery.where(criteria.getRestriction());
        if (! countDistinct && !criteria.getGroupList().isEmpty()) {
        	throw new IllegalArgumentException("Cannot count results having a group expression");
        }
//        countQuery.groupBy(criteria.getGroupList());
//        if (criteria.getGroupRestriction() != null) {
//        	countQuery.having(criteria.getGroupRestriction());
//        }
        countRoot.alias(root.getAlias());

        return countQuery.distinct(criteria.isDistinct());
    }

    @SuppressWarnings("unchecked")
    private void doJoinsOnFetches(Set<? extends Fetch<?, ?>> joins, Root<?> root) {
        doJoins((Set<? extends Join<?, ?>>) joins, root);
    }

    private void doJoins(Set<? extends Join<?, ?>> joins, Root<?> root) {
        for (Join<?, ?> join : joins) {
            Join<?, ?> joined = root.join(join.getAttribute().getName(), join.getJoinType());
            joined.alias(join.getAlias());
            doJoins(join.getJoins(), joined);
        }
    }

    private void doJoins(Set<? extends Join<?, ?>> joins, Join<?, ?> root) {
        for (Join<?, ?> join : joins) {
            Join<?, ?> joined = root.join(join.getAttribute().getName(), join.getJoinType());
            joined.alias(join.getAlias());
            doJoins(join.getJoins(), joined);
        }
    }
}
